# Network
SERVER_ADDR = '192.168.0.126'
SERVER_PORT = '2333'
ZMQ_ID = "FQ"

# tensorflow
PB_FILE_DIR = './'
PB_FILE_NAME = 'stylize_quantized.pb'
INPUT_NODE = 'input:0'
STYLE_NODE = 'style_num:0'
OUTPUT_NODE = 'transformer/expand/conv3/conv/Sigmoid:0'
NUM_STYLES = 26
DESIRED_SIZE = 720
IMAGE_FILE_DIR = './'
IMAGE_FILE_NAME = 'test.png18210664811'

# Runtime Configuration
ENABLE_SINGLE_SESSION = True        # Use a single Sesssion for all threads, or one Session per thread
