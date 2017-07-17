package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import static org.tensorflow.demo.Offloading.Constant.SUCCESS;

/**
 * \brief   Take over all of the execution of tasks, including get models ready (ModelManager),
 *          uploading data to server, starting the processing, listening and receiving the result, etc.
 */
public class TaskExecuteEngine {

    private ModelManager modelManager;      /**< A ModelManager instance */
    private DeviceManager deviceManager;    /**< A DeviceManager instance that handle low layer data transmit */
    private Scheduler scheduler;            /**< A Scheduler instance which will be called for getting next task */

    /**
     * \brief   Simple constructor.
     *
     *          Initialize the ModelManager, DeviceManager, Scheduler
     */
    public TaskExecuteEngine(ModelManager modelManager, DeviceManager deviceManager, Scheduler scheduler) {
        this.modelManager = modelManager;
        this.deviceManager = deviceManager;
        this.scheduler = scheduler;
    }

    /**
     * \brief   Execute a task.
     *
     *          Upload the raw data, launch the computing, fetch the result.
     *
     * \param   task        instance of task to be processed
     * \return  error number
     */
    public int runTask(Task task) {
        //todo:
        return SUCCESS;
    }

    /**
     * \brief   Start a group of threads and keep running to process tasks asynchronously
     * 
     * \return  error number
     */
    public int run() {
        // TODO: 17-7-17
        return SUCCESS;
    }
}
