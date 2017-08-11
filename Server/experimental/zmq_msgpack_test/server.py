import time
import zmq
import msgpack

context = zmq.Context()
socket = context.socket(zmq.REP)
socket.bind("tcp://*:5555")

_data = [x for x in range(10000)]
data = "".join(str(i) for i in _data)



while True:
    message = socket.recv()
    print("Receive request: %s" % len(message))

    # unpacking
    unpacker = msgpack.Unpacker()
    unpacker.feed(message)
    taskId = unpacker.unpack()
    print("Task ID: %d" % taskId)
    appName = unpacker.unpack()
    print("App name: %s" % appName)
    modelName = unpacker.unpack()
    print("Model name: %s" % modelName)
    inputNodes = unpacker.unpack()
    print("Input nodes: %s" % inputNodes)
    unpacker.skip() # skip inputValues
    unpacker.skip() # skip dims
    outputNodes = unpacker.unpack()
    odims = unpacker.unpack()

    # packing result
    packer = msgpack.Packer(use_single_float=True, autoreset=False)
    packer.pack(taskId)
    packer.pack(appName)
    packer.pack(modelName)
    packer.pack(outputNodes)
    packer.pack({"output_1":[0.1], "output_2":[1.0,2.0,2.0,4.0]})
    packer.pack(odims)
    packer.pack(456)

    print("Packed len: %s" % len(packer.bytes()))
    socket.send(packer.bytes())