from __future__ import absolute_import, unicode_literals
import tensorflow as tf
import datetime
import os.path
import sys
import numpy as np


PB_FILE_DIR = './'
PB_FILE_NAME = 'stylize_quantized.pb'
INPUT_NODE = 'input:0'
STYLE_NODE = 'style_num:0'
OUTPUT_NODE = 'transformer/expand/conv3/conv/Sigmoid:0'
NUM_STYLES = 26
DESIRED_SIZE = 720
IMAGE_FILE_DIR = './'
IMAGE_FILE_NAME = 'test.png'


def create_graph():
    # Creates graph from a pb file
    with tf.gfile.FastGFile(PB_FILE_NAME, 'rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())
        _ = tf.import_graph_def(graph_def, name='')


def run_inference(image):
    """
    
    :param image: Image file name. 
    :return: Nothing
    """
    # if not tf.gfile.Exists(image):
    #     tf.logging.fatal('File does not exist %s', image)
    # image_data = tf.gfile.FastGFile(image, 'rb').read()
    image_data = [float(i) for i in range(DESIRED_SIZE * DESIRED_SIZE * 3)]
    image_data = np.array(image_data)

    for i in range(len(image_data)):
        image_data[i] = image_data[i] % 255
    create_graph()

    with tf.Session() as sess:
        input_tensor = sess.graph.get_tensor_by_name(INPUT_NODE)
        style_tensor = sess.graph.get_tensor_by_name(STYLE_NODE)
        output_tensor = sess.graph.get_tensor_by_name(OUTPUT_NODE)
        styleVals = []
        for i in range(NUM_STYLES):
            styleVals.append(float(1)/26)
        # styleVals[1] = float(1.0)
        image_data = image_data.reshape((1, DESIRED_SIZE, DESIRED_SIZE, 3))

        startTime = datetime.datetime.now()
        for i in range(10):
            print(i)
            result = sess.run(output_tensor, {input_tensor: image_data, style_tensor: styleVals})
        endTime = datetime.datetime.now()
        print((endTime - startTime)/10)

        result = np.array(result)
        result = result.reshape((-1))

        # post process
        result_int = [DESIRED_SIZE * DESIRED_SIZE]
        for i in range(len(result_int)):
            result_int[i] = 0xFF000000 | ((int(result[i * 3] * 255)) << 16) | ((int(result[i * 3 + 1] * 255)) << 8) | (int(result[i * 3 + 2] * 255))


def main(_):
    image = os.path.join(IMAGE_FILE_DIR, IMAGE_FILE_NAME)
    run_inference(image)

if __name__ == '__main__':
    tf.app.run(main=main, argv=[sys.argv[0]])
