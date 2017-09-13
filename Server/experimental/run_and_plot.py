import numpy as np
import matplotlib.pyplot as plt
from multiprocessing import Queue
import time
import numpy as np

import sys
sys.path.append("..")
import constant
import mProcess_tf

concurrency_num = [i+1 for i in range(16)]
sleep_times_s = [0, 0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.5, 0.6]

def compute_avg_from_queue(queue):
    result = 0.0;
    c = queue.qsize()
    for i in range(c):
        result += queue.get()
    return result / c

def run_multi_process():        # for completion time (per & total)
    constant.ENABLE_PLOT = True
    print("Testing multiprocessing version...")

    _queue = Queue()
    results = []

    fig, ax = plt.subplots()
    for sleep_time in sleep_times_s:
        print("Testing sleep time = %s" % sleep_time)
        for i in concurrency_num:
            startTime = time.time()
            mProcess_tf.startServer(i, tsleep=sleep_time, queue=_queue)
            endTime = time.time()
            t = compute_avg_from_queue(_queue)
            results.append(t)       # Per-process avg completion time
            # results.append((endTime - startTime) * 1000)        # Total completion time
            print("%s-concurrency test has finished in %s" % (i, t))

        # results[0] = 1421
        # results[1] = 1446
        # results[2] = 1586
        # results[3] = 1535

        line1, = ax.plot(concurrency_num, results, label='sleep_time='+str(sleep_time)+"s")
        results.clear()
    ax.set_xlabel('Number of Processes')
    ax.set_ylabel('Per-process Completion Time(ms)')
    # ax.set_ylabel('Total Completion Time(ms)')
    ax.legend(loc='upper left')
    plt.show()

# with 1-thread config when create tf.Session
# 1-concurrency test has finished in 1421.9794273376465
# 2-concurrency test has finished in 1468.7604904174805
# 3-concurrency test has finished in 1451.9704182942708
# 4-concurrency test has finished in 1719.0876603126526
# 5-concurrency test has finished in 1756.7167282104492
# 6-concurrency test has finished in 2022.8732029596965
# 7-concurrency test has finished in 2444.838830402919
# 8-concurrency test has finished in 2754.721760749817
# 9-concurrency test has finished in 3028.0006726582847
# 10-concurrency test has finished in 3558.6557388305664
# 11-concurrency test has finished in 3667.0236587524414
# 12-concurrency test has finished in 3861.1984252929688
# 13-concurrency test has finished in 4268.079097454364
# 14-concurrency test has finished in 4391.522271292551
# 15-concurrency test has finished in 4485.021066665649
# 16-concurrency test has finished in 4915.256172418594

# without thread config when create tf.Session
# 1-concurrency test has finished in 535.3753566741943
# 2-concurrency test has finished in 873.0850219726562
# 3-concurrency test has finished in 1179.7569592793782
# 4-concurrency test has finished in 1516.6699886322021
# 5-concurrency test has finished in 1788.4234428405762
# 6-concurrency test has finished in 2157.8056812286377
# 7-concurrency test has finished in 2613.2565225873673
# 8-concurrency test has finished in 2810.4883432388306
# 9-concurrency test has finished in 3394.5131566789414
# 10-concurrency test has finished in 3315.2384996414185
# 11-concurrency test has finished in 3575.8193189447575
# 12-concurrency test has finished in 3638.9963030815125
# 13-concurrency test has finished in 3729.159318483793
# 14-concurrency test has finished in 3865.8730813435145
# 15-concurrency test has finished in 4156.186723709106
# 16-concurrency test has finished in 4402.360737323761

def run_multi_process_for_per_span_time():
    constant.ENABLE_SPAN_TIME_PLOT = True
    print("Testing multiprocessing version for per process span time...")

    _queue = Queue()
    fig, ax = plt.subplots()
    baseTime = time.time()
    results = []

    concurrency = int(input("Input concurrency:"))
    sleep_time = float(input("Input sleep time in s:"))
    mProcess_tf.startServer(concurrency, tsleep=sleep_time, queue=_queue)

    c = _queue.qsize()
    for i in range(c):
        # line, = ax.plot((np.array(_queue.get())-baseTime)*1000, [i+1, i+1], label=str(i+1))
        results.append(_queue.get())
    results = np.array(results)
    results = (results - baseTime) * 1000
    t = results.min()
    results -= t
    for i in range(c):
        line, = ax.plot(results[i], [i+1, i+1], label=str(i+1))

    ax.set_xlabel('Time(ms)')
    ax.set_ylabel('Per-process Time Span')
    # ax.legend(loc='upper left')
    plt.show()

if __name__ == "__main__":
    # run_multi_process()
    run_multi_process_for_per_span_time()