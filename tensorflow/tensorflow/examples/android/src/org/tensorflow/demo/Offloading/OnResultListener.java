package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-13.
 */

import java.util.ArrayList;

/**
 * \brief   To handle the logic after receive the result of TensorFlow
 */
public interface OnResultListener {
    /**
     * \brief   When computing is completed successfully.
     *
     * \param   result          Vector of result in Float.
     */
    void onResult(ArrayList<Float> result);

    /**
     * \brief   When error occurred.
     *
     * \param   errno           As its name.
     */
    void onError(int errno);
}
