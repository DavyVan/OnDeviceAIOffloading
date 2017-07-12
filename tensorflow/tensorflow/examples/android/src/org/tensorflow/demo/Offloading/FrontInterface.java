package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-12.
 */

/**
 * \brief Contains the main API of On-Device offloading framework.
 *
 * Users(developers) only need to call methods in this, and the framework take over all the staff of
 * computing.
 */
public interface FrontInterface {
    public int commit();  //todo: params?
}
