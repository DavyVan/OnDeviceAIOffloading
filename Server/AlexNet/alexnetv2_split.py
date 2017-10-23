# Transparented from alexnetv2_all.py

import tensorflow as tf
import numpy as np

import alexnetv2_all
from tensorflow.python.tools import freeze_graph
from tensorflow.python.framework import graph_io

slim = alexnetv2_all.slim
default_image_size = 224

# Take an input tensor with shape=[batch_size, height, width, channels],
# which is [1, 224, 224, 3] in our case
def alexnetv2_front(inputs, scope='alexnet_v2_front'):
    with tf.variable_scope(scope, 'alexnet_v2_front', [inputs]) as sc:
        end_points_collection = sc.name + '_end_points'
        # Collect outputs for conv2d, fully_connected and max_pool2d.
        with slim.arg_scope([slim.conv2d, slim.fully_connected, slim.max_pool2d], outputs_collections=[end_points_collection]):
            conv1 = slim.conv2d(inputs, 64, [11, 11], 4, padding='VALID', scope='conv1')
            pool1 = slim.max_pool2d(conv1, [3, 3], 2, scope='pool1')
            conv2 = slim.conv2d(pool1, 192, [5, 5], scope='conv2')
            pool2 = slim.max_pool2d(conv2, [3, 3], 2, scope='pool2')
            conv3 = slim.conv2d(pool2, 384, [3, 3], scope='conv3')
            conv4 = slim.conv2d(conv3, 384, [3, 3], scope='conv4')
            conv5 = slim.conv2d(conv4, 256, [3, 3], scope='conv5')
            pool5 = slim.max_pool2d(conv5, [3, 3], 2, scope='pool5')

            # Convert end_points_collection into a end_point dict.
            end_points = slim.utils.convert_collection_to_dict(end_points_collection)
            return pool5, end_points

def run_front():
    with slim.arg_scope(alexnetv2_all.alexnet_v2_arg_scope()):
        input_tensor = tf.placeholder(tf.float32, shape=[1, default_image_size, default_image_size, 3])
        outputs, end_points = alexnetv2_front(input_tensor)

        sess = tf.Session()
        sess.run(tf.global_variables_initializer())

        input_data = [float(i%255) for i in range(default_image_size * default_image_size * 3)]
        input_data = np.array(input_data)
        input_data = input_data.reshape((1, default_image_size, default_image_size, 3))

        result = sess.run(outputs, {input_tensor: input_data})
        print(outputs)
        print(result[0][0][0])
        writer = tf.summary.FileWriter('tfb_log', sess.graph)

        freeze_front(sess)

def freeze_front(sess):
    # Freeze
    front_saver = tf.train.Saver()
    checkpoint_path = front_saver.save(sess, './save-front/front')
    graph_io.write_graph(sess.graph, 'save-front', 'input_graph.pb')
    input_graph_path = './save-front/input_graph.pb'
    input_saver_def_path = ""
    input_binary = False
    output_node_names = "alexnet_v2_front/pool5/MaxPool"
    restore_op_name = ""        # Deprecated
    filename_tensor_name = ""   # Deprecated
    output_graph_path = './save-front/output_graph.pb'
    clear_devices = True
    input_meta_graph = './save-front/front.meta'
    freeze_graph.freeze_graph(input_graph_path,
                                input_saver_def_path,
                                input_binary,
                                checkpoint_path,
                                output_node_names,
                                restore_op_name,
                                filename_tensor_name,
                                output_graph_path,
                                clear_devices,
                                "", "")

def test_freeze_front():
    # Creates graph from a pb file
    with tf.gfile.FastGFile('./save-front/output_graph.pb', 'rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())
        tf.import_graph_def(graph_def, name='')
    
    sess = tf.Session()
    input_tensor = sess.graph.get_tensor_by_name("Placeholder:0")
    input_data = [float(i%255) for i in range(default_image_size * default_image_size * 3)]
    input_data = np.array(input_data)
    input_data = input_data.reshape((1, default_image_size, default_image_size, 3))

    output_tensor = sess.graph.get_tensor_by_name('alexnet_v2_front/pool5/MaxPool:0')
    
    result = sess.run(output_tensor, {input_tensor: input_data})
    print(result.shape)

################################################################################

# Take an input tensor with shape=[1, 5, 5, 256] in our case
def alexnetv2_back(inputs, num_classes=1000, is_training=True, dropout_keep_prob=0.5, spatial_squeeze=True, scope='alexnet_v2_back'):
    with tf.variable_scope(scope, 'alexnet_v2_back', [inputs]) as sc:
        end_points_collection = sc.name + '_end_points'
        # Collect outputs for conv2d, fully_connected and max_pool2d.
        with slim.arg_scope([slim.conv2d, slim.fully_connected, slim.max_pool2d], outputs_collections=[end_points_collection]):
            # Use conv2d instead of fully_connected layers.
            with slim.arg_scope([slim.conv2d], weights_initializer=alexnetv2_all.trunc_normal(1.005), biases_initializer=tf.constant_initializer(0.1)):
                fc6 = slim.conv2d(inputs, 4096, [5, 5], padding='VALID',
                                scope='fc6')
                dropout6 = slim.dropout(fc6, dropout_keep_prob, is_training=is_training,
                                scope='dropout6')
                fc7 = slim.conv2d(dropout6, 4096, [1, 1], scope='fc7')
                dropout7 = slim.dropout(fc7, dropout_keep_prob, is_training=is_training,
                                scope='dropout7')
                fc8 = slim.conv2d(dropout7, num_classes, [1, 1],
                                activation_fn=None,
                                normalizer_fn=None,
                                biases_initializer=tf.zeros_initializer(),
                                scope='fc8')
                net = fc8
            
            # Convert end_points_collection into a end_point dict.
            end_points = slim.utils.convert_collection_to_dict(end_points_collection)
            if spatial_squeeze:
                net = tf.squeeze(net, [1, 2], name='fc8/squeezed')
                end_points[sc.name + '/fc8'] = net
            return net, end_points

def run_back():
    with slim.arg_scope(alexnetv2_all.alexnet_v2_arg_scope()):
        input_tensor = tf.placeholder(tf.float32, shape=[1, 5, 5, 256])
        outputs, end_points = alexnetv2_back(input_tensor)

        sess = tf.Session()
        sess.run(tf.global_variables_initializer())

        input_data = [float(i%20) for i in range(5 * 5 * 256)]
        input_data = np.array(input_data)
        input_data = input_data.reshape((1, 5, 5, 256))

        result = sess.run(outputs, {input_tensor: input_data})
        print(outputs)
        writer = tf.summary.FileWriter('tfb_log', sess.graph)

        freeze_back(sess)
        
def freeze_back(sess):
    # Freeze
    front_saver = tf.train.Saver()
    checkpoint_path = front_saver.save(sess, './save-back/back')
    graph_io.write_graph(sess.graph, 'save-back', 'input_graph.pb')
    input_graph_path = './save-back/input_graph.pb'
    input_saver_def_path = ""
    input_binary = False
    output_node_names = "alexnet_v2_back/fc8/squeezed"
    restore_op_name = ""        # Deprecated
    filename_tensor_name = ""   # Deprecated
    output_graph_path = './save-back/output_graph.pb'
    clear_devices = True
    input_meta_graph = './save-back/back.meta'
    freeze_graph.freeze_graph(input_graph_path,
                                input_saver_def_path,
                                input_binary,
                                checkpoint_path,
                                output_node_names,
                                restore_op_name,
                                filename_tensor_name,
                                output_graph_path,
                                clear_devices,
                                "", "")

def test_freeze_back():
    # Creates graph from a pb file
    with tf.gfile.FastGFile('./save-back/output_graph.pb', 'rb') as f:
        graph_def = tf.GraphDef()
        graph_def.ParseFromString(f.read())
        tf.import_graph_def(graph_def, name='')
    
    sess = tf.Session()
    input_tensor = sess.graph.get_tensor_by_name("Placeholder:0")
    input_data = [float(i%255) for i in range(5 * 5 * 256)]
    input_data = np.array(input_data)
    input_data = input_data.reshape((1, 5, 5, 256))

    output_tensor = sess.graph.get_tensor_by_name('alexnet_v2_back/fc8/squeezed:0')
    
    result = sess.run(output_tensor, {input_tensor: input_data})
    print(result.shape)

if __name__ == '__main__':
    # run_front()
    # run_back()
    # test_freeze_front()
    test_freeze_back()