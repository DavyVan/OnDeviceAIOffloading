package org.tensorflow.demo.Offloading;

import java.util.ArrayList;

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
     *          Allocate memory for devices list and call DeviceManager::init
     */
    public DeviceManager() {
        super();
        devices = new ArrayList<DeviceAdapter>();
        init();
    }

    /**
     * Discover all devices, add them into devices list and initialize them.
     */
    @Override
    public int init() {
        //todo:
        return 0;
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
     * True:    Exist any available device
     * False:   No available device
     */
    @Override
    public boolean isAvailable(int deviceId) {
        boolean ret = false;
        for (DeviceAdapter device : devices) {
            ret |= device.isAvailable(-1);
        }
        return ret;
    }

    /**
     * \brief   Return identifiers of all available devices
     *
     *          Only device name is returned (index implies the device id
     *
     * \return  devices' name list in String
     */
    public String[] getAllDevices() {
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            names[i] = devices.get(i).getClass().getName();
        }
        return names;
    }
}
