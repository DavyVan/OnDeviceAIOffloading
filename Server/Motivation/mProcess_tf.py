import threading
from multiprocessing import Process, Queue
import time
import datetime
import numpy as np
import os

import zmq
import msgpack
import tensorflow as tf

import sys
sys.path.append("..")
import constant

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'

def create_graph():
    # Creates graph from a pb file
    with tf.gfile.FastGFile("../" + constant.PB_FILE_NAME, 'rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())
        _ = tf.import_graph_def(graph_def, name='')

def run_inference(queue=None):
    # print("Thread %s start" % multiprocessing.)

    # Initialize Tensorflow
    create_graph()
    # gpu_options = tf.GPUOptions(per_process_gpu_memory_fraction=0.333)
    # sess = tf.Session(config=tf.ConfigProto(gpu_options=gpu_options))
    config = tf.ConfigProto()
    config.gpu_options.allow_growth = True
    # config.log_device_placement=True
    config.allow_soft_placement=False
    if constant.ENABLE_THREAD_LIMITATION == True:
        config.intra_op_parallelism_threads=1
        config.inter_op_parallelism_threads=1
    sess = tf.Session(config=config)
    # sess = tf.Session()

    # if not tf.gfile.Exists(image):
    #     tf.logging.fatal('File does not exist %s', image)
    # image_data = tf.gfile.FastGFile(image, 'rb').read()
    image_data = [float(i) for i in range(constant.DESIRED_SIZE * constant.DESIRED_SIZE * 3)]
    image_data = np.array(image_data)

    for i in range(len(image_data)):
        image_data[i] = image_data[i] % 255
    # create_graph()

    input_tensor = sess.graph.get_tensor_by_name(constant.INPUT_NODE)
    style_tensor = sess.graph.get_tensor_by_name(constant.STYLE_NODE)
    output_tensor = sess.graph.get_tensor_by_name(constant.OUTPUT_NODE)
    styleVals = []
    for i in range(constant.NUM_STYLES):
        styleVals.append(float(1)/26)
    # styleVals[1] = float(1.0)
    image_data = image_data.reshape((1, constant.DESIRED_SIZE, constant.DESIRED_SIZE, 3))

    startTime = time.time()
    result = sess.run(output_tensor, {input_tensor: image_data, style_tensor: styleVals})
    endTime = time.time()
    # print((endTime - startTime) * 1000)
    if constant.ENABLE_PLOT == True:
        queue.put((endTime - startTime) * 1000)
    elif constant.ENABLE_SPAN_TIME_PLOT == True:
        queue.put([startTime, endTime])

    result = np.array(result)
    result = result.reshape((-1))

    # post process
    # result_int = [constant.DESIRED_SIZE * constant.DESIRED_SIZE]
    # for i in range(len(result_int)):
    #     result_int[i] = 0xFF000000 | ((int(result[i * 3] * 255)) << 16) | ((int(result[i * 3 + 1] * 255)) << 8) | (int(result[i * 3 + 2] * 255))

def startServer(n, tsleep=0, queue=None):
    processes = []
    for _ in range(n):
        processes.append(Process(target=run_inference, args=(queue,)))
    for i in range(n):
        time.sleep(tsleep)
        processes[i].start()
    for i in range(n):
        processes[i].join()

def main():
    n = input("Please input processes num:")
    n = int(n)
    startServer(n)

if __name__ == '__main__':
    main()