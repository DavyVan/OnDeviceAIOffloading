from __future__ import absolute_import, unicode_literals
import tensorflow as tf
import time
import os.path
import sys
import numpy as np
import threading
import queue

import constant
import packing

def create_graph():
    # Creates graph from a pb file
    with tf.gfile.FastGFile(constant.PB_FILE_NAME, 'rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())
        _ = tf.import_graph_def(graph_def, name='')

def run_inference(sess, request, dlcqueue, sendQueue):
    print("@@@FQ@@@ - Thread %s start" % threading.currentThread().getName())

    # unpack
    (_id,           # long
    appName,        # Strin
    modelName,      # String
    bufferIndex,    # int
    inputNodes,     # String[]
    inputValues,    # float[][]
    dims,           # long[][]
    outputNodes,    # String[]
    odims           # Map<String, long[]>
    ) = packing.unpackReq(request)

    # Initialize tf.Session if needed
    if sess == None:
        create_graph()
        config = tf.ConfigProto()
        config.gpu_options.allow_growth = True
        sess = tf.Session(config=config)

    # prepare to feed
    inputs = {}
    for i in range(len(inputNodes)):
        tensor = sess.graph.get_tensor_by_name(inputNodes[i].decode('utf-8'))
        inputs[tensor] = np.array(inputValues[i]).reshape(dims[i])

    outputTensors = {}      
    for outputNode in outputNodes:
        outputTensors[outputNode] = sess.graph.get_tensor_by_name(outputNode.decode('utf-8'))

    # run
    startTime = time.time()
    result = sess.run(outputTensors, inputs)        # dict<string, numpy.array>
    endTime = time.time()
    
    computingCost = (endTime - startTime) * 1000        # milisecond

    # pack result - convert numpy.array to python list
    for key in result:
        # print(result[key])
        result[key] = result[key].reshape([-1]).tolist()
    
    # pack result - obtain downloadingCost
    downloadingCost = 0        # N/A
    if not dlcqueue.empty():
        downloadingCost = dlcqueue.get()
    print("Add downloadingCost in result: %f" % downloadingCost)
    
    # pack result
    reply = packing.packRep(_id, appName, modelName, bufferIndex, outputNodes, result, odims, computingCost, downloadingCost)
    print("Task---%d completed!" % _id)
    sendQueue.put(reply)
