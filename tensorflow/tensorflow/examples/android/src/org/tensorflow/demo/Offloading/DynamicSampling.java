package org.tensorflow.demo.Offloading;

/**
 * \brief   If a scheduler support dynamic sampling, it should implement this interface
 */
public interface DynamicSampling {

    /**
     * \brief   Make the descion that whether take current data frame or drop it
     * 
     * \param   modelName       To identify the data stream
     * \return  Take it or drop it
     */
    boolean sample(String modelName);
}