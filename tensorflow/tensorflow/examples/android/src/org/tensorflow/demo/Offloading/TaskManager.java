package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import static org.tensorflow.demo.Offloading.Constant.SUCCESS;

/**
 * \brief   Take over all of the execution of tasks, including get models ready (ModelManager),
 *          uploading data to server, starting the processing, listening and receiving the result, etc.
 */
public class TaskManager {

    private ModelManager modelManager;      /**< A ModelManager instance */
    private DeviceManager deviceManager;    /**< A DeviceManager instance that handle low layer data transmit */

    /**
     * \brief   Constructor.
     *
     *          Initialize the ModelManager
     */
    public TaskManager() {
        this.modelManager = new ModelManager();
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
}
