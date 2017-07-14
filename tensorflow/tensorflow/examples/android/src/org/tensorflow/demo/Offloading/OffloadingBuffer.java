package org.tensorflow.demo.Offloading;

import java.util.ArrayList;

import static org.tensorflow.demo.Offloading.Constant.BUFFER_SIZE;

/**
 * Created by fanquan on 17-7-14.
 */

/**
 * /brief   To maintain the buffer and the status of tasks inside.
 *
 *          Use Task[] as basic data structure, with fixed size, which is created during initialization
 */
public class OffloadingBuffer {

    private Task[] buffer;                  /**< Buffer itself */
    private TaskManager taskManager;        /**< A reference to a TaskManager who will execute each task */

    private int nextSlot;                   /**< Indicate where the next new task should be located */

    /**
     * /brief   Initialize buffer sub-system.
     */
    public void init() {
        // Allocate memory for buffer
        buffer = new Task[BUFFER_SIZE];
        nextSlot = 0;
    }

    /**
     * \brief   Mark a task in buffer as completed and move window forward if need.
     *
     * \param   index       Indicate which task is involved.
     */
    public void markAsDone(int index) {
        buffer[index].isDone = true;

        //todo: how can we move windows?
    }

    /**
     * \brief   Insert a new task into buffer and dispatch it to a device if it can.
     *
     *          This method will modify Task::bufferIndex
     *          If buffer is full, the oldest task will be overwritten
     *
     * \param   task        The new task instance
     * \return  Insertion succeed or failed
     */
    public boolean insert(Task task) {
        buffer[nextSlot] = task;
        buffer[nextSlot].bufferIndex = nextSlot;
        runTask(nextSlot);
        nextSlot = (nextSlot + 1) % BUFFER_SIZE;

        return true;
    }

    /**
     * \brief   Decide which device should process the task and run.
     *
     * \param   index       Index of task in buffer
     */
    private void runTask(int index) {
        //todo: windows design; private?
    }

    /**
     * \brief   Apply the windows computed by Scheduler
     *
     * \param   windows     The list of windows' size
     * \sa      Called byScheduler::apply
     */
    public void changeWindows(ArrayList<Integer> windows) {
        //todo: we may implement the windows' logic entirely in Scheduler
    }
}
