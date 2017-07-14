package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-13.
 */

import java.util.Map;

/**
 * \brief   Act as a struct. Store information about offloading tasks.
 */
public class Task {
    public String appName;                      /**< Which app commit this task */
    public Map<String, Float[]> rawData;        /**< Data to be processed */
    public boolean isDone;                      /**< Whether it has been processed */
    public String modelName;                    /**< Model file name */
    public String modelFileName;                /**< Model file path */
    public int bufferIndex;                     /**< Index in buffer */
}
