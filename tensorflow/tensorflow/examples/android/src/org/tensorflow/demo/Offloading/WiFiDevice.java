package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-17.
 */

public class WiFiDevice extends DeviceAdapter {
    public WiFiDevice() {
        super(true, true, "WiFi");
    }

    @Override
    public int init() {
        // TODO: 17-7-17
        return 0;
    }

    @Override
    public int uploadTask(int deviceId, Task task) {
        // TODO: 17-7-17
        return 0;
    }

    @Override
    public int startCompute(int deviceId) {
        // TODO: 17-7-17
        return 0;
    }

    @Override
    public int fetchResult(int deviceId) {
        // TODO: 17-7-17
        return 0;
    }

    @Override
    public int uploadModel(int deviceId, String modelFileName) {
        // TODO: 17-7-17
        return 0;
    }

    @Override
    public boolean isAvailable(int deviceId) {
        // TODO: 17-7-17
        return false;
    }
}
