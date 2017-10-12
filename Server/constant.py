# Network
SERVER_ADDR = '192.168.0.240'
SERVER_PORT = '2333'
ZMQ_ID = "FQ"

# TensorFlow 
PB_FILE_DIR = './'
DEMO_INDICATOR = 3  # 1:style   2:Classify  3:Detect
PB_FILE_NAME = ''
if DEMO_INDICATOR == 1:
    PB_FILE_NAME = 'stylize_quantized.pb'
elif DEMO_INDICATOR == 2:
    PB_FILE_NAME = 'tensorflow_inception_graph.pb'
elif DEMO_INDICATOR == 3:
    PB_FILE_NAME = 'multibox_model.pb'

# STYLE_PB_FILE_NAME = 'stylize_quantized.pb'
# INPUT_NODE = 'input:0'
# STYLE_NODE = 'style_num:0'
# OUTPUT_NODE = 'transformer/expand/conv3/conv/Sigmoid:0'
# NUM_STYLES = 26
# DESIRED_SIZE = 256

# Runtime Configuration
ENABLE_SINGLE_SESSION = True        # Use a single Sesssion for all threads, or one Session per thread
ENABLE_PLOT = False
ENABLE_SPAN_TIME_PLOT = False
