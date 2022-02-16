package com.example.pilot;

import com.example.pilot.ui.utils.MediaFramesBuffer;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;

public class MediaFramesBufferTest {
    private static MediaFrame mf(long seq) {
        MediaFrame mediaFrame = Mockito.mock(MediaFrame.class);
        Mockito.when(mediaFrame.getSeqNum()).thenReturn(seq);
        return mediaFrame;
    }



    @Test
    public void framesCanBeAddedInOrder() {
        // given
        MediaFramesBuffer<MediaFrame> buffer = new MediaFramesBuffer<>(4);
        List<MediaFrame> mediaFrames = getMockList(3);

        // when
        mediaFrames.forEach(buffer::put);

        // then
        Assert.assertEquals(mediaFrames.size(), buffer.getFilledSize());
        Assert.assertEquals(mediaFrames.size(), buffer.getConsecutiveFilledSize());
    }


    @Test
    public void framesCanBeReceivedInOrder() {
        // given
        MediaFramesBuffer<MediaFrame> buffer = new MediaFramesBuffer<>(4);
        List<MediaFrame> mediaFrames = getMockList(3);

        // when
        mediaFrames.forEach(buffer::put);

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
        MediaFramesBuffer<MediaFrame> buffer = new MediaFramesBuffer<>(4);
        List<MediaFrame> mediaFrames = getMockList(3);

        // when
        buffer.put(mediaFrames.get(0));
        buffer.put(mediaFrames.get(2));
        buffer.put(mediaFrames.get(1));

        // then
        Assert.assertEquals(mediaFrames.size(), buffer.getConsecutiveFilledSize());

        mediaFrames.forEach(frame -> {
            Assert.assertEquals(buffer.get(), frame);
        });
    }


    @Test
    public void gapDoesntCountAsConsecutiveFrames() {
        // given
        MediaFramesBuffer<MediaFrame> buffer = new MediaFramesBuffer<>(6);
        List<MediaFrame> mediaFrames = getMockList(4);

        // when
        buffer.put(mediaFrames.get(0));
        buffer.put(mediaFrames.get(2));
        buffer.put(mediaFrames.get(3));

        // then
        Assert.assertEquals(1, buffer.getConsecutiveFilledSize());
        Assert.assertEquals(3, buffer.getFilledSize());
    }


    @Test
    public void gapCanBeSkipped() {
        // given
        MediaFramesBuffer<MediaFrame> buffer = new MediaFramesBuffer<>(6);
        List<MediaFrame> mediaFrames = getMockList(4);

        // when
        buffer.put(mediaFrames.get(0));
        buffer.put(mediaFrames.get(2));
        buffer.put(mediaFrames.get(3));

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
