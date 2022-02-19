package com.example.pilot.networking.udp;

public class FragmentBuffer {

    private static class Node {
        public final MediaFrame value;
        public Node next;

        public Node(MediaFrame value, Node next) {
            this.value = value;
            this.next = next;
        }

        public int getSeqNum() {
            return value.getSeqNum();
        }
    }

    private Node head;
    private Node tail;
    private int lastFullyRcvdInOrderSeq;
    private int size;

    public FragmentBuffer(int lastFullyRcvdInOrderSeq) {
        this.head = this.tail = null;
        this.lastFullyRcvdInOrderSeq = -1;
        this.size = 0;
    }

    // not wrapped in optional for performance reasons, will return null if this seqNum is not present
    public MediaFrame get(int seqNum) {
        if (head == null || seqNum < head.getSeqNum() || seqNum > tail.getSeqNum())
            return null;

        // with high probability it will be head so explicit check should be worth
        if (head.getSeqNum() == seqNum)
            return head.value;

        Node tmp = head;
        while (tmp != tail && seqNum >= tmp.next.getSeqNum()) {
            tmp = tmp.next;
        }

        return tmp.getSeqNum() == seqNum ? tmp.value : null;
    }

    // mediaFrame with that seq can't be present
    public void put(MediaFrame mediaFrame) {
        int seqNum = mediaFrame.getSeqNum();

        if (seqNum <= lastFullyRcvdInOrderSeq)
            return;

        Node node = new Node(mediaFrame, null);
        this.size++;

        if (head == null) {
            head = tail = node;
        }
        else if (seqNum > tail.getSeqNum()) {
            tail.next = node;
            tail = node;
        }
        else if (seqNum < head.getSeqNum()) {
            node.next = head;
            head = node;
        }
        else {
            Node tmp = head;
            while (tmp != tail && tmp.next.getSeqNum() < seqNum) {
                tmp = tmp.next;
            }

            node.next = tmp.next;
            tmp.next = node;
        }
    }

    public void removeFullyReceived(MediaFrame mediaFrame) {
        int seqNum = mediaFrame.getSeqNum();
        this.size--;

        if (seqNum == head.value.getSeqNum()) {
            lastFullyRcvdInOrderSeq = mediaFrame.getSeqNum();
            head = head.next;
            if (head == null)
                tail = null;
        }
        else {
            Node tmp = head.next;
            Node prev = head;
            while(tmp != tail) {
                if (tmp.value.getSeqNum() == seqNum)
                    break;
                prev = tmp;
                tmp = tmp.next;
            }

            if (tmp == tail) {
                prev.next = null;
                tail = prev;
            }
            else {
                prev.next = tmp.next;
            }
        }
    }

    // remove all media frames with sequence < seqNum,
    // if fragment with sequence < seqNum is received after this call
    // it will be ignored
    public void removePreceding(int seqNum) {
        lastFullyRcvdInOrderSeq = seqNum;

        if (head == null)
            return;

        if (seqNum > tail.getSeqNum()) {
            head = tail = null;
            this.size = 0;
        }
        else {
            while (head != null && head.getSeqNum() < seqNum) {
                head = head.next;
                this.size--;
            }
        }
    }

    public int getSize() {
        return size;
    }

    public int getLastSeq() {
        if (tail != null)
            return tail.getSeqNum();
        return lastFullyRcvdInOrderSeq;
    }
}
