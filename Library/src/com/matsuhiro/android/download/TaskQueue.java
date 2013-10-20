package com.matsuhiro.android.download;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.os.AsyncTask;
import android.util.Log;

public class TaskQueue<T extends AsyncTask<?, ?, ?>> {
    private static final String TAG = TaskQueue.class.getSimpleName();

    private final BlockingQueue<T> mQueue = new LinkedBlockingQueue<T>();

    private volatile int mCount = 0;

    private final int MAXIMUM_RUN_TASK_COUNT;

    public TaskQueue(int maximumCount) {
        MAXIMUM_RUN_TASK_COUNT = maximumCount;
    }

    public T getTask() {
        T task = null;
        final int runCount;
        synchronized (this) {
            runCount = mCount - mQueue.size();
        }
        Log.d(TAG, "get task, mCount : " + mCount);
        Log.d(TAG, "get task, runCount : " + runCount);
        if (runCount >= MAXIMUM_RUN_TASK_COUNT) {
            synchronized (this) {
                try {
                    Log.d(TAG, "get wait");
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            try {
                task = mQueue.take();
                Log.d(TAG, "taken task");
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        return task;
    }

    public synchronized void putTask(T task) {
        try {
            mQueue.put(task);
            mCount += 1;
            Log.d(TAG, "put task, mCount : " + mCount);
            notifyAll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void removeTask(T task) {
        if (task == null) {
            return;
        }
        mQueue.remove(task);
        mCount -= 1;
        if (mCount < 0) {
            mCount = 0;
        }
        Log.d(TAG, "remove task, mCount : " + mCount);
        notifyAll();
    }

    public synchronized void notifyComplete() {
        mCount -= 1;
        if (mCount < 0) {
            mCount = 0;
        }
        Log.d(TAG, "completed task, mCount : " + mCount);
        notifyAll();
    }

    public void removeAll() {
        for (T item : mQueue) {
            mQueue.remove(item);
        }
    }

    public int getCount() {
        return mCount;
    }
}
