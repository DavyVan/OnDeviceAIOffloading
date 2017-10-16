import numpy as np
import matplotlib.pyplot as plt
from multiprocessing import Queue
import time

import sys
import os
sys.path.append("..")
import constant
import mProcess_tf
import DataFileManager

concurrency_num = [1, 2, 3, 4, 8, 12, 16]
sleep_times_s = [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6]

DATA_ROOT_DIR = 'data/'
DATA_DIR_TOTAL_TIME = 'totalcomptime'
DATA_DIR_PER_TIME = 'percomptime'
DATA_DIR_LIMITATION = '_limit'
DATA_FILE_EXT = '.npy'

markers = ['s', 'o', 'X', 'P', 'd', '*', '^']

def compute_avg_from_queue(queue):
    result = 0.0
    c = queue.qsize()
    for i in range(c):
        result += queue.get()
    return result / c

def run_multi_process():        # for completion time (per & total)
    constant.ENABLE_PLOT = True
    print("Testing multiprocessing version...")

    _queue = Queue()
    results = []

    # open output file
    if not os.path.isdir(get_output_dir()):
        os.makedirs(get_output_dir())
        print("Output directory doesn't exist. Created.")
    result_fd = open(get_output_full_path(), 'wb')

    # output metadata to file
    np.save(result_fd, concurrency_num)
    np.save(result_fd, sleep_times_s)

    # Run and output data
    for sleep_time in sleep_times_s:
        print("Testing sleep time = %s" % sleep_time)
        for i in concurrency_num:
            startTime = time.time()
            mProcess_tf.startServer(i, tsleep=sleep_time, queue=_queue)
            endTime = time.time()
            t = compute_avg_from_queue(_queue)
            if constant.ENABLE_TOTAL_TIME_PLOT:
                results.append((endTime - startTime) * 1000)        # Total completion time
                print("%s-concurrency test has finished in %s" % (i, (endTime - startTime)*1000))
            else:
                results.append(t)       # Per-process avg completion time
                print("%s-concurrency test has finished in %s" % (i, t))

        # results[0] = 1421
        # results[1] = 1446
        # results[2] = 1586
        # results[3] = 1535
        np.save(result_fd, np.array(results))
        results.clear()
    result_fd.close()

    # Draw
    draw_plot_from_file()

def get_output_filename():
    return time.strftime('%Y_%m_%d_%H_%M_%S', time.localtime(time.time())) + DATA_FILE_EXT

def get_output_dir():
    return DATA_ROOT_DIR + \
           (DATA_DIR_TOTAL_TIME if constant.ENABLE_TOTAL_TIME_PLOT else DATA_DIR_PER_TIME) + \
           (DATA_DIR_LIMITATION if constant.ENABLE_THREAD_LIMITATION else '') + '/'

def get_output_full_path():
    return get_output_dir() + get_output_filename()

def draw_plot_from_file(filename=None):
    print("Start to draw from file...")

    # fetch the latest data file
    if filename is None:
        filename = DataFileManager.fetch_latest_file(get_output_dir(), extname=DATA_FILE_EXT)
    print('Selected data file: ' + filename)
    data_fd = open(get_output_dir() + filename, 'rb')

    # read from file
    _concurrency_num = np.load(data_fd)
    print("Concurrency_num in this file is: " + str(_concurrency_num))
    _sleep_times_s = np.load(data_fd)
    print("Sleep_times_s in this file is: " + str(_sleep_times_s))
    
    # draw
    fig, ax = plt.subplots()
    # fig.set_size_inches(4,4)
    for i in range(len(_sleep_times_s)):
        _result = np.load(data_fd)
        print("Current data row is: " + str(_result))
        line1, = ax.plot(_concurrency_num, _result, label='sleep_time='+str(_sleep_times_s[i])+'s', marker=markers[i])
    ax.set_xlabel('Number of Processes', fontsize='small')
    ax.set_ylabel('Total Completion Time(ms)' if constant.ENABLE_TOTAL_TIME_PLOT else 'Per-process Completion Time(ms)', fontsize='small')
    ax.tick_params(labelsize='small')
    ax.legend(loc='upper left', fontsize='small')
    plt.show()
    print("Draw from file...done")
    data_fd.close()

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
    run_multi_process()
    # draw_plot_from_file()
    # run_multi_process_for_per_span_time()