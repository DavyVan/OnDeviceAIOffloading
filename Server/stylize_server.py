import threading
import queue
import time

import zmq
import tensorflow as tf

import constant
import inference

sendQueue = queue.Queue()
downloadingCostQueue = queue.Queue()

def sendFromQueue(socket):
    print("@@@FQ@@@ - Sending thread started.")
    while True:
        if not sendQueue.empty():
            startTime = time.time()
            socket.send(sendQueue.get())
            endTime = time.time()
            downloadingCost = (endTime - startTime) * 1000
            downloadingCostQueue.put(downloadingCost)

def startServer():

    sess = None

    # Initialize Tensorflow
    if constant.ENABLE_SINGLE_SESSION:
        inference.create_graph()
        config = tf.ConfigProto()
        config.gpu_options.allow_growth = True
        sess = tf.Session(config=config)

    # Initialize ZMQ
    context = zmq.Context()
    socket = context.socket(zmq.ROUTER)
    socket.setsockopt(zmq.SNDTIMEO, -1)
    socket.bind("tcp://" + constant.SERVER_ADDR + ":" + constant.SERVER_PORT)

    # Start send thread
    threading.Thread(target=sendFromQueue, args=(socket,)).start()

    # Start receive request
    while True:
        request = socket.recv()
        print("Receive request")

        threading.Thread(target=inference.run_inference, args=(sess, request, downloadingCostQueue, sendQueue)).start()

def main():
    startServer()

if __name__ == '__main__':
    main()
