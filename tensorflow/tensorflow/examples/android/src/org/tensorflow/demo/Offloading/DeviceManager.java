package org.tensorflow.demo.Offloading;

import android.util.Log;

import java.util.ArrayList;

import static org.tensorflow.demo.Offloading.Constant.NO_DEVICE_AVAILABLE;
import static org.tensorflow.demo.Offloading.Constant.SUCCESS;
import static org.tensorflow.demo.Offloading.Constant.getErrorMessage;

/**
 * Created by fanquan on 17-7-15.
 */

/**
 * \brief   Manage all the computing device.
 * 
 *          Discover, connect, transmit, keep alive, query.
 */
public class DeviceManager extends DeviceAdapter {

    private ArrayList<DeviceAdapter> devices;       /**< All the devices information are stored here */

    /**
     * \brief   Constructor.
     *
     *          Allocate memory for devices list
     */
    public DeviceManager() {
        super();
        devices = new ArrayList<DeviceAdapter>();
    }

    /**
     * Discover all devices, add them into devices list and initialize them.
     */
    @Override
    public int init() {
        // Scan all possible devices
        // 1. local
        DeviceAdapter localDevice = new LocalDevice();
        int errno = localDevice.init();
        if (errno == SUCCESS)
            devices.add(localDevice);
        else
            Log.e("FQ", getErrorMessage(errno));

        // 2. Wi-Fi
        DeviceAdapter wifiDevice = new WiFiDevice();
        errno = wifiDevice.init();
        if (errno == SUCCESS)
            devices.add(wifiDevice);
        else
            Log.e("FQ", getErrorMessage(errno));

        if (devices.size() == 0)
            return NO_DEVICE_AVAILABLE;
        else
            return SUCCESS;
    }

    @Override
    public int uploadTask(int deviceId, Task task) {
        // forward directly
        return devices.get(deviceId).uploadTask(-1, task);
    }

    @Override
    public int startCompute(int deviceId) {
        // forward directly
        return devices.get(deviceId).startCompute(-1);
    }

    @Override
    public int fetchResult(int deviceId) {
        // forward directly
        return devices.get(deviceId).fetchResult(-1);
    }

    @Override
    public int uploadModel(int deviceId, String modelFileName) {
        // forward directly
        return devices.get(deviceId).uploadModel(-1, modelFileName);
    }

    /**
     * \note    deviceId is used to indicate only remote device is considered(set to 1) or not(set to 0).
     * True:    Exist any available device
     * False:   No available device
     */
    @Override
    public boolean isAvailable(int deviceId) {
        boolean ret = false;
        for (DeviceAdapter device : devices) {
            if ((deviceId == 0) || (deviceId == 1 && device.isRemote == true)) {
                ret |= device.isAvailable(-1);
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
        return (DeviceAdapter[]) devices.toArray();
    }

    public void markAsIdle(int deviceId) {
        devices.get(deviceId).isIdle = true;
    }
}
