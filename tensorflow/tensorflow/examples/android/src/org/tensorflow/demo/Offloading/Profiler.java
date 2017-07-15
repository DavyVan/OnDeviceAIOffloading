package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * \brief   Manage information about data streams
 *
 *          Handle info I/O and the database(a Map<String, StreamInfo>)
 */
public class Profiler {

    private Map<String, StreamInfo> databaseMap;        /**< Use a Map to store the information */

    /**
     * /brief   Constructor. Initialize the database.
     */
    public Profiler() {
        databaseMap = new HashMap<String, StreamInfo>();
    }

    /**
     * \brief   Get infomation about a stream
     *
     * \param   modelName       Model file name
     * \return  A StreamInfo contains information about the stream that the model indicates
     */
    public StreamInfo fetchInfo(String modelName) {
        return databaseMap.get(modelName);
    }

    /**
     * \brief   Update new information about a data stream
     *
     *          This method is usually called by TaskManager after a task is done.
     *
     * \param   modelName       Model file name, identifying the data stream
     * \param   newInfo         StreamInfo instance contain the latest information about the stream
     */
    public void updateInfo(String modelName, StreamInfo newInfo) {
        //todo: this is not a simple replacement. need some stat calculation.
    }
}
