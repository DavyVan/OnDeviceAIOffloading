package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import java.util.Map;

/**
 * \brief   Store all of the configurable constants, e.g. buffer size, errno, etc.
 */
public final class Constant {
    public static final int BUFFER_SIZE = 100;

    // errno
    public static final int SUCCESS = 0;

    public static String getErrorMessage(int errno) {
        switch (errno) {
            case SUCCESS: return "Action completed successfully.";

            default: return "Unknown errno!";
        }
    }
}
