package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import static org.tensorflow.demo.Offloading.Constant.SUCCESS;
import static org.tensorflow.demo.Offloading.Constant.WAIT_FOR_MODEL;
import static org.tensorflow.demo.Offloading.Constant.isVitalError;
import static org.tensorflow.demo.Offloading.Constant.logIfError;

/**
 * \brief   Take over all of the execution of tasks, including get models ready (ModelManager),
 *          uploading data to server, starting the processing, listening and receiving the result, etc.
 */
public class TaskExecuteEngine {

    private ModelManager modelManager;      /**< A ModelManager instance */
    private DeviceManager deviceManager;    /**< A DeviceManager instance that handle low layer data transmit */
    private Scheduler scheduler;            /**< A Scheduler instance which will be called for getting next task */

    private Handler callNextHandler;        /**< After a device uploaded a task, use this handler to send a message to TEE */
    private Handler onResultHandler;        /**< After result downloaded from a device, use this handler to tell TEE */

    private Handler frontEndHandler;

    /**
     * \brief   Simple constructor.
     *
     *          Initialize the ModelManager, DeviceManager, Scheduler
     *
     * \note    Last reviewed 2017.8.11 16:57
     */
    public TaskExecuteEngine(ModelManager modelManager, final DeviceManager deviceManager, Scheduler scheduler) {
        this.modelManager = modelManager;
        this.deviceManager = deviceManager;
        this.scheduler = scheduler;

        if (Looper.myLooper() == null) {
            Log.i("FQ", "Looper is not ready in current thread, preparing and starting loop...");
            Looper.prepare();
            Looper.loop();
        }

        frontEndHandler = null;

        callNextHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                int deviceId = bundle.getInt("deviceId");
                Log.i("FQ", "Device " + deviceManager.getAllDevices()[deviceId].deviceName + " call next task");

                // Ask scheduler for next task
                Task _next = TaskExecuteEngine.this.scheduler.next(deviceId);
                if (_next != null) {
                    logIfError(runTask(_next, deviceId));
                }
                else {      // If no task to do, mark isIdle = true
                    TaskExecuteEngine.this.deviceManager.markAsIdle(deviceId);
                    Log.i("FQ", "Device " + deviceManager.getAllDevices()[deviceId].deviceName + " is marked as idle");
                }
            }
        };

        onResultHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Task resultTask = (Task) msg.obj;
                Bundle bundle = msg.getData();
                int deviceId = bundle.getInt("deviceId");
                Log.i("FQ", "Result returned from device " + deviceManager.getAllDevices()[deviceId].deviceName + ", resultTask is -----------------------" + resultTask.id);

                // Tell scheduler task is done
                TaskExecuteEngine.this.scheduler.markAsDone(resultTask, deviceId);

                // Call back to user front end
                if (frontEndHandler != null) {
                    Message messageToFrontEnd = Message.obtain();
                    messageToFrontEnd.obj = resultTask;
                    frontEndHandler.sendMessage(messageToFrontEnd);
                }
            }
        };

        deviceManager.setHandlers(callNextHandler, onResultHandler);
    }

    /**
     * \brief   Execute a task.
     *
     *          Hand task in DeviceManager and done.
     *
     * \note    Last reviewed 2017.8.10 21:27
     *
     * \param   task        instance of task to be processed
     * \param   deviceId    device ID
     * \return  error number
     */
    public int runTask(Task task, int deviceId) {
        // Check model status
        if (!modelManager.isModelReady(task.modelName)) {       // Model is not get ready
            int errno = modelManager.getModelReady(task.modelName);
            logIfError(errno);
            if (isVitalError(errno))       // Terminate on vital error
                return errno;
            return WAIT_FOR_MODEL;
        }

        // Modify task's status
        task.status = 2;

        // Call DeviceManager
        deviceManager.preprocess(deviceId, task);
        int errno = deviceManager.uploadAndRun(deviceId, task);
        if (isVitalError(errno))
            return errno;
        else
            return SUCCESS;
    }

//    /**
//     * \brief   Start a group of threads and keep running to process tasks asynchronously
//     *
//     * \return  error number
//     */
//    public int run() {
//        return SUCCESS;
//    }

    /**
     * \brief   Check which device is idle and try to dispatch a task to it
     *
     * \note    Last reviewed 2017.8.10 21:24
     *
     * \return  error number
     */
    public int checkAndPushToIdle() {
        // Check which device is idle now
        DeviceAdapter[] devices = deviceManager.getAllDevices();
        for (int i = 0; i < devices.length; i++) {
            if (devices[i].isIdle) {        // If it's idle, try to ask scheduler for a task
                Log.i("IDLE", "Detected device-" + devices[i].deviceName + " is idle");
                Task _next = scheduler.next(i);
                if (_next != null) {
                    Log.i("FQ", "Run a new task on " + devices[i].deviceName);
                    logIfError(runTask(_next, i));
                }
            }
        }
        return SUCCESS;
    }

    /**
     * \note    Last reviewed 2017.8.10 21:24
     */
    public void setFrontEndHandler(Handler handler) {
        if (frontEndHandler == null)
            frontEndHandler = handler;
        else
            Log.i("FQ", "Front-end handler has been set.");
    }
}
