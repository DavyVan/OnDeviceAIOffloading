import numpy as numpy
import tensorflow as tf

# PB_FILENAME = '../stylize_quantized.pb'
PB_FILENAME = '../multibox_model.pb'
# PB_FILENAME = '../tensorflow_inception_graph.pb'
LOG_DIR = 'tfb_log'

with tf.gfile.FastGFile(PB_FILENAME, 'rb') as f:
    graph_def = tf.GraphDef()
    graph_def.ParseFromString(f.read())
    tf.import_graph_def(graph_def, name='')

# print([n.name for n in tf.get_default_graph().as_graph_def().node])
# print([op.name for op in tf.get_default_graph().get_operations()])

sess = tf.Session()
writer = tf.summary.FileWriter(LOG_DIR, sess.graph)
# writer.add_graph(sess.graph)