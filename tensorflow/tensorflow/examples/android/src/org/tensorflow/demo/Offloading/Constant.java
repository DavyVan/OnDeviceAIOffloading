package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import android.util.Log;

/**
 * \brief   Store all of the configurable constants, e.g. buffer size, errno, etc.
 */
public final class Constant {

    public static class Config {
        public static final int BUFFER_SIZE = 100;

        // Profiler
        // 1-Exponential Smoothing
        public static final float SMOOTHING_FACTOR = 0.5f;      /**< factor on old data */

        // Network
        public static final String SERVER_IP = "192.168.0.126";
        public static final int SERVER_PORT = 2333;
    }

    // Error
    public static final int SYSTEM_NOT_INIT = 1;
    public static final int NO_DEVICE_AVAILABLE = 2;
    public static final int IO_EXCEPTION = 3;

    // Info
    public static final int SUCCESS = 100;
    public static final int FRAME_DROPPED = 101;
    public static final int BUFFER_FULL = 102;
    public static final int TASK_NOT_EXIST = 103;
    public static final int WAIT_FOR_MODEL = 104;

    public static String getErrorMessage(int errno) {
        switch (errno) {
            // Error
            case SYSTEM_NOT_INIT:       return "Offloading system is not initialized yet.";
            case NO_DEVICE_AVAILABLE:   return "No device available after device discovery.";
            case IO_EXCEPTION:          return "Vital IO exception occurred.";

            // Info
            case SUCCESS:               return "Action completed successfully.";
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

    public static class Tools {
        private static double gcd(double a, double b) {
            while (b > 0) {
                double t = b;
                b = a % b;
                a = t;
            }
            return a;
        }

        private static double lcm(double a, double b) {
            return a * (b / gcd(a, b));
        }

        public static double lcm(double[] input) {
            double result = input[0];
            for (int i = 1; i < input.length; i++)
                result = lcm(result, input[i]);
            return result;
        }

        public static double min(double[] input) {
            double t = input[0];
            for (int i = 1; i < input.length; i++)
                t = Math.min(t, input[i]);
            return t;
        }
    }
}
