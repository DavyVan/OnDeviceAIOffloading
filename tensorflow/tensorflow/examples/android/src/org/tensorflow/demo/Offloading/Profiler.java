package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import java.util.HashMap;
import java.util.Map;

import static org.tensorflow.demo.Offloading.Constant.Config.SMOOTHING_FACTOR;

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
     * \brief   Get information about a stream
     *
     * \param   modelName       Model file name
     * \return  A StreamInfo contains information about the stream that the model indicates
     */
    public StreamInfo fetchInfoByModel(String modelName) {
        return databaseMap.get(modelName);
    }

    /**
     * \brief   Update new information about a data stream
     *
     *          This method is usually called by TaskExecuteEngine after a task is done.
     *
     * \param   newInfo         StreamInfo instance contain the latest information about the stream
     * \param   deviceId        Additionally, we need device ID to know which device just processed the task
     */
    public void updateInfo(StreamInfo newInfo, int deviceId) {

        StreamInfo _streamInfo = databaseMap.get(newInfo.modelName);

        // if not exist, just insert
        if (_streamInfo == null) {
            databaseMap.put(newInfo.modelName, newInfo);
        }
        else {      // update
            // 1-Exponential Smoothing: y'_(t+1) = y_(t) * alpha + y'_(t) * (1 - alpha)
            StreamInfo.Cost _cost = _streamInfo.costs.get(deviceId);
            StreamInfo.Cost _newCost = newInfo.costs.get(0);
            if (_cost.isRemote) {
                _cost.pre_process = (int) (_cost.pre_process * SMOOTHING_FACTOR + _newCost.pre_process * (1 - SMOOTHING_FACTOR));
                _cost.uploading = (int) (_cost.uploading * SMOOTHING_FACTOR + _newCost.uploading * (1 - SMOOTHING_FACTOR));
                _cost.downloading = (int) (_cost.downloading * SMOOTHING_FACTOR + _newCost.downloading * (1 - SMOOTHING_FACTOR));
                _cost.post_process = (int) (_cost.post_process * SMOOTHING_FACTOR + _newCost.post_process * (1 - SMOOTHING_FACTOR));
            }
            _cost.computing = (int) (_cost.computing * SMOOTHING_FACTOR + _newCost.computing * (1 - SMOOTHING_FACTOR));
            _cost.calculateSchedulingCost();
        }
    }
}
