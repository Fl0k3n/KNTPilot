package com.example.pilot;

import com.example.pilot.networking.udp.MediaFrame;
import com.example.pilot.networking.udp.MediaFramesBuffer;
import com.example.pilot.ui.utils.OverrunException;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;

public class MediaFramesBufferTest {
    private static MediaFrame mf(int seq) {
        MediaFrame mediaFrame = Mockito.mock(MediaFrame.class);
        Mockito.when(mediaFrame.getSeqNum()).thenReturn(seq);
        return mediaFrame;
    }


    private void insertFrames(List<MediaFrame> frames,  MediaFramesBuffer buffer) {
        for (MediaFrame mediaFrame: frames) {
            try {
                buffer.put(mediaFrame);
            } catch (OverrunException e) {
                Assert.fail("Insertion failed");
            }
        }
    }

    @Test
    public void framesCanBeAddedInOrder() {
        // given
        MediaFramesBuffer buffer = new MediaFramesBuffer(4);
        List<MediaFrame> mediaFrames = getMockList(3);

        // when
        insertFrames(mediaFrames, buffer);

        // then
        Assert.assertEquals(mediaFrames.size(), buffer.getFilledSize());
        Assert.assertEquals(mediaFrames.size(), buffer.getConsecutiveFilledSize());
    }


    @Test
    public void framesCanBeReceivedInOrder() {
        // given
        MediaFramesBuffer buffer = new MediaFramesBuffer(4);
        List<MediaFrame> mediaFrames = getMockList(3);

        // when
        insertFrames(mediaFrames, buffer);

        // then
        mediaFrames.forEach(frame -> {
            Assert.assertEquals(buffer.get(), frame);
        });

        Assert.assertEquals(0, buffer.getFilledSize());
        Assert.assertEquals(0, buffer.getConsecutiveFilledSize());
    }

    @Test
    public void outOfOrderGapCanBeFilled() {
        // given
        MediaFramesBuffer buffer = new MediaFramesBuffer(4);
        List<MediaFrame> mediaFrames = getMockList(3);

        // when
        try {
            buffer.put(mediaFrames.get(0));
            buffer.put(mediaFrames.get(2));
            buffer.put(mediaFrames.get(1));
        } catch (OverrunException e) {
            Assert.fail();
        }

        // then
        Assert.assertEquals(mediaFrames.size(), buffer.getConsecutiveFilledSize());

        mediaFrames.forEach(frame -> {
            Assert.assertEquals(buffer.get(), frame);
        });
    }


    @Test
    public void gapDoesntCountAsConsecutiveFrames() {
        // given
        MediaFramesBuffer buffer = new MediaFramesBuffer(6);
        List<MediaFrame> mediaFrames = getMockList(4);

        // when
        try {
            buffer.put(mediaFrames.get(0));
            buffer.put(mediaFrames.get(2));
            buffer.put(mediaFrames.get(3));
        } catch (OverrunException e) {
            Assert.fail();
        }

        // then
        Assert.assertEquals(1, buffer.getConsecutiveFilledSize());
        Assert.assertEquals(3, buffer.getFilledSize());
    }


    @Test
    public void gapCanBeSkipped() {
        // given
        MediaFramesBuffer buffer = new MediaFramesBuffer(6);
        List<MediaFrame> mediaFrames = getMockList(4);

        // when
        try {
            buffer.put(mediaFrames.get(0));
            buffer.put(mediaFrames.get(2));
            buffer.put(mediaFrames.get(3));
        } catch (OverrunException e) {
            Assert.fail();
        }

        buffer.skipMissingGap();
        // then

        Assert.assertEquals(2, buffer.getConsecutiveFilledSize());
        Assert.assertEquals(2, buffer.getFilledSize());
        Assert.assertEquals(mediaFrames.get(2), buffer.get());
        Assert.assertEquals(mediaFrames.get(3), buffer.get());
    }

    private static List<MediaFrame> getMockList(int size) {
        List<MediaFrame> res = new LinkedList<>();
        for (int i=0; i<size; i++)
            res.add(mf(i));
        return res;
    }


}
