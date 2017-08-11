package org.tensorflow.demo.Offloading;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static org.tensorflow.demo.Offloading.Constant.Config.BUFFER_SIZE;
import static org.tensorflow.demo.Offloading.Constant.TASK_NOT_EXIST;
import static org.tensorflow.demo.Offloading.Constant.logIfError;

/**
 * Created by fanquan on 17-7-17.
 */

/**
 * \brief   LCM & Sliding Windows Scheduler
 */
public class LCMScheduler implements Scheduler, DynamicSampling {

    private Profiler profiler;                          /**< A reference to a Profiler instance */
    private OffloadingBuffer offloadingBuffer;          /**< A reference to a OffloadingBuffer instance */
    private ArrayList<SingleWindow> currentWindows;     /**< A array storing windows size*/

    private int sampleInterval;                         /**< in ms */
    private long lastSampleTime;                        /**< As its name */


    /**
     * \brief   Simple constructor only assign all member variables without initializing scheduling policy
     */
    public LCMScheduler(Profiler profiler, OffloadingBuffer offloadingBuffer) {
        this.profiler = profiler;
        this.offloadingBuffer = offloadingBuffer;

        // Allocate member variables
        currentWindows = new ArrayList<>();
    }

    /**
     * Scheduler
     *
     * \note    Last reviewed 2017.8.10 21:24
     */
    @Override
    public void init(int deviceNum) {
        // Scheduling policy - fill the currentWindows & deviceId
        for (int i = 0; i < deviceNum; i++) {
            currentWindows.add(new SingleWindow(1, i));
        }

        // Dynamic sampling - set sample interval as zero (no sampling)
        sampleInterval = 0;
        lastSampleTime = System.currentTimeMillis();
    }

    /**
     * \note    Last reviewed 2017.8.10 22:15
     */
    @Override
    public void calculateQuota(String modelName) {

        int n = currentWindows.size();
        StreamInfo streamInfo = profiler.fetchInfoByModel(modelName);
        // Because we run all scheduling in one master thread so we can do calculation on site
        // prepare all data in a double[]
        double[] temp = new double[n];
        for (int i = 0; i < n; i++)
            temp[i] = streamInfo.costs.get(i).schedulingCost;

        // LCM
        double lcm = Constant.Tools.lcm(temp);

        // K_i
        for (double x : temp)
            x = lcm / x;

        // K'_i
        double min = Constant.Tools.min(temp);
        for (int i = 0; i < temp.length; i++)
//            x = Math.ceil(x / min);
            temp[i] = Math.ceil(temp[i] / min);

        // write results back into SingleWindow in deviceId-increment order (i.e. reset all windows)
        // NOTE: apply() MUST be called right after following assignments
        for (int i = 0; i < n; i++) {
            currentWindows.get(i).size = (int) temp[i];
            currentWindows.get(i).deviceId = i;
        }
        apply();

        calcSamplingRate(modelName);
    }

    /**
     * \note    Last reviewed 2017.8.10 22:15
     */
    @Override
    public void apply() {
        // Sort currentWindows by size in descent order
        Collections.sort(currentWindows, new Comparator<SingleWindow>() {
            @Override
            public int compare(SingleWindow lhs, SingleWindow rhs) {
                return lhs.size - rhs.size;
            }
        });

        // change SingleWindow.position
        // NOTE: the first position keep fixed, since it equals to OffloadingBuffer.head
        int totalSize = currentWindows.get(0).size;
        for (int i = 1; i < currentWindows.size(); i++) {
            if (totalSize + currentWindows.get(i).size > BUFFER_SIZE) {     // If total size > buffer size
                throw new RuntimeException("Total windows size is larger than buffer size. Abort.");
            }
            currentWindows.get(i).position = (currentWindows.get(i - 1).position + currentWindows.get(i - 1).size) % BUFFER_SIZE;
        }
    }

    /**
     * \note    Last reviewed 2017.8.10 21:26
     */
    @Override
    public Task next(int deviceId) {

        SingleWindow window = getWindowByDeviceId(deviceId);
        int start = window.position;
        int size = window.size;
        Task task = null;

        while (size-- > 0) {
            task = offloadingBuffer.get(start);
            if (task.status == 0) {
                task.status = 1;
                return task;
            }
            start++;
            start %= BUFFER_SIZE;
        }
        return null;
    }

    /**
     * \note    Last reviewed 2017.8.11 16:57
     */
    @Override
    public void markAsDone(Task task, int deviceId) {

        // Check bufferIndex & id
        if (offloadingBuffer.get(task.bufferIndex).id != task.id) {     // task over-written
            logIfError(TASK_NOT_EXIST);
            return;
        }

        // Call Profiler to update statistics data
        profiler.updateInfo(new StreamInfo(task.modelName, task.appName, task.cost), deviceId);

        // Deal with task status
        task.status = 3;

        // Check whether the device just processed a task is the slowest device
        // If so, trigger re-schedule
        // If not, nothing to be done
        if (deviceId == currentWindows.get(currentWindows.size() - 1).deviceId)
            calculateQuota(task.modelName);

        // Slide the windows. Firstly, check the task is the first task
        if (offloadingBuffer.isHead(task.bufferIndex)) {
            // Then, check whether the 2st windows is occupied
            SingleWindow secondWindow = currentWindows.get(1);
            if (offloadingBuffer.get(secondWindow.position) == null) {
                // All condition are met, slide window
                int offset = 0;
                int head = task.bufferIndex;
                while (offloadingBuffer.get(head) != null && offloadingBuffer.get(head).status == 3) {
                    offloadingBuffer.delete(head, -1);
                    head++;
                    head %= BUFFER_SIZE;
                    offset++;
                }
                allWindowsMoveForward(offset);
            }
        }
    }

    @Override
    public Profiler getProfiler() {
        return profiler;
    }

    /**
     * DynamicSampling
     *
     * \note    Last reviewed 2017.8.10 22:15
     */
    @Override
    public boolean sample(String modelName) {

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSampleTime >= sampleInterval) {
            lastSampleTime = currentTime;
            return true;
        }
        else
            return false;
    }

    /**
     * \note    Last reviewed 2017.8.10 22:15
     */
    @Override
    public void calcSamplingRate(String modelName) {        // the modelName is useless now
        double v = 0;
        StreamInfo streamInfo = profiler.fetchInfoByModel(modelName);
        int n = streamInfo.costs.size();
        for (int i = 0; i < n; i++) {
            v += 1.0 / (double) streamInfo.costs.get(i).schedulingCost;     // the formula is simplified
        }
        v = 1.0 / v;
        sampleInterval = (int) v;       // ground

        // clean all untouched tasks in buffer
        int c = offloadingBuffer.cleanUntouchedTask();
        Log.i("FQ", String.format("Sampling rate changed, %d tasks were cleaned.", c));
    }

    private SingleWindow getWindowByDeviceId(int deviceId) {
        // iterate the list
        for (int i = 0; i < currentWindows.size(); i++) {
            if (currentWindows.get(i).deviceId != deviceId)
                continue;
            return currentWindows.get(i);
        }
        return null;        // un-touchable statement
    }

    /**
     * \note    Last reviewed 2017.8.11 16:57
     */
    private void allWindowsMoveForward(int offset) {
        for (SingleWindow window : currentWindows) {
            window.position += offset;
            window.position %= BUFFER_SIZE;
        }
    }

    /**
     * \brief   A struct that store attributes of a single window
     */
    private class SingleWindow {
        public int size;        /**< Window size */
        public int position;    /**< Window start point */
        public int deviceId;    /**< As its name */

        /**
         * \brief   Simple constructor which set all to zero
         */
        public SingleWindow() {
            size = 0;
            position = 0;
            deviceId = -1;
        }

        /**
         * \brief   Simple constructor which set size as wish and set position as zero
         */
        public SingleWindow(int size, int deviceId) {
            this.size = size;
            this.deviceId = deviceId;
            position = 0;
        }

        /**
         * \brief   Simple constructor which set all as wish
         */
        public SingleWindow(int size, int position, int deviceId) {
            this.size = size;
            this.position = position;
            this.deviceId = deviceId;
        }
    }
}
