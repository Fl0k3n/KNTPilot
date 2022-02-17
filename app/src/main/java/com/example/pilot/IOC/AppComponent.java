package com.example.pilot.IOC;

import com.example.pilot.ui.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {SecurityModule.class, NetworkingModule.class, MediaModule.class})
public interface AppComponent {
    void inject(MainActivity mainActivity);
}