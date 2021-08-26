package com.tezcansarizeybek.esc_bluetooth_serial;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.concurrent.*;


public class ThreadPool {

    private Runnable mActive;

    private static ThreadPool threadPool;

    private ThreadPoolExecutor threadPoolExecutor;

    private final static int CPU_AVAILABLE = Runtime.getRuntime().availableProcessors();

    private final static int MAX_POOL_COUNTS = CPU_AVAILABLE * 2 + 1;

    private final static long AVAILABLE = 1L;

    private final static int CORE_POOL_SIZE = CPU_AVAILABLE + 1;

    private final BlockingQueue<Runnable> mWorkQueue = new ArrayBlockingQueue<>(CORE_POOL_SIZE);

    private final ArrayDeque<Runnable> mArrayDeque = new ArrayDeque<>();

    private ThreadPool() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder("ThreadPool");
        threadPoolExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_COUNTS, AVAILABLE, TimeUnit.SECONDS, mWorkQueue, threadFactory);
    }

    public static ThreadPool getInstantiation() {
        if (threadPool == null) {
            threadPool = new ThreadPool();
        }
        return threadPool;
    }

    public void addParallelTask(Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("addTask(Runnable runnable)");
        }
        if (threadPoolExecutor.getActiveCount()<MAX_POOL_COUNTS) {
            Log.i("Lee","目前有"+threadPoolExecutor.getActiveCount()+" "+mWorkQueue.size()+"");
          synchronized (this){
              threadPoolExecutor.execute(runnable);
          }
        }
    }
    public synchronized void addSerialTask(final Runnable r) { //串行线程
        if (r == null) {
            throw new NullPointerException("addTask(Runnable runnable)");
        }
        mArrayDeque.offer(() -> {
            try {
                r.run();
            } finally {
                scheduleNext();
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }
    private void scheduleNext() {
        if ((mActive = mArrayDeque.poll()) != null) {
            threadPoolExecutor.execute(mActive);
        }
    }

    public void stopThreadPool() {
        if (threadPoolExecutor != null) {
            threadPoolExecutor.shutdown();
            threadPoolExecutor = null;
            threadPool = null;
        }
    }
}