package org.tensorflow.demo.Offloading;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.tensorflow.demo.Offloading.Constant.Config.DELTA_E_REAL_AVG_ALPHA;
import static org.tensorflow.demo.Offloading.Constant.Config.DELTA_E_REAL_AVG_NUM;
import static org.tensorflow.demo.Offloading.Constant.Config.DELTA_E_REAL_VARIANCE_THRESHOLD;
import static org.tensorflow.demo.Offloading.Constant.Config.DELTA_S_CLIMBING_STEP;
import static org.tensorflow.demo.Offloading.Constant.Config.DELTA_S_SHRINKING_STEP;
import static org.tensorflow.demo.Offloading.Constant.Config.DELTA_S_UPDATE_INTERVAL;
import static org.tensorflow.demo.Offloading.Constant.Config.KEEP_STABLE_ROUND;
import static org.tensorflow.demo.Offloading.Constant.Config.SEND_DELAY_MS;
import static org.tensorflow.demo.Offloading.Constant.Config.SERVER_IP;
import static org.tensorflow.demo.Offloading.Constant.Config.SERVER_PORT;
import static org.tensorflow.demo.Offloading.Constant.INTERRUPTED_EXCEPTION;
import static org.tensorflow.demo.Offloading.Constant.IO_EXCEPTION;
import static org.tensorflow.demo.Offloading.Constant.SUCCESS;

/**
 * Created by fanquan on 17-7-17.
 */

public class WiFiDevice extends DeviceAdapter {

    private SparseArray<Task> ibuffer;      /**< Reuse OffloadingBuffer as internal buffer */
    private Thread IThread;                 /**< Thread for network input */
    private HandlerThread OThread;          /**< Thread for network output */
    private Handler OThreadHandler;         /**< Handler for triggering the uploading */

    private DeviceManager deviceManager;

    // ZMQ
    ZMQ.Context context;
    ZMQ.Socket requester;

    // Smooth Send-Back
    private int delta_e_real_counter_avg;           /**< counter for above */
    private int delta_e_real_counter_update;
    private int stable_period_counter;
    private int last_stable_delta_s;
    private boolean skip_first_interval = true;

    public WiFiDevice(DeviceManager deviceManager) {
        super(true, true, "WiFi");
        this.deviceManager = deviceManager;
    }

    /**
     * \note    Last reviewed 2017.8.11 17:12
     */
    @Override
    public int init() {
        // Initialize internal buffer
        ibuffer = new SparseArray<>();

        // Initialize output thread
        OThread = new HandlerThread("WiFi-upload");
        OThread.start();
        OThreadHandler = new Handler(OThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Task _task = (Task) msg.obj;
                packAndSend(_task);
            }
        };

        // Initialize socket & connect
        OThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                context = ZMQ.context(2);
                Log.i("FQ", "Connecting to server...");
                requester = context.socket(ZMQ.DEALER);
                requester.setSendTimeOut(-1);       // Block
                requester.setIdentity("FQ".getBytes());
                requester.connect("tcp://" + SERVER_IP + ":" + SERVER_PORT);
                Log.i("FQ", "Server connected");

                // Start to fetch result
                fetchResult(WiFiDevice.this.id);
            }
        });

        delta_e_real_counter_avg = 0;
        delta_e_real_counter_update = 0;
        stable_period_counter = 0;
        last_stable_delta_s = 0;    // zero is invalided

        return SUCCESS;
    }

    @Override
    public int preprocess(int deviceId, Task task) {
        return 0;
    }

    /**
     * \note    Last reviewed 2017.8.11 17:13
     */
    @Override
    public int uploadAndRun(int deviceId, Task task) {
        // Insert task in buffer
        ibuffer.put((int) task.id, task);

        // Call output thread to handle packing and sending
        Message msg = Message.obtain();
        msg.obj = task;
        OThreadHandler.sendMessage(msg);

        return SUCCESS;
    }

    /**
     * \note    Last reviewed 2017.8.11 17:07
     */
    private synchronized int packAndSend(Task task) {
        // msgpack
        long packStart = System.currentTimeMillis();
        final MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packLong(task.id);                           // Task::id
            packer.packString(task.appName);                    // Task::appName
            packer.packString(task.modelName);                  // Task::modelName
            packer.packInt(task.bufferIndex);                   // Task::bufferIndex
            // No need to pack Task::status

            packer.packArrayHeader(task.inputNodes.size());     // Task::inputNodes
            for (String s : task.inputNodes) {
                packer.packString(s + ":0");
            }
            packer.packArrayHeader(task.inputValues.size());    // Task::inputValues
            for (float[] x : task.inputValues) {
                // Don't use msgpack to serialize large float array any more for performance consideration
//                packer.packArrayHeader(x.length);       // should calculate dims to length for accuracy
//                for (float f : x) {
//                    packer.packFloat(f);
//                }
                // pack as binary
                int byteNum = x.length * 4;     // 4 pytes per float
                ByteBuffer byteBuffer = ByteBuffer.allocate(byteNum);
                FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
                floatBuffer.put(x);

                packer.packBinaryHeader(byteNum);
                packer.writePayload(byteBuffer.array());
            }

            packer.packArrayHeader(task.dims.size());           // Task::dims
            for (long[] x : task.dims) {
                packer.packArrayHeader(x.length);
                for (long l : x) {
                    packer.packLong(l);
                }
            }

            packer.packArrayHeader(task.outputNodes.length);    // Task::outputNodes
            for (String s : task.outputNodes) {
                packer.packString(s + ":0");
            }
            // No need to pack Task::outputs
            packer.packMapHeader(task.odims.size());            // Task::odims
            for (Map.Entry<String, long[]> entry : task.odims.entrySet()) {     // should check the map size
                packer.packString(entry.getKey());
                packer.packArrayHeader(entry.getValue().length);
                for (long l : entry.getValue()) {
                    packer.packLong(l);
                }
            }

            // No need to pack Task::cost

            packer.flush();
//            Log.i("FQ", "Packed data length: " + packer.toByteArray().length);
            long packEnd = System.currentTimeMillis();
            // time of msgpack should calc alone and += preprocess time
            task.cost.pre_process += packEnd - packStart;
            task.cost.calculateSchedulingCost();

            // Send
            long sendStart = System.currentTimeMillis();
//            delay(SEND_DELAY_MS);
            requester.send(packer.toByteArray(), 0);
            long sendEnd = System.currentTimeMillis();
            task.cost.uploading = (int) (sendEnd - sendStart + SEND_DELAY_MS);      // Add fake network delay
            task.cost.calculateSchedulingCost();
            packer.close();

            // Wait for delta_s
            long t = delta_s - packEnd + packStart;
            if (t > 0)
                Thread.sleep(t);
//            Thread.sleep(100);
        }
        catch (IOException e) {
            Log.e("FQ", "IOException occurred when packing task!\n" + e.getMessage());
            return IO_EXCEPTION;
        }
        catch (InterruptedException e) {      // Regard to sleep()
            Log.e("FQ", "Interrupted exception occurred when it wait for delta_s!\n" + e.getMessage());
            return INTERRUPTED_EXCEPTION;
        }

        // Callback - callNextHandler
        // No matter which stage's cost is bigger, we fetch next task right after finish uploading
        // Since we consider the cost as we adjusting the sampling rate
        Message callNextMsg = Message.obtain();

        Bundle callNextBundle = new Bundle();
        callNextBundle.putInt("deviceId", this.id);
        callNextMsg.setData(callNextBundle);

        deviceManager.callNextHandler.sendMessage(callNextMsg);

        return SUCCESS;
    }

    @Override
    public int startCompute(int deviceId) {
        return 0;
    }

    /**
     * \note    Last reviewed 2017.8.11 17:12
     */
    @Override
    public int fetchResult(final int deviceId) {
        // init IThread:
        IThread = new Thread() {
            @Override
            public void run() {
                while (true) {
                    byte[] reply = requester.recv(0);
                    if (reply.length == 0)
                        continue;

                    // update delta_e_real & delta_e_real^2 | Design V3.0
                    delta_e_real_counter_avg++;
                    delta_e_real_counter_update++;
                    long currentTime = System.currentTimeMillis();
                    int t = (int) (currentTime - lastResultTime);
                    lastResultTime = currentTime;
                    if (delta_e_real_counter_avg < DELTA_E_REAL_AVG_NUM) {
                        float smooth_factor = 1.0f / delta_e_real_counter_avg;
                        delta_e_real = (long) (delta_e_real * (1 - smooth_factor) + smooth_factor * t);
                        delta_e_real_squared = (long) (delta_e_real_squared * (1 - smooth_factor) + smooth_factor * t * t);
                    }
                    else {
                        delta_e_real = (long) (delta_e_real * (1 - DELTA_E_REAL_AVG_ALPHA) + DELTA_E_REAL_AVG_ALPHA * t);
                        delta_e_real_squared = (long) (delta_e_real_squared * (1 - DELTA_E_REAL_AVG_ALPHA) + DELTA_E_REAL_AVG_ALPHA * t * t);
                    }
                    if (skip_first_interval) {      // skip the first very big interval
                        skip_first_interval = false;
                        delta_e_real_counter_avg--;
                        delta_e_real_counter_update--;
                        delta_e_real = 0;
                        delta_e_real_squared = 0;
                    }

                    // Update delta_s
                    if (delta_e_real_counter_update % DELTA_S_UPDATE_INTERVAL == 0) {
                        // Calculate variance of delta_e
                        long variance = delta_e_real_squared - delta_e_real * delta_e_real;

                        // if un-stable
                        if (variance > DELTA_E_REAL_VARIANCE_THRESHOLD) {
                            stable_period_counter = 0;
//                            if (last_stable_delta_s != 0) {
//                                delta_s = last_stable_delta_s;
//                                last_stable_delta_s = 0;
//                            }
//                            else
                                delta_s += DELTA_S_CLIMBING_STEP;
                        }
                        else {      // if stable
                            stable_period_counter++;
                            last_stable_delta_s = delta_s;
                            if (stable_period_counter >= KEEP_STABLE_ROUND) {       // up to 5 periods
                                stable_period_counter = 0;
                                delta_s -= DELTA_S_SHRINKING_STEP;
                                if (delta_s < 0)
                                    delta_s = 0;
                            }
                        }
                        Log.i("SSB", String.format("Variance=%d Ds=%d stable_period=%d", variance, delta_s, stable_period_counter));
                    }

                    MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(reply);
                    try {
                        long id = unpacker.unpackInt();                      // Task::id
//                        Log.i("FQ", "Received task id: " + id);
                        String appName = unpacker.unpackString();           // Task::appName
                        String modelName = unpacker.unpackString();         // Task::modelName
                        int bufferIndex = unpacker.unpackInt();             // Task::bufferIndex
                        // No status returned

                        // No inputNodes returned
                        // No inputValues returned
                        // No dims returned

                        int numOutputNodes = unpacker.unpackArrayHeader();  // Task::outputNodes
                        String[] outputNodes = new String[numOutputNodes];
                        for (int i = 0; i < numOutputNodes; i++)
                            outputNodes[i] = unpacker.unpackString().split(":")[0];

                        int numOutputs = unpacker.unpackMapHeader();        // Task::outputs
                        Map<String, float[]> outputs = new HashMap<>();
                        for (int i = 0; i < numOutputs; i++) {
                            String key = unpacker.unpackString().split(":")[0];
                            // if pack as array
//                            int n = unpacker.unpackArrayHeader();
//                            float[] value = new float[n];
//                            for (int j = 0; j < n; j++) {
//                                value[j] = unpacker.unpackFloat();
//                            }

                            // if pack as binary
                            int n = unpacker.unpackBinaryHeader();
                            float[] value = new float[n/4];
                            byte[] valueByte = unpacker.readPayload(n);
                            ByteBuffer valueBuffer = ByteBuffer.wrap(valueByte);
                            valueBuffer.asFloatBuffer().get(value);

                            outputs.put(key, value);
                        }
                        int numOdims = unpacker.unpackMapHeader();          // Task::odims
                        Map<String, long[]> odims = new HashMap<>();
                        for (int i = 0; i < numOdims; i++) {
                            String key = unpacker.unpackString();
                            int n = unpacker.unpackArrayHeader();
                            long[] value = new long[n];
                            for (int j = 0; j < n; j++)
                                value[j] = unpacker.unpackLong();
                            odims.put(key, value);
                        }

//                        Task replyTask = new Task(id, appName, null, null, null, outputNodes, odims, modelName);
                        Task replyTask = ibuffer.get((int) id);
                        replyTask.bufferIndex = bufferIndex;
                        replyTask.outputs = outputs;

                        replyTask.cost.computing = unpacker.unpackInt();
                        replyTask.cost.downloading = unpacker.unpackInt();
                        replyTask.cost.delta_s = delta_s;

                        // Callback - onResultHandler
                        Message onResultMsg = Message.obtain();
                        onResultMsg.obj = replyTask;

                        Bundle onResultBundle = new Bundle();
                        onResultBundle.putInt("deviceId", deviceId);
                        onResultMsg.setData(onResultBundle);

                        deviceManager.onResultHandler.sendMessage(onResultMsg);

                        ibuffer.delete((int) id);
                    }
                    catch (IOException e) {
                        Log.e("FQ", "IOException occurred when unpacking task!\n" + e.getMessage());
                    }
                }
            }
        };
        IThread.start();
        return SUCCESS;
    }

    @Override
    public int postprocess(int deviceId, Task task) {
        return 0;
    }

    @Override
    public int uploadModel(int deviceId, String modelFileName) {
        return 0;
    }

    @Override
    public boolean isAvailable(int deviceId) {
        return true;
    }
}
