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
            _send = sendQueue.get()
            # socket.send(_send)
            socket.send_multipart([constant.ZMQ_ID.encode(), b'', _send])
            # socket.send_multipart([constant.ZMQ_ID.encode(), _send])
            endTime = time.time()
            downloadingCost = (endTime - startTime) * 1000
            downloadingCostQueue.put(downloadingCost)
            print("Send back a result: %s" % len(_send))

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
        print("Receive request (%s)" % len(request))
        # continue
        if len(request) > 100:
            threading.Thread(target=inference.run_inference, args=(sess, request, downloadingCostQueue, sendQueue)).start()
        else:
            print("Content: %s", str(request))

def main():
    startServer()

if __name__ == '__main__':
    main()
