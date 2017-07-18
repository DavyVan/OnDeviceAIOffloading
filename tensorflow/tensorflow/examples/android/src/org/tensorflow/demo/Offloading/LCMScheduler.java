package org.tensorflow.demo.Offloading;

import android.util.Log;

import java.util.ArrayList;

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
    private ArrayList<Integer> deviceId;                /**< According the windows size, the device ID */

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
        deviceId = new ArrayList<>();
    }

    /**
     * Scheduler
     */
    @Override
    public void init(int deviceNum) {
        // TODO: 17-7-17  dynamic sampling
        // Scheduling policy - fill the currentWindows & deviceId
        for (int i = 0; i < deviceNum; i++) {
            currentWindows.add(new SingleWindow(1));
            deviceId.add(i+1);
        }

        // Dynamic sampling - set sample interval as zero (no sampling)
        sampleInterval = 0;
        lastSampleTime = System.currentTimeMillis();
    }

    @Override
    public void calculateQuota(String modelName) {
        // TODO: 17-7-17
        apply();
        calcSamplingRate(modelName);
    }

    @Override
    public void apply() {
        // TODO: 17-7-17  change SingleWindow.position
    }

    @Override
    public Task next(int deviceId) {
        // TODO: 17-7-17 need an algo to deal with SingleWindow.position
        return null;
    }

    @Override
    public void markAsDone(int index) {
        // TODO: 17-7-17 need to check index and unique id, slide the windows if needed
    }

    @Override
    public Profiler getProfiler() {
        return profiler;
    }

    /**
     * DynamicSampling
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

    /**
     * \brief   A struct that store attributes of a single window
     */
    private class SingleWindow {
        public int size;        /**< Window size */
        public int position;    /**< Window start point */

        /**
         * \brief   Simple constructor which set all to zero
         */
        public SingleWindow() {
            size = 0;
            position = 0;
        }

        /**
         * \brief   Simple constructor which set size as wish and set position as zero
         */
        public SingleWindow(int size) {
            this.size = size;
            position = 0;
        }

        /**
         * \brief   Simple constructor which set all as wish
         */
        public SingleWindow(int size, int position) {
            this.size = size;
            this.position = position;
        }
    }
}
