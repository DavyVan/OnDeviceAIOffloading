package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import android.util.Log;

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
     *
     * \note    Last reviewed 2017.8.10 21:24
     */
    public Profiler() {
        databaseMap = new HashMap<String, StreamInfo>();
    }

    /**
     * \brief   Get information about a stream
     *
     * \note    Last reviewed 2017.8.10 21:24
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
     * \note    Last reviewed 2017.8.10 21:56
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
//            Log.i("COST", "Old cost:");
//            _cost.printToLog();
            if (_cost.isRemote) {
                if (_newCost.pre_process > 0) {
                    if (_cost.pre_process == 0)
                        _cost.pre_process = _newCost.pre_process;
                    else
                        _cost.pre_process = (int) (_cost.pre_process * SMOOTHING_FACTOR + _newCost.pre_process * (1 - SMOOTHING_FACTOR));
                }
                if (_newCost.uploading > 0) {
                    if (_cost.uploading == 0)
                        _cost.uploading = _newCost.uploading;
                    else
                        _cost.uploading = (int) (_cost.uploading * SMOOTHING_FACTOR + _newCost.uploading * (1 - SMOOTHING_FACTOR));
                }
                if (_newCost.downloading > 0) {
                    if (_cost.downloading == 0)
                        _cost.downloading = _newCost.downloading;
                    else
                        _cost.downloading = (int) (_cost.downloading * SMOOTHING_FACTOR + _newCost.downloading * (1 - SMOOTHING_FACTOR));
                }
                if (_newCost.post_process > 0) {
                    if (_cost.post_process == 0)
                        _cost.post_process = _newCost.post_process;
                    else
                        _cost.post_process = (int) (_cost.post_process * SMOOTHING_FACTOR + _newCost.post_process * (1 - SMOOTHING_FACTOR));
                }
            }
            if (_cost.computing == 0)
                _cost.computing = _newCost.computing;
            else
                _cost.computing = (int) (_cost.computing * SMOOTHING_FACTOR + _newCost.computing * (1 - SMOOTHING_FACTOR));

            // delta_s is passed straight.
            if (_newCost.delta_s != 0)
                _cost.delta_s = _newCost.delta_s;

            _cost.calculateSchedulingCost();
            _streamInfo.updateMaxCost();
//            Log.i("COST", "Updated cost:");
//            _cost.printToLog();
        }
    }

    public void print() {
        String ret = "";
        for (Map.Entry<String, StreamInfo> entry : databaseMap.entrySet()) {
            ret += String.format("Stream: %s\n", entry.getKey());
            for (int i = 0; i < entry.getValue().costs.size(); i++) {
                ret += String.format("Device-%d schedulingCost: %d\n", i, entry.getValue().costs.get(i).schedulingCost);
            }
        }
        Log.i("WIN", ret);
    }
}
