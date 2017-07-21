package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-15.
 */

/**
 * \brief   Define most functionalities that computing device should support
 *
 *          This interface will implemented by DeviceManager and all specific adapters.
 *          For specific adapter implementations, the deviceId parameter is useless and set -1.
 */
public abstract class DeviceAdapter {

    public String deviceName;       /**< String identifier of current device */
    public boolean isRemote;        /**< Indicate whether this device is remote or local */
    public boolean isParallel;      /**< Indicate whether this device can run tasks in parallel */
    public boolean isIdle;          /**< Indicate whether this device is idle or not */

    /**
     * \brief   Constructor called by specific adapter.
     *
     * \param   isRemote        As its name
     */
    public DeviceAdapter(boolean isRemote, boolean isParallel, String deviceName) {
        this.isRemote = isRemote;
        this.isParallel = isParallel;
        this.deviceName = deviceName;
        isIdle = false;
    }

    /**
     * \brief   Constructor called by DeviceManager
     */
    public DeviceAdapter() {
        isRemote = isParallel = isIdle = false;
        deviceName = "N/A";
    }

    /**
     * \brief   Initialize the device
     * 
     *          Connect to the device and get ready to receive data
     *          
     * \return  error number
     */
    public abstract int init();
    
    /**
     * \brief   Upload raw data to the server
     *
     * \param   deviceId        Which device will process the task
     * \param   task            Instance of task
     * \return  error number
     */
    public abstract int uploadTask(int deviceId, Task task);

    /**
     * \brief   Start the ready task on remote device.
     *
     *          If the raw data of a task and model it needs are already uploaded to server, we say
     *          this task is ready for start.
     *          For now, this method doesn't take any parameter. It's to say, we should keep this
     *          method will be called right after uploading is completed. And if possible, the server
     *          should launch all ready tasks or prepare to launch.
     *
     * \param   deviceId        Which device will process the task
     * \return  error number
     */
    public abstract int startCompute(int deviceId);

    /**
     * \brief   todo: maybe this is the callback method?
     *
     * \param   deviceId        Which device will process the task
     */
    public abstract int fetchResult(int deviceId);

    /**
     * \brief   As its name
     *
     * \param   deviceId        Which device will process the task
     * \param   modelFileName       As its name
     * \return  error number
     */
    public abstract int uploadModel(int deviceId, String modelFileName);

    /**
     * \brief   Return whether the compute device is available or not
     *
     * \param   deviceId        Which device will process the task
     * \return  a boolean
     */
    public abstract boolean isAvailable(int deviceId);
}
