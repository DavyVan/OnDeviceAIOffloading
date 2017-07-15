package org.tensorflow.demo.Offloading;

/**
 * Created by fanquan on 17-7-15.
 */

/**
 * \brief   Structure contains information about data stream
 */
public class StreamInfo {

    public String modelName;        /**< Use model to identify a stream */
    public String modelFileName;    /**< Full path of model file */
    public String appName;          /**< Which app this stream belongs to */

    public Cost[] costs;        /**< %Cost for each device */

    /**
     * \brief   Constructor
     */
    public StreamInfo(String modelName, String modelFileName, String appName) {
        this.modelName = modelName;
        this.modelFileName = modelFileName;
        this.appName = appName;

        //todo:
    }

    /**
     * \brief   A sub-structure that contains time cost for each stage
     *
     *          All times are in ms
     */
    public class Cost {

        public int pre_process;     /**< Time cost of pre-process stage */
        public int uploading;       /**< Time cost of uploading stage */
        public int computing;       /**< Time cost of computing stage */
        public int downloading;     /**< Time cost of downloading result stage */
        public int post_process;    /**< Time cost of post-process stage */

        public boolean isRemote;    /**< Indicate whether the device which bind with current cost instance is remote or local */
        public int schedulingCost;  /**< Accordingly, the cost considered by scheduler */

        /**
         * \brief   Constructor. Assign all the members
         *
         * \param   pre_process     ::pre_process
         * \param   uploading       ::uploading
         * \param   computing       ::computing
         * \param   downloading     ::downloading
         * \param   post_process    ::post_process
         * \param   isRemote        ::isRemote
         */
        public Cost(int pre_process, int uploading, int computing, int downloading, int post_process, boolean isRemote) {
            this.pre_process = pre_process;
            this.uploading = uploading;
            this.computing = computing;
            this.downloading = downloading;
            this.post_process = post_process;
            this.isRemote = isRemote;

            calculateSchedulingCost();
        }

        /**
         * \brief   Default constructor
         */
        public Cost() {
            post_process = 0;
            uploading = 0;
            computing = 0;
            downloading = 0;
            post_process = 0;
            isRemote = false;
            schedulingCost = 0;
        }

        /**
         * \brief   As its name
         */
        private void calculateSchedulingCost() {
            if (isRemote) {     // remote
                if (pre_process + uploading > post_process + downloading)
                    schedulingCost = pre_process + uploading;
                else
                    schedulingCost = post_process + downloading;
            }
            else    // local
                schedulingCost = computing;
        }
    }


}
