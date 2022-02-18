package com.example.pilot.networking.udp;

import androidx.annotation.GuardedBy;

import java.util.Optional;

public class FragmentAssembler implements StreamSkippedObserver {
    @GuardedBy("this") private FragmentBuffer fragmentBuffer;
    private final boolean requiresFragmentation;

    public FragmentAssembler(boolean requiresFragmentation) {
        this.fragmentBuffer = new FragmentBuffer();
        this.requiresFragmentation = requiresFragmentation;
    }

    // present if mediaFrame is fully assembled
    public Optional<MediaFrame> handleDatagram(byte[] packet, int length) {
        if (!requiresFragmentation) {
            MediaFrame mediaFrame = buildMediaFrame(packet, length);

            if (!mediaFrame.isFullyRecvd())
                throw new IllegalArgumentException("Expected full media frame, got fragmented with size "
                        + mediaFrame.getTotalSize() + ", got only " + length);

            return Optional.of(mediaFrame);
        }

        return handleFragmentedDatagram(packet, length);
    }

    private synchronized Optional<MediaFrame> handleFragmentedDatagram(byte[] packet, int length) {
        int seqNum = MediaFrame.extractSeqNum(packet);

        MediaFrame alreadyPresentFrame = fragmentBuffer.get(seqNum);
        if (alreadyPresentFrame != null) {
            alreadyPresentFrame.putFragment(packet, length);

            if (alreadyPresentFrame.isFullyRecvd()) {
                fragmentBuffer.removeFullyReceived(alreadyPresentFrame);
                return Optional.of(alreadyPresentFrame);
            }
        }
        else {
            MediaFrame newFrame = buildMediaFrame(packet, length);
            fragmentBuffer.put(newFrame);

            // needed to keep counters valid, probably will never be ready tho
            if (newFrame.isFullyRecvd()) {
                fragmentBuffer.removeFullyReceived(newFrame);
                return Optional.of(newFrame);
            }
        }

        return Optional.empty();
    }

    private MediaFrame buildMediaFrame(byte[] packetData, int length) {
        MediaFrame mediaFrame = MediaFrame.buildFromRaw(packetData);
        mediaFrame.putFragment(packetData, length);

        return mediaFrame;
    }

    @Override
    public synchronized void onSkippedTo(int seqNum) {
        fragmentBuffer.removePreceding(seqNum);
    }

    public synchronized void clearBuffer() {
        this.fragmentBuffer = new FragmentBuffer();
    }
}
