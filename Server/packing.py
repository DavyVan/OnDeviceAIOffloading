import msgpack

def unpackReq(request):
    unpacker = msgpack.Unpacker()
    unpacker.feed(request)

    taskId = unpacker.unpack()
    print("Task ID: %d" % taskId)
    appName = unpacker.unpack()
    modelName = unpacker.unpack()
    bufferIndex = unpacker.unpack()
    inputNodes = unpacker.unpack()
    inputValues = unpacker.unpack()
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
    # packer.pack_map_header(len(outputNodes))
    # for key in outputs:
    #     packer.pack(key)
    #     print(len(outputs[key]))
    #     packer.pack(outputs[key])
    packer.pack(outputs)
    packer.pack(odims)

    packer.pack(int(computingCost))
    packer.pack(int(downloadingCost))

    return packer.bytes()