package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

/**
 * \brief   Store all of the configurable constants, e.g. buffer size, errno, etc.
 */
public final class Constant {
    public static final int BUFFER_SIZE = 100;

    // errno
    public static final int SUCCESS = 0;
    public static final int SYSTEM_NOT_INIT = 1;
    public static final int NO_DEVICE_AVAILABLE = 2;

    public static String getErrorMessage(int errno) {
        switch (errno) {
            case SUCCESS: return "Action completed successfully.";
            case SYSTEM_NOT_INIT: return "Offloading system is not initialized yet.";
            case NO_DEVICE_AVAILABLE: return "No device available after device discovery.";

            default: return "Unknown errno!";
        }
    }
}
