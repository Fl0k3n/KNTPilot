package com.example.pilot.IOC;

import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.ui.utils.FPSCounter;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.ui.utils.SoundPlayer;
import com.example.pilot.ui.utils.VideoPlayer;
import com.example.pilot.ui.views.ImageViewer;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MediaModule {
    private static final int MAX_VIDEO_FPS = 30;
    private static final int AUDIO_SAMPLES_IN_FRAME = 256;

    private final GuiRunner guiRunner;
    private final ImageView imageView;
    private final AppCompatActivity activity;

    public MediaModule(GuiRunner guiRunner, ImageView imageView, AppCompatActivity activity) {
        this.guiRunner = guiRunner;
        this.imageView = imageView;
        this.activity = activity;
    }


    @Provides
    @Singleton
    public ImageViewer provideImageViewer(MessageSender messageSender) {
        return new ImageViewer(activity, imageView) {

            @Override
            public void onSwipe(float real_dx, float real_dy) {
                messageSender.sendSwipeMessage(real_dx, real_dy);
            }

            @Override
            public void onClick(float x, float y) {
                messageSender.sendClickMessage(x, y);
            }
        };
    }


    @Provides
    @Singleton
    public FPSCounter provideFPSCounter() {
        return new FPSCounter(MAX_VIDEO_FPS);
    }

    @Provides
    @Singleton
    public VideoPlayer provideVideoPlayer(FPSCounter fpsCounter, ImageViewer imageViewer) {
        return new VideoPlayer(guiRunner, imageViewer, fpsCounter, MAX_VIDEO_FPS);
    }

    @Provides
    @Singleton
    public SoundPlayer provideSoundPlayer() {
        return new SoundPlayer(true, 44100, 384, AUDIO_SAMPLES_IN_FRAME);
    }

    @Provides
    @Named("fps ui update time")
    public long provideUIUpdateTime() {
        return 500;
    }

    @Provides
    @Named("fps ui update time unit")
    public TimeUnit provideUIUpdateTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    @Provides
    @Named("auth activity")
    public AppCompatActivity provideAuthActivity() {
        return activity;
    }

    @Provides
    @Named("settings activity")
    public AppCompatActivity provideSettingsActivity() {
        return activity;
    }

    @Provides
    @Named("main activity")
    public AppCompatActivity provideMainActivity() {
        return activity;
    }

    @Provides
    public GuiRunner provideGuiRunner() {
        return guiRunner;
    }
}
