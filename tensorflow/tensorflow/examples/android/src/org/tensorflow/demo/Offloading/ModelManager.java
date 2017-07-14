package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-14.
 */

import static org.tensorflow.demo.Offloading.Constant.SUCCESS;

/**
 * \brief   Upload models, query status.
 *
 *          For now, it's a dummy class do nothing.
 */
public class ModelManager {

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
