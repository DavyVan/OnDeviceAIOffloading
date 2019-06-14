package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-13.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * \brief   Act as a struct. Store information about offloading tasks.
 */
public class Task {

    // Metadata
    public long id;                             /**< Unique ID */
    public String appName;                      /**< Which app commit this task */
    public String modelName;                    /**< Model file name */
    public int bufferIndex;                     /**< Index in buffer */
    public int status;                          /**< 0:untouched,
                                                     1:scheduled,
                                                     2:uploaded or pending for uploading,
                                                     3:done */

    // Input
    public ArrayList<String> inputNodes;         /**< Input node tag */
    public ArrayList<float[]> inputValues;      /**< Input values */
    public ArrayList<long[]> dims;              /**< Dimensions of input values */

    // Output
    public String[] outputNodes;                /**< output node tag */
    public Map<String, float[]> outputs;        /**< Result data */
    public Map<String, long[]> odims;           /**< Dimensions of output values */

    // Feed back
    public StreamInfo.Cost cost;                /**< Cost instance tracking time cost for profiling */

    /**
     * \brief   Simple constructor, assign member variables
     *
     * \param   id              ::id
     * \param   appName         ::appName
     * \param   inputNode       ::inputNode
     * \param   inputValues     ::inputValues
     * \param   dims            ::dims
     * \param   outputNodes     ::outputNodes
     * \param   modelName       ::modelName
     * \param   modelFileName   ::modelFileName
     */
    public Task(long id,
                String appName,
                ArrayList<String> inputNodes,
                ArrayList<float[]> inputValues,
                ArrayList<long[]> dims,
                String[] outputNodes,
                Map<String, long[]> odims,
                String modelName) {
        this.id = id;
        this.appName = appName;
        this.inputNodes = inputNodes;
        this.inputValues = inputValues;
        this.dims = dims;
        this.outputNodes = outputNodes;
        this.odims = odims;
        this.modelName = modelName;

        status = 0;
        bufferIndex = -1;
        outputs = new HashMap<>();
        cost = new StreamInfo.Cost();
    }
}
