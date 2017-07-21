package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import android.util.Log;

/**
 * \brief   Store all of the configurable constants, e.g. buffer size, errno, etc.
 */
public final class Constant {
    public static final int BUFFER_SIZE = 100;

    // Error
    public static final int SYSTEM_NOT_INIT = 1;
    public static final int NO_DEVICE_AVAILABLE = 2;

    // Info
    public static final int SUCCESS = 100;
    public static final int FRAME_DROPPED = 101;
    public static final int BUFFER_FULL = 102;
    public static final int TASK_NOT_EXIST = 103;
    public static final int WAIT_FOR_MODEL = 104;

    public static String getErrorMessage(int errno) {
        switch (errno) {
            case SUCCESS:               return "Action completed successfully.";
            case SYSTEM_NOT_INIT:       return "Offloading system is not initialized yet.";
            case NO_DEVICE_AVAILABLE:   return "No device available after device discovery.";
            case FRAME_DROPPED:         return "Current frame was dropped after dynamic sampling.";
            case BUFFER_FULL:           return "Buffer is full.";
            case TASK_NOT_EXIST:        return "Specific task doesn't exist any more. Maybe it's been cleaned.";
            case WAIT_FOR_MODEL:        return "Model is not ready yet, please wait when we uploading it.";

            default: return "Unknown errno!";
        }
    }

    public static void logIfError(int errno) {
        if (errno != SUCCESS) {
            if (isVitalError(errno))
                Log.e("FQ", getErrorMessage(errno));
            else
                Log.i("FQ", getErrorMessage(errno));
        }
    }

    public static boolean isVitalError(int errno) {
        return errno / 100 == 0;
    }
}
