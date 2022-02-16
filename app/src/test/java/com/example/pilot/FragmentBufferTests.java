package com.example.pilot;
import com.example.pilot.networking.udp.FragmentBuffer;
import com.example.pilot.networking.udp.MediaFrame;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;


public class FragmentBufferTests {
    private static MediaFrame mf(int seq) {
        MediaFrame mediaFrame = Mockito.mock(MediaFrame.class);
        Mockito.when(mediaFrame.getSeqNum()).thenReturn(seq);
        return mediaFrame;
    }

    private static FragmentBuffer getFragmentBuffer() {
        return new FragmentBuffer();
    }

    @Test
    public void emptyBufferReturnsNullForEverySeq() {
        // given
        FragmentBuffer buffer = getFragmentBuffer();

        // when
        MediaFrame mf1 = buffer.get(-1);
        MediaFrame mf2 = buffer.get(0);
        MediaFrame mf3 = buffer.get(1);

        //then
        Assert.assertNull(mf1);
        Assert.assertNull(mf2);
        Assert.assertNull(mf3);
    }

    @Test
    public void bufferReturnsNullIfFrameWithGivenSeqIsNotPresent() {
        // given
        FragmentBuffer buffer = getFragmentBuffer();
        MediaFrame mediaFrame = mf(2);
        buffer.put(mediaFrame);

        // when
        MediaFrame mf1 = buffer.get(-1);
        MediaFrame mf2 = buffer.get(0);
        MediaFrame mf3 = buffer.get(1);
        MediaFrame mf4 = buffer.get(3);

        // then
        Assert.assertNull(mf1);
        Assert.assertNull(mf2);
        Assert.assertNull(mf3);
        Assert.assertNull(mf4);
    }

    @Test
    public void bufferReturnsMediaFrameWithGivenSeqIfItsPresent() {
        // given
        FragmentBuffer buffer = getFragmentBuffer();
        MediaFrame mediaFrame = mf(2);
        buffer.put(mediaFrame);

        // when
        MediaFrame mediaFrame1 = buffer.get(2);

        // then
        Assert.assertSame(mediaFrame, mediaFrame1);
        Assert.assertEquals( 1, buffer.getSize());
    }

    @Test
    public void multipleFramesCanBeStored() {
        // given
        FragmentBuffer buffer = getFragmentBuffer();
        MediaFrame mf1 = mf(1);
        MediaFrame mf2 = mf(2);
        MediaFrame mf3 = mf(3);
        MediaFrame mf4 = mf(4);


        // when
        buffer.put(mf2);
        buffer.put(mf1);
        buffer.put(mf4);
        buffer.put(mf3);

        MediaFrame mf_1 = buffer.get(1);
        MediaFrame mf_2 = buffer.get(2);
        MediaFrame mf_3 = buffer.get(3);
        MediaFrame mf_4 = buffer.get(4);

        // then
        Assert.assertSame(mf1, mf_1);
        Assert.assertSame(mf2, mf_2);
        Assert.assertSame(mf3, mf_3);
        Assert.assertSame(mf4, mf_4);
    }

    @Test
    public void framesCanBeRemoved() {
        // given
        FragmentBuffer buffer = getFragmentBuffer();
        MediaFrame mf1 = mf(1);
        MediaFrame mf2 = mf(2);
        MediaFrame mf3 = mf(3);
        MediaFrame mf4 = mf(4);

        buffer.put(mf1);
        buffer.put(mf3);
        buffer.put(mf2);
        buffer.put(mf4);

        // when
        buffer.removeFullyReceived(mf2);
        MediaFrame mf_2 = buffer.get(2);

        // then
        Assert.assertEquals(3, buffer.getSize());
        Assert.assertNull(mf_2);
    }

    @Test
    public void orderIsPersistedWhenFrameIsRemoved() {
        // given
        FragmentBuffer buffer = getFragmentBuffer();
        MediaFrame mf1 = mf(1);
        MediaFrame mf2 = mf(2);
        MediaFrame mf3 = mf(3);
        MediaFrame mf4 = mf(4);

        buffer.put(mf1);
        buffer.put(mf3);
        buffer.put(mf2);
        buffer.put(mf4);

        // when
        buffer.removeFullyReceived(mf2);
        MediaFrame mf_1 = buffer.get(1);
        MediaFrame mf_3 = buffer.get(3);
        MediaFrame mf_4 = buffer.get(4);

        // then
        Assert.assertEquals(3, buffer.getSize());
        Assert.assertSame(mf1, mf_1);
        Assert.assertSame(mf3, mf_3);
        Assert.assertSame(mf4, mf_4);
    }
}
