package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-17.
 */

import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

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

    @Override
    public int commit(Map<String, Float[]> data, String modelFileName, String appName) {

        // Make sure the system is initialized
        if (!isInitialized) {
            return SYSTEM_NOT_INIT;
        }
        //todo:
        return 0;
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
        return SUCCESS;
    }

    @Override
    public boolean isOffloadingAvailable() {
        //todo
        return true;
    }

    @Override
    public void setOnResultListener(OnResultListener listener) {
        //todo
    }
}
