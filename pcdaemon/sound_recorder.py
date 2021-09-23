import pyaudio
import wave
import socket
import time
import threading
import queue

# len(data) = CHUNK * CHANNELS * # of FORMAT bytes
CHUNK = 1024
FORMAT = pyaudio.paInt16
CHANNELS = 2
RATE = 44100
RECORD_SECONDS = 10
FILENAME = 'output.wav'
SOCK_DATA_LEN = CHUNK * CHANNELS * 2


def record_and_write():
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT,
                    channels=CHANNELS,
                    rate=RATE,
                    input=True,
                    frames_per_buffer=CHUNK)

    print("* recording")

    frames = []

    for i in range(0, int(RATE / CHUNK * RECORD_SECONDS)):
        data = stream.read(CHUNK)
        frames.append(data)

    print("* done recording")

    stream.stop_stream()
    stream.close()
    p.terminate()
    wf = wave.open(FILENAME, 'wb')
    wf.setnchannels(CHANNELS)
    wf.setsampwidth(p.get_sample_size(FORMAT))
    wf.setframerate(RATE)
    wf.writeframes(b''.join(frames))
    wf.close()


def play():
    wf = wave.open(FILENAME, 'rb')
    print(wf.getframerate())
    p = pyaudio.PyAudio()
    stream = p.open(format=p.get_format_from_width(wf.getsampwidth()),
                    channels=wf.getnchannels(),
                    rate=wf.getframerate(),
                    output=True)

    data = wf.readframes(CHUNK)

    while data != '':
        stream.write(data)
        time.sleep(0.01)
        data = wf.readframes(CHUNK)

    stream.stop_stream()
    stream.close()

    p.terminate()


CHUNK_SIZE = 10


def _start_capturing(queue: queue.Queue):
    p = pyaudio.PyAudio()
    stream = p.open(format=FORMAT,
                    channels=CHANNELS,
                    rate=RATE,
                    input=True,
                    frames_per_buffer=CHUNK)

    print("* recording")
    frames = []

    for i in range(0, int(RATE / CHUNK * RECORD_SECONDS)):
        data = stream.read(CHUNK)
        queue.put(data)
        frames.append(data)

    stream.stop_stream()
    stream.close()
    p.terminate()

    wf = wave.open(FILENAME, 'wb')
    wf.setnchannels(CHANNELS)
    wf.setsampwidth(p.get_sample_size(FORMAT))
    wf.setframerate(RATE)
    wf.writeframes(b''.join(frames))
    wf.close()

    print('done')
    queue.put('\0')


def capture_and_stream():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind(('127.0.0.1', 9889))
    s.listen(1)
    cli, addr = s.accept()

    buffer = queue.Queue()

    threading.Thread(target=_start_capturing,
                     args=(buffer,), daemon=True).start()

    while True:
        data = buffer.get(block=True)
        if data == '\0':
            break
        cli.send(data)

    s.close()


if __name__ == '__main__':
    capture_and_stream()
    # _start_capturing(queue.Queue())
    # play()
    # record_and_write()
