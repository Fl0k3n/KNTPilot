# KNTPilot

### Secure and easy to use* Remote Desktop for controlling PC from Android Phone
### Demo: <a href="https://www.youtube.com/watch?v=lhgo4ZmHUS4">YouTube demo</a>

<br/>

---

## Overview

<br/>

Client - server application containing:

- Desktop Server written in Python responsible for:
    - handling the connection with a single remote user
    - authenticating remote user with password configured by the desktop owner
    - establishing encrypted channels for streaming media and receiving input commands
    - controlling keyboard and mouse
    - streaming of desktop screen(s) and audio in real time
    
<br/>

- Android Application written in Java responsible for:
    - connecting to the desktop server via secure channel using password authentication
    - displaying the desktop screen
    - playing the desktop audio
    - translating finger taps into mouse clicks
    - translating user input on a virtual phone keyboard to keystrokes on a desktop keyboard
    - providing an interface via natural gestures to enlarge the required part of the desktop screen so that the user can "click" in precise position with their finger

<br/>

---

## Architecture

<br/>

### Communication

---

Written from scratch on the socket API. The client connects and authenticates himself using a TCP connection, then opens a UDP port to which the server streams media. Both channels are secure (or should be; see Security section)

- Commands like mouse clicks, keyboard input, QoS of media streaming are sent via TCP json-based protocol
- Audio and video are sent via RTP'alike protocol built on top of UDP, application-layer fragmentation is supported

Results (in LAN conditions!):
- acceptable latency, with default buffering audio is transmitted flawlessly, video is transmitted reasonably well
- video and audio is not perfectly synchronized, with default configuration 1 stream may be even 100ms in front of another
- Quality of Service doesn't work very well for now, which results in video frame loss when greater screen resolution (>HD) is streamed


Further developement:
- QoS that allows auto stream quality change (better compression, etc...) under poor network conditions or larger resolutions
- adaptive buffering so latency can be minimized further
- UDP hole punching so that app can be used on WAN


---

TCP packet:
```
base64 encoded JSON:
{
    code: int // see pcdaemon/msg_codes.py for details,
    body: Any // Json object containing data specific for given code
}
```

UDP packet:
```
    Media packet:
    |--------------------------------------------|
    |  code(8)    |         reserved(24)         |
    |--------------------------------------------|
    |           sequence number(32)              |
    |--------------------------------------------|
    |                 size(32)                   |
    |--------------------------------------------|
    |                 offset(32)                 |
    |--------------------------------------------|
    |                   data                     |
    |--------------------------------------------|

- code - see pcdaemon/media_msg_codes.py for details
- reserved - pad bytes, will probably contain timestamps in future
- sequence number - 32b uint used for ordering UDP packets, separate numbers for every code
- size - of entire media frame(eg. 1 screenshot), used to allocate buffers for fragment assembly
- offset - of this fragment within single sequence number
- data - raw/compressed bytes containing media specific for that code
```

<br/>


### Security

---

#### tldr; Written from cryptographic primitives and unnecessarily complicated for educational purposes. Might be (and most likely is) flawed.

<br/>

Based on public key certificates generated in self-designed json format on desktop (see pcdaemon/security/certificate_authority.py).

Public key of Certificate Authority(CA) (which is just owner's desktop in this case) is hardcoded into Android App. When client connects simplified TLS handshake is established:
- client sends hello message
- server sends a certificate containing its RSA public key, signed using RSA with the CA's private key
- client verifies the CA signature with the hardcoded CA public key, then generates a secret key for normal communication, encrypts it with the public key taken from the certificate, and sends it to the server
- server decrypts the secret key with its private key (stored on desktop)

Both parties share a secret key now (which could've been simply preshared instead of everything above).

Every succeding TCP transmission is encrypted with AES using an established secret key. Whole packets are authenticated with AES-GCM MACs.

User inputs a password which is sent to the server. If it's correct, the user will have full control over the PC.

Once the user is authenticated, the server generates another secret key used for UDP transmission and sends it to the user. All media streams are encrypted with ChaCha20 (which is faster than AES, which is used for messaging) and authenticated with Poly1305 MAC's using this secret key.

---

Both the TCP and UDP packets described above are encrypted and wrapped in the packet below:

```
TlS('ish) Packet
  |--------------------------------------------|
  |  tls_code(8)|    size(16)   | nonce_len(8) |
  |--------------------------------------------|
  {                                            }
  {             nonce(nonce_len * 8)           }
  {                                            }
  |--------------------------------------------|
  {                                            }
  {               data(size * 8)               }
  {                                            }
  |--------------------------------------------|

tls_code - see pcdaemon/media_msg_codes.py for details 
size - size of encapsuldated data
nonce_length - length in bytes of nonce
nonce - unique random number used to encrypt this packet
data - encrypted bytes of protocols described above
```

---

<br/>

## Compatibility and Performance

Phone application written in Java 8, for Android SDK >= 26

Server written in Python 3.8.10 tested on Ubuntu 20.04, but no OS specific code was used so it should work on Windows too.

Flawless audio transmission and ~25 fps when resolution lower than HD is streamed, about 10 fps when Full HD is streamed were reached in LAN conditions on 200$'ish class phone.
