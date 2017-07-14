package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-13.
 */

/**
 * \brief   All the offloading schedulers must extend this interface so that we can implement multiple
 *          scheduling policies.
 *
 * \note    Implementations of this interface must include several member variables (see Scheduler.java).
 */
public interface Scheduler {

//    Profiler profiler = null;                   /**< Instant of Profiler where Scheduler get info from */
//    ArrayList<Integer> currentWindows = null;   /**< As its name */
//    ArrayList<String> deviceName;               /**< Devices' name, keep the same order with Scheduler::currentWindows */

    /**
     * \brief   Calculate how many task each device should take in one scheduling circle.
     *
     *          All of the information needed by scheduling process will be fetched from Profiler
     */
    void calculateQuota();

    /**
     * \brief   Apply the quota(a.k.a. the sliding window) to the buffer
     *
     * \sa      OffloadingBuffer::changeWindow
     */
    void apply(OffloadingBuffer buffer);
}
