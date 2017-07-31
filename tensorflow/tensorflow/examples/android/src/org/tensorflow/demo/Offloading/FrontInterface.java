package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-12.
 */

import android.app.Activity;
import android.os.Handler;
import java.util.ArrayList;
import java.util.Map;

/**
 * \brief   Contains the main API of On-Device offloading framework.
 *
 *          Users(developers) only need to call methods in this, and the framework take over all the
 *          staff of computing.
 */
public interface FrontInterface {
    /**
     * \brief   Users call this method to commit a task, including data and model, then the framework
     *          handle the remaining.
     *
     * \param   data              Input data in Map with Tensor's name as key and value as value.
     * \param   modelFileName     As its name.
     * \param   appName           Caller's name.
     * \return  errno
     */
    int commit(String modelFileName, String appName,
               ArrayList<String> inputNodes, ArrayList<float[]> inputValues, ArrayList<long[]> dims,
               String[] outputNodes, Map<String, long[]> odims);

    /**
     * \brief   Initialize the whole system.
     *
     *          Instantiate all components, detect available compute devices and connect to them.
     *
     * \return  errno
     */
    int init(Activity activity);

    /**
     * \brief   Query the availability of offloading functionality.
     *
     * \return  Whether they're available or not.
     */
    boolean isOffloadingAvailable();

    /**
     * \brief   As its name.
     *
     * \param   handler     The handler which handle the result of computing.
     */
    void setOnResultHandler(Handler handler);
}
