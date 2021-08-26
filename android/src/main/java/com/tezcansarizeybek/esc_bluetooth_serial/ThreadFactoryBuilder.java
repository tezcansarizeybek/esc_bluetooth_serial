package com.tezcansarizeybek.esc_bluetooth_serial;

import java.util.concurrent.ThreadFactory;

public class ThreadFactoryBuilder implements ThreadFactory {

    private final String name;
    private final int counter;

    public ThreadFactoryBuilder(String name) {
        this.name = name;
        counter = 1;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setName("ThreadFactoryBuilder_" + name + "_" + counter);
        return thread;
    }
}
