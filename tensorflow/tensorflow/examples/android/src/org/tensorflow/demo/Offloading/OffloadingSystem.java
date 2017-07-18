package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-17.
 */

import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

import static org.tensorflow.contrib.android.TensorFlowInferenceInterface.ASSET_FILE_PREFIX;
import static org.tensorflow.demo.Offloading.Constant.BUFFER_FULL;
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

    @Override
    public int commit(String modelFileName, String appName,
                      ArrayList<String> inputNodes, ArrayList<float[]> inputValues, ArrayList<long[]> dims,
                      String[] outputNodes) {

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
        Task task = new Task(nextTaskId++, appName, inputNodes, inputValues, dims, outputNodes, modelName);

        // Initialize stream metadata if meet this stream at the first time
        if (profiler.fetchInfoByModel(modelName) == null) {
            StreamInfo streamInfo = new StreamInfo(modelName, appName, deviceManager.getAllDevices());
            profiler.updateInfo(modelName, streamInfo);
        }

        // Insert into buffer
        if (offloadingBuffer.insert(task, false) == BUFFER_FULL) {      // buffer is full, trigger the dynamic sampling
            dynamicSampler.calcSamplingRate(modelName);
            if (dynamicSampler.sample(modelName))
                offloadingBuffer.insert(task, true);
            else
                return FRAME_DROPPED;
        }
        return SUCCESS;
    }

    @Override
    public int init() {
        // Instantiate, keep the order
        profiler = new Profiler();
        offloadingBuffer = new OffloadingBuffer();
        // TODO: 17-7-17 Use JAVA Reflection to dynamic load different scheduler
        scheduler = new LCMScheduler(profiler, offloadingBuffer);
        dynamicSampler = (DynamicSampling) scheduler;
        deviceManager = new DeviceManager();
        modelManager = new ModelManager(deviceManager);
        taskExecuteEngine = new TaskExecuteEngine(modelManager, deviceManager, scheduler);

        // Initialize
        int errno = deviceManager.init();
        if (errno != SUCCESS) {
            Log.e("FQ", getErrorMessage(errno));
            return errno;
        }

        scheduler.init(deviceManager.getAllDevices().length);
        taskExecuteEngine.run();
        isInitialized = true;
        return SUCCESS;
    }

    @Override
    public boolean isOffloadingAvailable() {

        return deviceManager.isAvailable(1);        // only remote device is considered
    }

    @Override
    public void setOnResultListener(OnResultListener listener) {
        //todo
    }
}
