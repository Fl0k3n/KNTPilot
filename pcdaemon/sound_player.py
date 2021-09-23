import pyaudio
import wave
import socket
import threading
import queue

CHUNK = 1024
FORMAT = pyaudio.paInt16
CHANNELS = 2
RATE = 44100
RECORD_SECONDS = 10
FILENAME = 'output_player.wav'
SOCK_DATA_LEN = CHUNK * CHANNELS * 2

frame_added_lock = threading.Lock()
frame_added_cond = threading.Condition(frame_added_lock)
finished = False


def _start_playing(buffer: queue.Queue):
    global finished
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT,
                    channels=CHANNELS,
                    rate=RATE,
                    output=True,
                    output_device_index=3)

    REQ_BUFF_SIZE = 15
    finished = False
    while not finished:
        with frame_added_lock:
            while buffer.qsize() < REQ_BUFF_SIZE and not finished:
                frame_added_cond.wait()

        while True:
            try:
                data = buffer.get(block=False)
                stream.write(data)
            except queue.Empty:
                break

    stream.close()
    p.terminate()


def recv_and_play():
    global finished
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect(('127.0.0.1', 9889))
    print('connected')

    buffer = queue.Queue()
    threading.Thread(target=_start_playing, args=(
        buffer,), daemon=True).start()
    dced = False

    while not dced:
        chunk_data = []
        size = 0
        while size < SOCK_DATA_LEN:
            rcvd = s.recv(SOCK_DATA_LEN)
            size += len(rcvd)
            if len(rcvd) == 0:
                print("lost conn")
                dced = True
                finished = True
                break
            chunk_data.append(rcvd)

        with frame_added_lock:
            if not finished:
                buffer.put(b''.join(chunk_data))
            frame_added_cond.notify_all()

    s.close()


if __name__ == '__main__':
    recv_and_play()
