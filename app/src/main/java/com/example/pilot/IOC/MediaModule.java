package com.example.pilot.IOC;

import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.ui.controller.MenuController;
import com.example.pilot.ui.utils.FPSCounter;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.ui.utils.ImageViewController;
import com.example.pilot.ui.utils.SoundPlayer;
import com.example.pilot.ui.utils.VideoPlayer;

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
    private final EditText keyboardInput;

    public MediaModule(GuiRunner guiRunner, ImageView imageView,
                       AppCompatActivity activity, EditText keyboardInput)
    {
        this.guiRunner = guiRunner;
        this.imageView = imageView;
        this.activity = activity;
        this.keyboardInput = keyboardInput;
    }


    @Provides
    @Singleton
    public ImageViewController provideImageViewer(MessageSender messageSender, MenuController menuController) {
        return new ImageViewController(activity, imageView, messageSender, menuController);
    }

    @Provides
    @Singleton
    public FPSCounter provideFPSCounter() {
        return new FPSCounter(MAX_VIDEO_FPS);
    }

    @Provides
    @Singleton
    public VideoPlayer provideVideoPlayer(FPSCounter fpsCounter, ImageViewController imageViewController) {
        return new VideoPlayer(guiRunner, imageViewController, fpsCounter, MAX_VIDEO_FPS);
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

    @Provides
    @Named("keyboard")
    public EditText provideKeyboardInput() {
        return keyboardInput;
    }

}
