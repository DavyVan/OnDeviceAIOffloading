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
        public static final int BUFFER_SIZE = 10;

        // Profiler
        // 1-Exponential Smoothing
        public static final float SMOOTHING_FACTOR = 0.5f;      /**< factor on old data */

        // Network
        public static final String SERVER_IP = "192.168.0.240";
        public static final int SERVER_PORT = 2333;
        public static final int SEND_DELAY_MS = 0;

        // Scheduler
        public static final int INIT_SAMPLE_INTERVAL = 200;
        public static final boolean ENABLE_FIXED_SAMPLE_RATE = true;
        public static final int[] INIT_WINS = {1, 1};               /**< {Local, WiFi} */
        public static final String BUFFER_TYPE = "Separated";       /**< {Separated|Single} */
        public static final String BUFFER_CLEAN_TYPE = "Intersection";      /**< {All|Intersection} */

        // Smooth Send-Back
        public static final int[] DEVICE_CONCURRENCY_NUMS = {1, 4};     /**< {Local, WiFi} */
        public static final int DELTA_E_REAL_AVG_NUM = 16;
        public static final float DELTA_E_REAL_AVG_ALPHA = 1.0f / DELTA_E_REAL_AVG_NUM;
//        public static final float DELTA_S_CALIBRATION_FACTOR = 0.1f;      // deprecated in design v3.0
        public static final int DELTA_S_UPDATE_INTERVAL = 8;
        public static final int DELTA_S_CLIMBING_STEP = 50;         /**< in milliseconds */
        public static final int DELTA_S_SHRINKING_STEP = 10;        /**< in milliseconds */
        public static final int DELTA_E_REAL_VARIANCE_THRESHOLD = 70000;
        public static final int KEEP_STABLE_ROUND = 2;
        /**
         * SEND_DELAY_MS=250
         * AVG_NUM=50
         * lr=0.01
         * with buffer cleaning
         * Very good!
         */
    }

    // Error
    public static final int SYSTEM_NOT_INIT = 1;
    public static final int NO_DEVICE_AVAILABLE = 2;
    public static final int IO_EXCEPTION = 3;
    public static final int INTERRUPTED_EXCEPTION = 4;

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
            case INTERRUPTED_EXCEPTION: return "Vital Interrupted exception occurred.";

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

        public static void delay(long time) {
            try {
                Thread.sleep(time);
            }
            catch (InterruptedException e) {
                Log.e("FQ", e.getMessage());
            }
        }
    }
}
