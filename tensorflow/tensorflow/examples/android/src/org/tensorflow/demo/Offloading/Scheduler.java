package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-13.
 */

import java.util.ArrayList;

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
     *
     * \param   modelName       Identify the stream
     */
    void calculateQuota(String modelName);

    /**
     * \brief   Apply the quota(a.k.a. the sliding window) to the buffer
     *
     * \sa      OffloadingBuffer::changeWindow
     */
    void apply();

    /**
     * \brief   Get next Task to be processed.
     *          
     *          This method is supposed to be called by TaskExecuteEngine
     * 
     * \param   deviceId        As its name
     * \return  Task instance
     */
    Task next(int deviceId);

    /**
     * \brief   Mark a Task as completed, then re-schedule if needed.
     * 
     * \param   task        Which task was just completed
     * \param   deviceId    Which device processed this task
     */
    void markAsDone(Task task, int deviceId);

    /**
     * \brief   Getter for profiler instance.
     * 
     *          This means a scheduler instance must contain a Profiler instance
     * 
     * \return  The instance of profiler
     */
    Profiler getProfiler();

    /**
     * \brief   Initialize scheduling policy
     *
     * \param   deviceNum       Number of all the devices
     */
    void init(int deviceNum);
}
