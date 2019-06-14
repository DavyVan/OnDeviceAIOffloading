import msgpack
from struct import pack, unpack

def unpackReq(request):
    unpacker = msgpack.Unpacker()
    unpacker.feed(request)

    taskId = unpacker.unpack()
    print("Task ID: %d" % taskId)
    appName = unpacker.unpack()
    modelName = unpacker.unpack()
    bufferIndex = unpacker.unpack()
    inputNodes = unpacker.unpack()
    # inputValues = unpacker.unpack()       # if pack as array
    inputValues_t = unpacker.unpack()       # if pack as binary. inputValues_t should be a list<byte[]>
    inputValues = list()
    for values in inputValues_t:
        inputValues.append(unpack(">%df" % (len(values)/4), values))

    dims = unpacker.unpack()
    outputNodes = unpacker.unpack()
    odims = unpacker.unpack()

    return (taskId, appName, modelName, bufferIndex, inputNodes, inputValues, dims, outputNodes, odims)

def packRep(taskId, appName, modelName, bufferIndex, outputNodes, outputs, odims, computingCost, downloadingCost):
    packer = msgpack.Packer(use_single_float=True, autoreset=False)
    # packer.reset()
    packer.pack(taskId)
    packer.pack(appName)
    packer.pack(modelName)
    packer.pack(bufferIndex)

    packer.pack(outputNodes)
    # packer.pack(outputs)        # if pack as array
    packer.pack_map_header(len(outputs))    # if pack as binary
    for key, value in outputs.items():
        packer.pack(key)
        packer.pack(pack(">%df" % (len(value)), *value))

    packer.pack(odims)

    packer.pack(int(computingCost))
    packer.pack(int(downloadingCost))

    return packer.bytes()