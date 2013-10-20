
package com.matsuhiro.android.download;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadManager {

    private DownloadHandler mDownloadHandler;

    private TaskQueue<DownloadTask> mTaskQueue;

    private TaskManageThread<DownloadTask, String> mTaskManageThread;

    private static final int NUMBER_OF_THREADS_IN_SAME_TIME = 4;

    private static final int ADD_TASK = 0;

    private static final int PAUSE_TASK = 1;

    private static final int RESUME_TASK = 2;

    private static final int DELETE_TASK = 3;

    private ConcurrentHashMap<String, DownloadTask> mDownloadTasks;
    private ConcurrentHashMap<String, DownloadTask> mPausedDownloadTasks;

    private Handler mDownloadListenerHandelr = null;

    public DownloadManager(Context context, Handler handler) {
        mDownloadListenerHandelr = handler;
        HandlerThread handlerThread = new HandlerThread("DownlaodManagerThread");
        handlerThread.start();

        mDownloadTasks = new ConcurrentHashMap<String, DownloadTask>();
        mPausedDownloadTasks = new ConcurrentHashMap<String, DownloadTask>();

        Looper looper = handlerThread.getLooper();
        mDownloadHandler = new DownloadHandler(looper, context, this);
        mTaskQueue = new TaskQueue<DownloadTask>(NUMBER_OF_THREADS_IN_SAME_TIME);
        mTaskManageThread = new TaskManageThread<DownloadTask, String>(mTaskQueue);
        mTaskManageThread.start();
    }

    private void notifyEvent(final DownloadTask task, final int event, final String url, final Throwable error) {
        if (mDownloadListenerHandelr == null) {
            return;
        }
        mDownloadListenerHandelr.post(new Runnable() {
            public void run() {
                DownloadTaskListener listener = task.getListener();
                if (!(listener instanceof DownloadTaskListener)) {
                    return;
                }
                switch (event) {
                case EVENT_ADDED:
                    listener.queuedTask(task);
                    break;
                case EVENT_PAUSED:
                    listener.pausedDownload(task);
                    break;
                case EVENT_RESUMED:
                    listener.resumedDownload(task);
                    break;
                case EVENT_DELETED:
                    listener.deletedDownload(task);
                    break;
                case EVENT_ERROR:
                    listener.errorDownload(task, error);
                    break;
                }
            }
        });
    }

    public static final int EVENT_ADDED = 1;
    public static final int EVENT_COMPLETED = EVENT_ADDED + 1;
    public static final int EVENT_PAUSED = EVENT_COMPLETED + 1;
    public static final int EVENT_RESUMED = EVENT_PAUSED + 1;
    public static final int EVENT_DELETED = EVENT_RESUMED + 1;
    public static final int EVENT_PROGRESS = EVENT_DELETED + 1;
    public static final int EVENT_ERROR = EVENT_PROGRESS + 1;

    public void addTask(DownloadTask task) {
        sendMessage(ADD_TASK, 0, 0, task);
    }

    public void pause(String url) {
        DownloadTask task = mDownloadTasks.get(url);
        if (task instanceof DownloadTask) {
            sendMessage(PAUSE_TASK, 0, 0, task);
        }
    }

    public void resume(String url) {
        DownloadTask task = mPausedDownloadTasks.get(url);
        if (task instanceof DownloadTask) {
            sendMessage(RESUME_TASK, 0, 0, task);
        }
    }

    public void delete(String url) {
        DownloadTask task = mDownloadTasks.get(url);
        if (task instanceof DownloadTask) {
            sendMessage(DELETE_TASK, 0, 0, task);
            return;
        }
        task = mPausedDownloadTasks.get(url);
        if (task instanceof DownloadTask) {
            sendMessage(DELETE_TASK, 0, 0, task);
        }
    }

    public void notifyCompleted() {
        mTaskQueue.notifyComplete();
    }

    private void sendMessage(int what, int arg1, int arg2, Object obj) {
        Message msg = new Message();
        msg.what = what;
        msg.arg1 = arg1;
        msg.arg2 = arg2;
        msg.obj = obj;
        mDownloadHandler.sendMessage(msg);
    }

    private static class DownloadHandler extends Handler {
        private final WeakReference<Context> mConextRef;
        private final WeakReference<DownloadManager> mDownloadManagerRef;

        DownloadHandler(Looper looper, Context context, DownloadManager dnManager) {
            super(looper);
            mConextRef = new WeakReference<Context>(context);
            mDownloadManagerRef = new WeakReference<DownloadManager>(dnManager);
        }

        @Override
        public void handleMessage(Message msg) {
            Context context = mConextRef.get();
            if (!(context instanceof Context)) {
                // error
                return;
            }
            DownloadManager dnMgr = mDownloadManagerRef.get();
            DownloadTask task = (DownloadTask)msg.obj;
            switch(msg.what) {
                case ADD_TASK:
                    {
                        String url = task.getUrl();
                        if (dnMgr.mDownloadTasks.containsKey(url)) {
                            DownloadException e = new DownloadException("" + url + " is downloading");
                            dnMgr.notifyEvent(task, EVENT_ERROR, url, e);
                        }
                        if (dnMgr.mPausedDownloadTasks.containsKey(url)) {
                            DownloadException e = new DownloadException("" + url + " is paused");
                            dnMgr.notifyEvent(task, EVENT_ERROR, url, e);
                        }
                        dnMgr.mDownloadTasks.put(url, task);
                        dnMgr.mTaskQueue.putTask(task);
                        dnMgr.notifyEvent(task, EVENT_ADDED, url, null);
                    }
                    break;
                case PAUSE_TASK:
                    {
                        task.cancel();
                        String url = task.getUrl();
                        dnMgr.mDownloadTasks.remove(url);
                        dnMgr.mTaskQueue.removeTask(task);
                        try {
                            DownloadTask newTask = new DownloadTask(
                                    task.getContext(),
                                    dnMgr, 
                                    task.getUrl(),
                                    task.getFileName(),
                                    task.getPath(),
                                    task.getListener());
                            dnMgr.mPausedDownloadTasks.put(url, newTask);
                            dnMgr.notifyEvent(task, EVENT_PAUSED, url, null);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                            dnMgr.notifyEvent(task, EVENT_ERROR, url, e);
                        }
                    }
                    break;
                case RESUME_TASK:
                    {
                        String url = task.getUrl();
                        dnMgr.mPausedDownloadTasks.remove(url);
                        dnMgr.mDownloadTasks.put(url, task);
                        dnMgr.mTaskQueue.putTask(task);
                        dnMgr.notifyEvent(task, EVENT_RESUMED, url, null);
                    }
                    break;
                case DELETE_TASK:
                    {
                        task.cancel();
                        String url = task.getUrl();
                        dnMgr.mPausedDownloadTasks.remove(url);
                        dnMgr.mDownloadTasks.remove(url);
                        dnMgr.mTaskQueue.removeTask(task);
                        dnMgr.notifyEvent(task, EVENT_DELETED, url, null);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
