package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-17.
 */

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

import static org.tensorflow.contrib.android.TensorFlowInferenceInterface.ASSET_FILE_PREFIX;
import static org.tensorflow.demo.Offloading.Constant.BUFFER_FULL;
import static org.tensorflow.demo.Offloading.Constant.Config.BUFFER_TYPE;
import static org.tensorflow.demo.Offloading.Constant.Config.ENABLE_FIXED_SAMPLE_RATE;
import static org.tensorflow.demo.Offloading.Constant.FRAME_DROPPED;
import static org.tensorflow.demo.Offloading.Constant.SUCCESS;
import static org.tensorflow.demo.Offloading.Constant.SYSTEM_NOT_INIT;
import static org.tensorflow.demo.Offloading.Constant.getErrorMessage;

/**
 * \brief   Main class of the On-Device Offloading System
 */
public class OffloadingSystem implements FrontInterface {

    public boolean isInitialized = false;
    private Profiler profiler;
    private OffloadingBuffer offloadingBuffer;
    private Scheduler scheduler;
    private DynamicSampling dynamicSampler;
    private DeviceManager deviceManager;
    private ModelManager modelManager;
    private TaskExecuteEngine taskExecuteEngine;
    private static long nextTaskId = 0;

    /**
     * \note    Last reviewed 2017.8.10 21:33
     */
    @Override
    public int commit(String modelFileName, String appName,
                      ArrayList<String> inputNodes, ArrayList<float[]> inputValues, ArrayList<long[]> dims,
                      String[] outputNodes, Map<String, long[]> odims) {

        // Make sure the system is initialized
        if (!isInitialized) {
            return SYSTEM_NOT_INIT;
        }

        // Generate model name from model file name
        boolean hasAssetPrefix = modelFileName.startsWith(ASSET_FILE_PREFIX);
        String modelName = (hasAssetPrefix ? modelFileName.split(ASSET_FILE_PREFIX)[1] : modelFileName);

        // Do sampling
        if (!dynamicSampler.sample(modelName)) {        // dropped
            return FRAME_DROPPED;
        }

        // Encapsulate into a Task
        Task task = new Task(nextTaskId++, appName, inputNodes, inputValues, dims, outputNodes, odims, modelName);

        // Initialize stream metadata if meet this stream at the first time
        if (profiler.fetchInfoByModel(modelName) == null) {
            StreamInfo streamInfo = new StreamInfo(modelName, appName, deviceManager.getAllDevices());
            profiler.updateInfo(streamInfo, -1);
        }

        // Insert into buffer
        if (offloadingBuffer.insert(task, false) == BUFFER_FULL) {      // buffer is full, trigger the dynamic sampling
            if (!ENABLE_FIXED_SAMPLE_RATE)
                dynamicSampler.calcSamplingRate(modelName);

            // clean all untouched tasks in buffer
//            Log.i("BUFFER", "Buffer status before cleanUntouchedTask()");
//            offloadingBuffer.printBuffer(0, 19);
            offloadingBuffer.cleanUntouchedTask();
            Log.i("BUFFER", "Buffer status after cleanUntouchedTask()");
            offloadingBuffer.printBuffer(0, 19);

            // Retry
            if (dynamicSampler.sample(modelName))
                offloadingBuffer.insert(task, false);       // Never use force insertion since we have clean strategy
            else
                return FRAME_DROPPED;
        }

        // Let TaskExecuteEngine check if there is a task could be ran by an idle device
        taskExecuteEngine.checkAndPushToIdle();

        return SUCCESS;
    }

    /**
     * \note    Last reviewed 2017.8.10 21:33
     */
    @Override
    public int init(Activity activity) {
        // Instantiate, keep the order
        profiler = new Profiler();
        deviceManager = new DeviceManager(activity);        // Constructor does nothing
        // Initialize devices
        int errno = deviceManager.init();
        if (errno != SUCCESS) {
            Log.e("FQ", getErrorMessage(errno));
            return errno;
        }
        if (BUFFER_TYPE.equals("Separated"))
            offloadingBuffer = new SeparatedOffloadingBuffer(deviceManager.getAllDevices().length);
        else if (BUFFER_TYPE.equals("Single"))
            offloadingBuffer = new OffloadingBuffer();
        else
            throw new RuntimeException("Unknown buffer type!");
        // TODO: 17-7-17 Use JAVA Reflection to dynamic load different scheduler
        scheduler = new LCMScheduler(profiler, offloadingBuffer);
        dynamicSampler = (DynamicSampling) scheduler;
        scheduler.init(deviceManager.getAllDevices().length);
        ((LCMScheduler) scheduler).setDeviceManager(deviceManager);
        modelManager = new ModelManager(deviceManager);
        taskExecuteEngine = new TaskExecuteEngine(modelManager, deviceManager, scheduler);

        isInitialized = true;
        return SUCCESS;
    }

    @Override
    public boolean isOffloadingAvailable() {

        return deviceManager.isAvailable(1);        // only remote device is considered
    }

    /**
     * \note    Last reviewed 2017.8.10 21:33
     */
    @Override
    public void setOnResultHandler(Handler handler) {
        taskExecuteEngine.setFrontEndHandler(handler);
    }
}
