package org.tensorflow.demo.Offloading;

import android.util.Log;

import static org.tensorflow.demo.Offloading.Constant.BUFFER_FULL;
import static org.tensorflow.demo.Offloading.Constant.Config.BUFFER_SIZE;
import static org.tensorflow.demo.Offloading.Constant.SUCCESS;
import static org.tensorflow.demo.Offloading.Constant.TASK_NOT_EXIST;

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
    private int head;                       /**< Indicate where is the first task slot (in our circle queue) */

    /**
     * /brief   Constructor. Initialize buffer sub-system.
     */
    public OffloadingBuffer() {
        // Allocate memory for buffer
        buffer = new Task[BUFFER_SIZE[0]];
        nextSlot = 0;
        head = 0;
    }

    /**
     * \brief   Insert a new task into buffer.
     *
     *          This method will modify Task::bufferIndex
     *          If buffer is full, the oldest task will be overwritten
     *
     * \param   task        The new task instance
     * \return  Status code
     */
    public int insert(Task task, boolean force) {

        // if buffer is already full
        if (buffer[nextSlot] != null && buffer[nextSlot].status != 3 && !force) {
            return BUFFER_FULL;
        }

        // insert action
        task.bufferIndex = nextSlot;
        buffer[nextSlot] = task;
        nextSlot = (nextSlot + 1) % BUFFER_SIZE[0];
        Log.i("INSERT", "New task-" + task.id + " inserted");
        return SUCCESS;
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

    /**
     * \brief   Delete a task if the task is completed or disregarded
     *
     * \param   index       The index in buffer
     * \param   taskId      The unique ID, for identification
     * \return  error number
     */
    public int delete(int index, long taskId) {
        if (taskId != -1) {     // taskId is used
            if (taskId == buffer[index].id) {
                buffer[index] = null;
                if (index == head)
                    head++;
                return SUCCESS;
            }
            else
                return TASK_NOT_EXIST;
        }
        else {      // don't care taskId
            buffer[index] = null;
            if (index == head)
                head++;
            return SUCCESS;
        }
    }

    /**
     * \brief   Clean untouched(status==0) tasks because of adjustment of sampling rate
     *
     *          This method will sweep from rare to front
     *
     * \return  The number of task cleaned
     */
    public int cleanUntouchedTask() {
        int counter = 0;
        int p = (nextSlot - 1 + BUFFER_SIZE[0]) % BUFFER_SIZE[0];
        while (p != nextSlot) {
            if (buffer[p] == null) {
                p = (p - 1 + BUFFER_SIZE[0]) % BUFFER_SIZE[0];
                continue;
            }

            if (buffer[p].status != 0)
                break;
            else {
                buffer[p] = null;
                counter++;
                p = (p - 1 + BUFFER_SIZE[0]) % BUFFER_SIZE[0];
            }
        }
        nextSlot = (p + 1) % BUFFER_SIZE[0];
        System.gc();
        return counter;
    }

    /**
     * \brief   To know whether the task is at the head of circle queue
     *
     * \note    Last reviewed 2017.8.10 22:17
     *
     * \param   index       As its name
     * \return  the boolean
     */
    public boolean isHead(int index) {
        return head == index;
    }

    public void printBuffer(int start, int end) {
        String s = "";
        for (int i = start; i <= end; i++) {
            if (buffer[i] == null)
                s += "NULL" + "|";
            else
                s += buffer[i].id + "," + buffer[i].status + "|";
        }
        Log.i("BUFFER", s);
    }

    public int getHead() {
        return head;
    }

    /**
     * \brief   Clean all task and reset all pointers
     */
    public void reset() {
        buffer = new Task[BUFFER_SIZE[0]];
        nextSlot = 0;
        head = 0;
    }
}
