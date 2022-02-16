package com.example.pilot.networking.udp;

import java.net.DatagramPacket;
import java.util.Optional;

public class FragmentAssembler implements StreamSkippedObserver {
    private final FragmentBuffer fragmentBuffer;
    private final boolean requiresFragmentation;

    public FragmentAssembler(boolean requiresFragmentation) {
        this.fragmentBuffer = new FragmentBuffer();
        this.requiresFragmentation = requiresFragmentation;
    }

    // present if mediaFrame is fully assembled
    public Optional<MediaFrame> handleDatagram(DatagramPacket datagramPacket) {
        if (!requiresFragmentation) {
            MediaFrame mediaFrame = buildMediaFrame(datagramPacket);

            if (!mediaFrame.isFullyRecvd())
                throw new IllegalArgumentException("Expected full media frame, got fragmented with size "
                        + mediaFrame.getTotalSize() + ", got only " + datagramPacket.getLength());

            return Optional.of(mediaFrame);
        }

        return handleFragmentedDatagram(datagramPacket);
    }

    private Optional<MediaFrame> handleFragmentedDatagram(DatagramPacket datagramPacket) {
        byte[] packet = datagramPacket.getData();
        int seqNum = MediaFrame.extractSeqNum(packet);

        MediaFrame alreadyPresentFrame = fragmentBuffer.get(seqNum);
        if (alreadyPresentFrame != null) {
            alreadyPresentFrame.putFragment(packet, datagramPacket.getLength());

            if (alreadyPresentFrame.isFullyRecvd()) {
                fragmentBuffer.removeFullyReceived(alreadyPresentFrame);
                return Optional.of(alreadyPresentFrame);
            }
        }
        else {
            MediaFrame newFrame = buildMediaFrame(datagramPacket);
            fragmentBuffer.put(newFrame);
        }

        return Optional.empty();
    }

    private MediaFrame buildMediaFrame(DatagramPacket datagramPacket) {
        byte[] packetData = datagramPacket.getData();
        MediaFrame mediaFrame = MediaFrame.buildFromRaw(packetData);
        mediaFrame.putFragment(packetData, datagramPacket.getLength());

        return mediaFrame;
    }

    @Override
    public void onSkippedTo(int seqNum) {
        fragmentBuffer.removePreceding(seqNum);
    }
}
