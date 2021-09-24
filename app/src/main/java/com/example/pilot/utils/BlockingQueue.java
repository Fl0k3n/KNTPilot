package com.example.pilot.utils;

import java.util.LinkedList;

public class BlockingQueue<T> {
    private final LinkedList<T> tasks;

    public BlockingQueue() {
        tasks = new LinkedList<>();
    }

    public synchronized void put(T task) {
        tasks.add(task);
        this.notifyAll();
    }

    public synchronized T get() throws InterruptedException {
        while (tasks.isEmpty())
            this.wait();
        return this.tasks.removeFirst();
    }

    public synchronized void flush() {
        tasks.clear();
    }
}
