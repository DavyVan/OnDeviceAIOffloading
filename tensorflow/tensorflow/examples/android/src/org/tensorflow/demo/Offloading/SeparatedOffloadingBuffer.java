package org.tensorflow.demo.Offloading;

import android.util.Log;

import java.util.ArrayList;

import static org.tensorflow.demo.Offloading.Constant.BUFFER_FULL;
import static org.tensorflow.demo.Offloading.Constant.Config.BUFFER_CLEAN_TYPE;
import static org.tensorflow.demo.Offloading.Constant.Config.BUFFER_SIZE;
import static org.tensorflow.demo.Offloading.Constant.Config.ONLY_REMOTE;
import static org.tensorflow.demo.Offloading.Constant.SUCCESS;
import static org.tensorflow.demo.Offloading.Constant.TASK_NOT_EXIST;

/**
 * Created by fanquan on 17-8-18.
 */

public class SeparatedOffloadingBuffer extends OffloadingBuffer {

    private int deviceNum;
    private ArrayList<Task[]> buffer;
    private int[] nextSlots;
    private int[] heads;
    private int currentBuffer;              /**< i.e. deviceId */
    private Scheduler scheduler;

    private final int indexMultiplier = 10000;
    private int insertCounter;      /** 2018.01.10: Used by quota applying. I should find this bug earlier! */


    public SeparatedOffloadingBuffer(int deviceNum) {
        this.deviceNum = deviceNum;
        buffer = new ArrayList<>();
        reset();
    }

    @Override
    public synchronized int insert(Task task, boolean force) {

        // if buffer is already full
        if (buffer.get(currentBuffer)[nextSlots[currentBuffer]] != null && !force)
            return BUFFER_FULL;

        // insert
        task.bufferIndex = nextSlots[currentBuffer] + currentBuffer * indexMultiplier;      // Use indexMultiplier to indicate buffer
        buffer.get(currentBuffer)[nextSlots[currentBuffer]] = task;
        nextSlots[currentBuffer] = (nextSlots[currentBuffer] + 1) % BUFFER_SIZE[currentBuffer];
        insertCounter++;
        Log.i("INSERT", "New task-" + task.id + " inserted in buffer-" + currentBuffer);

        // maintain currentBuffer
        if (scheduler instanceof LCMScheduler) {
            LCMScheduler.SingleWindow currentBufferWindow = ((LCMScheduler) scheduler).getWindowByDeviceId(currentBuffer);
            // BUG:
            // if current buffer content size can be divided by window size, seek next window's device id
//            if ((nextSlots[currentBuffer] + BUFFER_SIZE[currentBuffer] - heads[currentBuffer]) % BUFFER_SIZE[currentBuffer] % currentBufferWindow.size == 0) {
//                ArrayList<LCMScheduler.SingleWindow> windows = ((LCMScheduler) scheduler).getCurrentWindows();
//
//                int currentBufferWindowIndex = windows.indexOf(currentBufferWindow);
//                currentBuffer = windows.get((currentBufferWindowIndex + 1) % deviceNum).deviceId;
//            }
            // Bug fixed: don't use current buffer content size any more, instead, use a insertCounter
            if (insertCounter >= currentBufferWindow.size) {
                ArrayList<LCMScheduler.SingleWindow> windows = ((LCMScheduler) scheduler).getCurrentWindows();

                int currentBufferWindowIndex = windows.indexOf(currentBufferWindow);
                currentBuffer = windows.get((currentBufferWindowIndex + 1) % deviceNum).deviceId;

                insertCounter = 0;
            }
        }

        return SUCCESS;
    }

    @Override
    public synchronized Task get(int index) {
        return buffer.get(index / indexMultiplier)[index % indexMultiplier];
    }

    @Override
    public synchronized int delete(int index, long taskId) {
        int bufferIndex = index / indexMultiplier;
        int taskIndex = index % indexMultiplier;

        if (taskId != -1) {     // will check taskId
            if (taskId == buffer.get(bufferIndex)[taskIndex].id) {      // verified
                buffer.get(bufferIndex)[taskIndex] = null;
                if (taskIndex == heads[bufferIndex]){       // move head
//                    while (buffer.get(bufferIndex)[heads[bufferIndex]] == null)
//                        heads[bufferIndex] = (heads[bufferIndex] + 1) % BUFFER_SIZE;
                    int _h = heads[bufferIndex];
                    do {
                        heads[bufferIndex] = (heads[bufferIndex] + 1) % BUFFER_SIZE[bufferIndex];
                    } while (buffer.get(bufferIndex)[heads[bufferIndex]] == null && heads[bufferIndex] != _h);
                }
                return SUCCESS;
            }
            else
                return TASK_NOT_EXIST;
        }
        else {      // don't care taskId
            buffer.get(bufferIndex)[taskIndex] = null;
            if (taskIndex == heads[bufferIndex]){       // move head
//                while (buffer.get(bufferIndex)[heads[bufferIndex]] == null)
//                    heads[bufferIndex] = (heads[bufferIndex] + 1) % BUFFER_SIZE;
                int _h = heads[bufferIndex];
                do {
                    heads[bufferIndex] = (heads[bufferIndex] + 1) % BUFFER_SIZE[bufferIndex];
                } while (buffer.get(bufferIndex)[heads[bufferIndex]] == null && heads[bufferIndex] != _h);
            }
            return SUCCESS;
        }
    }

    @Override
    public synchronized int cleanUntouchedTask() {

        int totalCounter = 0;
        if (BUFFER_CLEAN_TYPE.equals("All")) {       // Clean all untouched task
            for (int i = 0; i < deviceNum; i++) {
                int nextSlot = nextSlots[i];
                Task[] _buffer = buffer.get(i);
                int counter = 0;

                int p = (nextSlot - 1 + BUFFER_SIZE[i]) % BUFFER_SIZE[i];
                while (p != nextSlot) {
                    if (_buffer[p] == null) {
                        p = (p - 1 + BUFFER_SIZE[i]) % BUFFER_SIZE[i];
                        continue;
                    }

                    if (_buffer[p].status != 0)
                        break;
                    else {
                        _buffer[p] = null;
                        counter++;
                        p = (p - 1 + BUFFER_SIZE[i]) % BUFFER_SIZE[i];
                    }
                }
                nextSlots[i] = (p + 1) % BUFFER_SIZE[i];
                totalCounter += counter;
            }
        }
        else if (BUFFER_CLEAN_TYPE.equals("Intersection")) {        // Clean 1 every 2
            for (int i = 0; i < deviceNum; i++) {       // For every buffer
                int head = heads[i];
                int nextSlot = head;
                int p = head;
                Task[] _buffer = buffer.get(i);
                boolean flag = true;        // if true then clean current task
                int counter = 0;

                // Find first untouched task
                while (p < head + BUFFER_SIZE[i]) {
                    if (_buffer[p % BUFFER_SIZE[i]] != null && _buffer[p % BUFFER_SIZE[i]].status == 0) {
                        break;
                    }
                    else {
                        p++;
                        nextSlot++;
                    }
                }
                // Now p and nextSlot are point to first untouched task

                // Start clean
                while (p < head + BUFFER_SIZE[i] && _buffer[p % BUFFER_SIZE[i]] != null) {
                    if (flag) {
                        _buffer[p % BUFFER_SIZE[i]] = null;      // clean
                        counter++;
                        p++;
                        flag = !flag;
                    }
                    else {
                        _buffer[nextSlot % BUFFER_SIZE[i]] = _buffer[p % BUFFER_SIZE[i]];     // compress buffer
                        _buffer[p % BUFFER_SIZE[i]] = null;
                        _buffer[nextSlot % BUFFER_SIZE[i]].bufferIndex = i * indexMultiplier + nextSlot % BUFFER_SIZE[i];       // change the Task.bufferIndex to new index
                        p++;
                        nextSlot++;
                        flag = !flag;
                    }
                }       // Clean done
                nextSlots[i] = (nextSlot) % BUFFER_SIZE[i];
                totalCounter += counter;
                Log.i("BUFFER", "Buffer-" + i + " has been cleaned, nextSlot is " + nextSlots[i]);
            }
        }

        System.gc();
        return totalCounter;
    }

    @Override
    public synchronized boolean isHead(int index) {
        int bufferIndex = index / indexMultiplier;
        int taskIndex = index % indexMultiplier;

        return taskIndex == heads[bufferIndex];
    }

    @Override
    public void printBuffer(int start, int end) {
        for (int i = 0; i < deviceNum; i++) {
            _printSingleBuffer(i, start, end);
        }
    }

    private void _printSingleBuffer(int i, int start, int end) {
        if (start == -1 && end == -1) {     // print all
            start = 0;
            end = BUFFER_SIZE[i] - 1;
        }
        else if (end > BUFFER_SIZE[i] - 1)
            end = BUFFER_SIZE[i] - 1;

        String s = "Buffer-" + i + ": ";
        for (int j = start; j <= end; j++) {
            if (buffer.get(i)[j] == null)
                s += "NULL|";
            else {
                Task t = buffer.get(i)[j];
                s += t.id + "," + t.status + "|";
            }
        }
        Log.i("BUFFER", s);
    }

    public synchronized int getHead(int deviceId) {
        return heads[deviceId];
    }

    @Override
    public synchronized void reset() {
        for (int i = 0; i < deviceNum; i++)
            buffer.add(new Task[BUFFER_SIZE[i]]);
        nextSlots = new int[deviceNum];
        heads = new int[deviceNum];

        if (ONLY_REMOTE == 0)
            currentBuffer = 1;
        else
            currentBuffer = 0;

        insertCounter = 0;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public int getIndexMultiplier() {
        return indexMultiplier;
    }
}
