package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.tensorflow.demo.Offloading.Constant.SUCCESS;

/**
 * \brief   Upload models, query status.
 *
 *          For now, it's a dummy class do nothing.
 */
public class ModelManager {

    private DeviceManager deviceManager;        /**< A DeviceManager instance to be used to perform transmittion */
    private Map<String, ArrayList<Integer>> modelCache;      /**< A list of models that are already exist on device, <modelName, deviceIds> */

    /**
     * \brief   Simple constructor
     */
    public ModelManager(DeviceManager deviceManager) {
        this.deviceManager = deviceManager;
        modelCache = new HashMap<>();
    }

    /**
     * \brief   Query the server whether it has the model or not
     *
     * \param   modelName       Name of model file
     * \return  Whether the server has the model or not
     * \note    As a dummy, always return true
     */
    public boolean isModelReady(String modelName) {
        //todo: dummy
        return true;
    }

    /**
     * \brief   Upload the model to the server.
     *
     *          The uploading act asynchronously by returning immediately.
     *
     * \param   modelName       Name of model file
     * \return  error number
     */
    public int getModelReady(String modelFileName) {
        //todo: implement asynchronously, this method will return immediately. Need a listener to call back
        return SUCCESS;
    }
}
