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
     * \brief   Allow to query for a specific Task instance given the index
     * 
     * \param   The index
     * \return  The Task instance
     */
    public Task get(int index) {
        return buffer[index];
    }
}
