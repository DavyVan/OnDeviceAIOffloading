import msgpack
import time

def main():
    bigarray = [x for x in range(720 * 720 * 3)]

    # pack as array
    packer = msgpack.Packer(use_single_float=True)
    startt = time.time()
    print(len(packer.pack(bigarray)))
    endt = time.time()
    print("Pack as array time: %f" % ((endt - startt) * 1000))

    # pack as binary
    packer.reset()
    startt = time.time()
    print(len(msgpack.packb(bigarray)))
    endt = time.time()
    print("Pack as binary time: %f" % ((endt - startt) * 1000))

if __name__ == "__main__":
    main()