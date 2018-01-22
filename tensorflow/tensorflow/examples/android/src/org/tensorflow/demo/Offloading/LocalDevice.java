package org.tensorflow.demo.Offloading;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static org.tensorflow.demo.Offloading.Constant.SUCCESS;

/**
 * Created by fanquan on 17-7-17.
 */

public class LocalDevice extends DeviceAdapter {

    private Task currentTask;                   /**< Store the handle of task which are currently doing */
    private HandlerThread handlerThread;        /**< Run all computing in another thread */
    private Handler handler;                    /**< Handler for trigger the computing */

    private DeviceManager deviceManager;
    private TensorFlowInferenceInterface tf;    /**< Instance of TF, for demo only. Lazy created */

    /**
     * \note    Last reviewed 2017.8.10 21:37
     */
    public LocalDevice(DeviceManager deviceManager) {
        super(false, false, "Local");
        currentTask = null;
        this.deviceManager = deviceManager;
        tf = null;
    }

    /**
     * \note    Last reviewed 2017.8.10 21:24
     */
    @Override
    public int init() {

        // Create thread and handler
        handlerThread = new HandlerThread("LocalComputing");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        return SUCCESS;
    }

    @Override
    public int preprocess(int deviceId, Task task) {
        // Do nothing on local device
        return SUCCESS;
    }

    /**
     * \note    Last reviewed 2017.8.11 16:59
     */
    @Override
    public int uploadAndRun(final int deviceId, final Task task) {

        handler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // record task
                currentTask = task;
//                Log.i("FQ", "currentTask is assigned at thread " + Thread.currentThread().toString());

                // Run - initialize TF
                if (tf == null)
                    tf = new TensorFlowInferenceInterface(deviceManager.mainActivity.getAssets(), currentTask.modelName);

                // Run - feed
//                Log.i("FQ", "currentTask: " + currentTask.toString());
//                Log.i("FQ", "currentTask.inputNodes: " + currentTask.inputNodes.toString());
                int n = currentTask.inputNodes.size();
                for (int i = 0; i < n; i++) {
                    tf.feed(currentTask.inputNodes.get(i), currentTask.inputValues.get(i), currentTask.dims.get(i));
                }

                // Run
                long start = System.currentTimeMillis();
                tf.run(currentTask.outputNodes, true);
                long end = System.currentTimeMillis();
                currentTask.cost.computing = (int) (end - start);
                currentTask.cost.delta_s = currentTask.cost.computing;

//                Log.i("FQ", "This uploadAndRun():post, thread " + Thread.currentThread().toString());
                fetchResult(deviceId);
            }
        });

        return SUCCESS;
    }

    @Override
    public int startCompute(int deviceId) {
        return 0;
    }

    /**
     * \note    Last reviewed 2017.8.11 16:59
     */
    @Override
    public int fetchResult(int deviceId) {
//        Log.i("FQ", "This is fetchResult(), thread " + Thread.currentThread().getName());
        // Run - fetch
        for (String s : currentTask.outputNodes) {
            long outputsize = 1;
            for (long l : currentTask.odims.get(s))
                outputsize *= l;
            float[] t = new float[(int) outputsize];
            tf.fetch(s, t);
            currentTask.outputs.put(s, t);
        }

        // Callback - onResultHandler
        Message onResultMsg = Message.obtain();
        onResultMsg.obj = currentTask;

        Bundle onResultBundle = new Bundle();
        onResultBundle.putInt("deviceId", deviceId);
        onResultMsg.setData(onResultBundle);

        // Callback - callNextHandler
        Message callNextMsg = Message.obtain();

        Bundle callNextBundle = new Bundle();
        callNextBundle.putInt("deviceId", deviceId);
        callNextMsg.setData(callNextBundle);

        deviceManager.callNextHandler.sendMessage(callNextMsg);
        deviceManager.onResultHandler.sendMessage(onResultMsg);


        // clean currentTask
        currentTask = null;
//        Log.i("FQ", "currentTask is null at thread " + Thread.currentThread().toString());

        return 0;
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
