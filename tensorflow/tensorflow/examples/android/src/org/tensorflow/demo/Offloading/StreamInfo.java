package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-15.
 */

import android.util.Log;

import java.util.ArrayList;

/**
 * \brief   Structure contains information about data stream
 */
public class StreamInfo {

    public String modelName;        /**< Use model to identify a stream */
    public String appName;          /**< Which app this stream belongs to */

    public ArrayList<Cost> costs;   /**< %Cost for each device, keep the order of device ID */
    public int maxCost;             /**< Max cost for this stream, taking all devices into consideration */
    public int maxCostDevice;       /**< Device ID of max cost */

    /**
     * \brief   Constructor
     *
     * \param   modelName       ::modelName
     * \param   appName         ::appName
     * \param   devices         Device list to init cost list
     */
    public StreamInfo(String modelName, String appName, DeviceAdapter[] devices) {
        this.modelName = modelName;
        this.appName = appName;
        maxCost = 0;
        maxCostDevice = -1;

        costs = new ArrayList<>();
        int deviceNum = devices.length;
        for (int i = 0; i < deviceNum; i++) {
            Cost cost = new Cost(devices[i].isRemote);
            costs.add(cost);
        }
    }

    /**
     * \brief   Simple constructor
     *
     *          This constructor usually called after a task was complete
     *
     * \param   modelName       ::modelName
     * \param   appName         ::appName
     * \param   cost            Use a cost instance to create a StreamInfo instance
     */
    public StreamInfo(String modelName, String appName, Cost cost) {
        this.modelName = modelName;
        this.appName = appName;
        maxCost = 0;
        maxCostDevice = -1;

        costs = new ArrayList<>();
        costs.add(cost);
    }

    public void updateMaxCost() {
        int _maxCost = 0;
        int _maxCostDevice = -1;

        // For all of the costs
        for (int i = 0; i < costs.size(); i++) {
            Cost t = costs.get(i);
            if (t.schedulingCost > _maxCost) {
                _maxCost = t.schedulingCost;
                _maxCostDevice = i;
            }
        }

        maxCost = _maxCost;
        maxCostDevice = _maxCostDevice;
    }

    /**
     * \brief   A sub-structure that contains time cost for each stage
     *
     *          All times are in ms
     */
    public static class Cost {

        public int pre_process;     /**< Time cost of pre-process stage */
        public int uploading;       /**< Time cost of uploading stage */
        public int computing;       /**< Time cost of computing stage */
        public int downloading;     /**< Time cost of downloading result stage
                                         Measured by sender: http://ieeexplore.ieee.org/document/917505*/
        public int post_process;    /**< Time cost of post-process stage */
        public int delta_s;         /**< As the name */

        public boolean isRemote;    /**< Indicate whether the device which bind with current cost instance is remote or local */
        public int schedulingCost;  /**< Accordingly, the cost considered by scheduler */

        /**
         * \brief   Constructor. Assign all the members
         * \note    Maybe useless?
         *
         * \param   pre_process     ::pre_process
         * \param   uploading       ::uploading
         * \param   computing       ::computing
         * \param   downloading     ::downloading
         * \param   post_process    ::post_process
         * \param   isRemote        ::isRemote
         */
        public Cost(int pre_process, int uploading, int computing, int downloading, int post_process, int delta_s, boolean isRemote) {
            this.pre_process = pre_process;
            this.uploading = uploading;
            this.computing = computing;
            this.downloading = downloading;
            this.post_process = post_process;
            this.delta_s = delta_s;
            this.isRemote = isRemote;

            calculateSchedulingCost();
        }

        /**
         * \brief   Simple constructor
         *
         * \param   isRemote        ::isRemote
         */
        public Cost(boolean isRemote) {
            post_process = 0;
            uploading = 0;
            computing = 0;
            downloading = 0;
            post_process = 0;
            delta_s = 0;
            this.isRemote = isRemote;
            schedulingCost = 0;
        }

        /**
         * \brief   Simple constructor without parameter.
         */
        public Cost() {
            post_process = 0;
            uploading = 0;
            computing = 0;
            downloading = 0;
            post_process = 0;
            delta_s = 0;
            isRemote = false;
            schedulingCost = 0;
        }

        /**
         * \brief   As its name
         */
        public void calculateSchedulingCost() {
            if (isRemote) {     // remote
                // Deprecated at 2018.1.5
//                if (pre_process + uploading > post_process + downloading)
//                    schedulingCost = pre_process + uploading;
//                else
//                    schedulingCost = post_process + downloading;
                schedulingCost = Math.max(pre_process + uploading, delta_s);    // Actually, this is consist with the design
            }
            else    // local
                schedulingCost = computing;
        }

        public void printToLog() {
//            Log.i("COST", String.format("pre-process: %d\nuploading: %d\ncomputing: %d\ndownloading: %d\npost-process: %d\ndelta_s in cost: %d\nschedulingCost: %d",
//                    pre_process, uploading, computing, downloading, post_process, delta_s, schedulingCost));
            Log.i("COST", String.format("pre-process: %d\nuploading: %d\ndelta_s: %d", pre_process, uploading, delta_s));
        }
    }


}
