import socket

import tcp_define

def start_client():
    """
    start_client() -> socket

    Start the TCP client.
    """
    sk = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sk.connect((tcp_define.SERVER_ADDR, tcp_define.SERVER_PORT))
    # reveive welcome message
    print(sk.recv(1024))
    return sk

def close_client(sk):
    """
    close_client() -> void

    Send signal message to server to indicate the closing of client, then terminate TCP connection.
    """
    sk.send('exit'.encode())
    sk.close()

if __name__ == '__main__':
    sk = start_client()
    for data in ['zhang', 'liu', 'wang']:
        sk.send(data.encode())
        print(sk.recv(1024).decode())
    close_client(sk)
    