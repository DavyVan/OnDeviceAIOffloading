package org.tensorflow.demo.Offloading;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;

import static org.tensorflow.demo.Offloading.Constant.Config.ONLY_REMOTE;
import static org.tensorflow.demo.Offloading.Constant.NO_DEVICE_AVAILABLE;
import static org.tensorflow.demo.Offloading.Constant.SUCCESS;
import static org.tensorflow.demo.Offloading.Constant.getErrorMessage;

/**
 * Created by fanquan on 17-7deviceId5.
 */

/**
 * \brief   Manage all the computing device.
 * 
 *          Discover, connect, transmit, keep alive, query.
 */
public class DeviceManager {

    private ArrayList<DeviceAdapter> devices;       /**< All the devices information are stored here */

    public Handler callNextHandler;
    public Handler onResultHandler;
    public Activity mainActivity;

    /**
     * \brief   Constructor.
     *
     *          Allocate memory for devices list
     *
     * \note    Last reviewed 2017.8.10 21:24
     */
    public DeviceManager(Activity activity) {
        super();
        devices = new ArrayList<DeviceAdapter>();
        callNextHandler = null;
        onResultHandler = null;
        mainActivity = activity;
    }

    /**
     * Discover all devices, add them into devices list and initialize them.
     * \note    Last reviewed 2017.8.10 21:24
     */
    public int init() {
        // Scan all possible devices
        int errno;
        // 1. local
        if (ONLY_REMOTE == 0) {
            DeviceAdapter localDevice = new LocalDevice(this);
            errno = localDevice.init();
            if (errno == SUCCESS) {
                devices.add(localDevice);
                localDevice.id = devices.size() - 1;
            }
            else
                Log.e("FQ", getErrorMessage(errno));
        }

        // 2. Wi-Fi
        DeviceAdapter wifiDevice = new WiFiDevice(this);
        errno = wifiDevice.init();
        if (errno == SUCCESS) {
            devices.add(wifiDevice);
            wifiDevice.id = devices.size() - 1;
        }
        else
            Log.e("FQ", getErrorMessage(errno));

        if (devices.size() == 0)
            return NO_DEVICE_AVAILABLE;
        else
            return SUCCESS;
    }

    public int preprocess(int deviceId, Task task) {
        // forward directly
        return devices.get(deviceId).preprocess(deviceId, task);
    }

    public int uploadAndRun(int deviceId, Task task) {
        // forward directly
        devices.get(deviceId).isIdle = false;
        return devices.get(deviceId).uploadAndRun(deviceId, task);
    }

//    public int startCompute(int deviceId) {
//        // forward directly
//        return devices.get(deviceId).startCompute(deviceId);
//    }

//    public int fetchResult(int deviceId) {
//        // forward directly
//        return devices.get(deviceId).fetchResult(deviceId);
//    }

    public int postprocess(int deviceId, Task task) {
        // forward directly
        return devices.get(deviceId).postprocess(deviceId, task);
    }

    public int uploadModel(int deviceId, String modelFileName) {
        // forward directly
        return devices.get(deviceId).uploadModel(deviceId, modelFileName);
    }

    /**
     * \note    deviceId is used to indicate only remote device is considered(set to 1) or not(set to 0).
     * True:    Exist any available device
     * False:   No available device
     */
    public boolean isAvailable(int deviceId) {
        boolean ret = false;
        for (DeviceAdapter device : devices) {
            if ((deviceId == 0) || (deviceId == 1 && device.isRemote == true)) {
                ret |= device.isAvailable(deviceId);
            }
        }
        return ret;
    }

    /**
     * \brief   Return all available devices
     *
     * \return  devices' list
     */
    public DeviceAdapter[] getAllDevices() {
        return (DeviceAdapter[]) devices.toArray(new DeviceAdapter[0]);
    }

    public void markAsIdle(int deviceId) {
        devices.get(deviceId).isIdle = true;
    }

    public void setHandlers(Handler callNextHandler, Handler onResultHandler) {
        this.callNextHandler = callNextHandler;
        this.onResultHandler = onResultHandler;
    }
}
