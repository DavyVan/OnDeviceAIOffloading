package com.example.fanquan.a0mqtest;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ZMQ.Context context;
    ZMQ.Socket requester;
    int count = 0;
    HandlerThread handlerThread;
    Handler handler;
//    String m = "";
//    float[] m;
    byte[] serializedData;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Prepare data
//        for(int i = 0; i < 10000; i++)
//            m += i;
//        Log.i("FQ", m);

//        m = new float[20*20*3];
//        for (int i = 0; i < 20*20*3; i++)
//            m[i] = i;
//        m[0]=221;
//        Log.i("FQ", "Data inited.");

        serializedData = packing(createTask());
        Log.i("FQ", "serialized data length: " + serializedData.length);

        handlerThread = new HandlerThread("NIO");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // ZMQ
        handler.post(new Runnable() {
            @Override
            public void run() {
                context = ZMQ.context(1);
                Log.i("FQ", "Connecting to server...");
                requester = context.socket(ZMQ.REQ);
                requester.connect("tcp://192.168.0.241:5555");
                Log.i("FQ", "Connected.");

//                requester.send(("Hello" + (count++) + m).getBytes(), 0);
//                requester.send(serializedData, 0);

//                byte[] reply = requester.recv(0);
//                Log.i("FQ", "Received: " + new String(reply).length());
//                if (reply.length != 0)
//                    unpacking(reply);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("FQ", "Clicked and sending...");

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        requester.send(serializedData, 0);

                        byte[] reply = requester.recv(0);
                        Log.i("FQ", "Received: " + new String(reply).length());
                        if (reply.length != 0)
                            unpacking(reply);

                        // To test serialize float array as binary for better performance
//                        try {
//                            // Pack as binary
//                            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
//                            long startt = System.currentTimeMillis();
//                            packer.packBinaryHeader(20*20*3*4);
//                            ByteBuffer byteBuffer = ByteBuffer.allocate(20*20*3*4);
//                            FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
//                            floatBuffer.put(m);
//                            packer.writePayload(byteBuffer.array());
//                            packer.flush();
//                            long endt = System.currentTimeMillis();
//                            Log.i("FQ", "Pack as byte time: " + (endt - startt));
//                            serializedData = packer.toByteArray();
//                            packer.close();
//
//                            // send
//                            requester.send(serializedData, 0);
//
//                            byte[] reply = requester.recv(0);
//                            Log.i("FQ", "Received: " + reply.length);
//
//                            // try to unpack
//                            MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(reply);
//                            startt = System.currentTimeMillis();
//                            int binaryHeader = unpacker.unpackBinaryHeader();
//                            Log.i("FQ", "BinaryHeader is " + binaryHeader);
//                            byte[] result = unpacker.readPayload(binaryHeader);
//                            ByteBuffer resultBuffer = ByteBuffer.wrap(result);
//                            float[] finalResult = new float[binaryHeader/4];
//                            resultBuffer.asFloatBuffer().get(finalResult);
//                            endt = System.currentTimeMillis();
//                            Log.i("FQ", "Unpacking is finished in " + (endt - startt));
//                            Log.i("FQ", "Final result is " + finalResult[0] + " " + finalResult[1]);
//                        }
//                        catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private Task createTask() {
        // inputNodes
        ArrayList<String> inputNodes = new ArrayList<>();
        inputNodes.add("inputNodes_A");
        inputNodes.add("inputNodes_B");

        // inputValues
        ArrayList<float[]> inputValues = new ArrayList<>();
        inputValues.add(new float[]{1.0f, 1.1f, 1.3f});
        inputValues.add(new float[]{2.0f, 2.1f, 2.2f, 2.3f});

        // dims
        ArrayList<long[]> dims = new ArrayList<>();
        dims.add(new long[]{1, 3});
        dims.add(new long[]{2, 2});

        // outputNodes
        String[] outputNodes = new String[]{"output_1", "output_2"};

        // odims
        Map<String, long[]> odims = new HashMap<>();
        odims.put("output_1", new long[]{1, 1});
        odims.put("output_2", new long[]{2, 2});

        Task t = new Task(123, "testTask", inputNodes, inputValues, dims, outputNodes, odims, "testModel.pb");
        t.cost.pre_process = 1;
        t.cost.uploading = 2;
        t.cost.computing = 3;
        t.cost.downloading = 4;
        t.cost.post_process = 5;
        t.cost.calculateSchedulingCost();

        return t;
    }

    private byte[] packing(Task task) {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        try {
            packer.packLong(task.id);
            packer.packString(task.appName);
            packer.packString(task.modelName);
            // no Task.status

            packer.packArrayHeader(task.inputNodes.size());
            for (String s : task.inputNodes) {
                packer.packString(s);
            }

            // pack as array
//            packer.packArrayHeader(task.inputValues.size());
//            for (float[] x : task.inputValues) {
//                packer.packArrayHeader(x.length);       // should calculate dims to length for accuracy
//                for (float f : x) {
//                    packer.packFloat(f);
//                }
//            }
            // pack as binary
            packer.packArrayHeader(task.inputValues.size());
            for (float[] array : task.inputValues) {
                int byteNum = array.length * 4;     // 4 bytes per float
                ByteBuffer byteBuffer = ByteBuffer.allocate(byteNum);
                FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
                floatBuffer.put(array);

                packer.packBinaryHeader(byteNum);
                packer.writePayload(byteBuffer.array());
            }

            packer.packArrayHeader(task.dims.size());
            for (long[] x : task.dims) {
                packer.packArrayHeader(x.length);
                for (long l : x) {
                    packer.packLong(l);
                }
            }

            packer.packArrayHeader(task.outputNodes.length);
            for (String s : task.outputNodes) {
                packer.packString(s);
            }
            // no outputs
            packer.packMapHeader(task.odims.size());
            for (Map.Entry<String, long[]> entry : task.odims.entrySet()) {     // should check the map size
                packer.packString(entry.getKey());
                packer.packArrayHeader(entry.getValue().length);
                for (long l : entry.getValue()) {
                    packer.packLong(l);
                }
            }

            // pack StreamInfo.Cost only for test
            packer.packInt(task.cost.pre_process);
            packer.packInt(task.cost.uploading);
            packer.packInt(task.cost.computing);
            packer.packInt(task.cost.downloading);
            packer.packInt(task.cost.post_process);
            packer.packInt(task.cost.schedulingCost);

            packer.flush();
            byte[] ret = packer.toByteArray();
            packer.close();
            return ret;
        }
        catch (IOException e) {
            Log.e("FQ", e.getMessage());
        }
        return new byte[88];
    }

    private void unpacking(byte[] data) {
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
        try {
            Log.i("FQ", "Unpacked ID: " + unpacker.unpackInt());
            Log.i("FQ", "Unpacked appName: " + unpacker.unpackString());
            Log.i("FQ", "Unpacked modelName: " + unpacker.unpackString());

            int numOutputNodes = unpacker.unpackArrayHeader();
            String[] outputNodes = new String[numOutputNodes];
            for (int i = 0; i < numOutputNodes; i++)
                outputNodes[i] = unpacker.unpackString();
            Log.i("FQ", "Unpacked outputNodes: " + Arrays.toString(outputNodes));

            int numOutputs = unpacker.unpackMapHeader();
            Map<String, float[]> outputs = new HashMap<>();
            for (int i = 0; i < numOutputs; i++) {
                String key = unpacker.unpackString();
                // if pack as array
//                int n = unpacker.unpackArrayHeader();
//                float[] value = new float[n];
//                for (int j = 0; j < n; j++) {
//                    value[j] = unpacker.unpackFloat();
//                }

                // if pack as binary
                int n = unpacker.unpackBinaryHeader();
                float[] value = new float[n/4];
                byte[] valueByte = unpacker.readPayload(n);
                ByteBuffer valueBuffer = ByteBuffer.wrap(valueByte);
                valueBuffer.asFloatBuffer().get(value);

                outputs.put(key, value);
            }
            Log.i("FQ", "Unpacked outputs: ");
            for (String s : outputs.keySet())
                Log.i("FQ", "" + Arrays.toString(outputs.get(s)));

            int numOdims = unpacker.unpackMapHeader();
            Map<String, long[]> odims = new HashMap<>();
            for (int i = 0; i < numOdims; i++) {
                String key = unpacker.unpackString();
                int n = unpacker.unpackArrayHeader();
                long[] value = new long[n];
                for (int j = 0; j < n; j++)
                    value[j] = unpacker.unpackLong();
                odims.put(key, value);
            }
            Log.i("FQ", "Unpacked odims: " + odims);

            Log.i("FQ", "Unpacked costComputing: " + unpacker.unpackInt());
        }
        catch (IOException e) {
            Log.e("FQ", e.getMessage());
        }
    }
}
