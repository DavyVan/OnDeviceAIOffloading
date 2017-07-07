import socket
import threading
import time

import tcp_define

def tcplink(sock, addr):
    print('Processing new connection from %s:%s' % addr)
    sock.send('Welcome!'.encode())
    while True:
        data = sock.recv(1024)
        time.sleep(1)
        if data.decode() == 'exit' or not data:
            break
        sock.send(('Hello, %s!' % data.decode()).encode())
    sock.close()
    print('Connection from %s:%s closed.' % addr)


def start_server():
    """start_server() -> void

    Start the server and listen on a specific socket
    """
    # Create socket
    sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sk.bind((tcp_define.SERVER_ADDR, tcp_define.SERVER_PORT))
    sk.listen(5)
    print('Listening on %s:%d' % (tcp_define.SERVER_ADDR, tcp_define.SERVER_PORT))

    # listen
    sock, addr = sk.accept()
    t = threading.Thread(target=tcplink, args=(sock, addr))
    t.start()

if __name__ == '__main__':
    start_server()
