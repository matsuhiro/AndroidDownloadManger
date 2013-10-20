
package com.matsuhiro.android.sample.app;

import com.matsuhiro.android.download.DownloadManager;
import com.matsuhiro.android.download.DownloadTask;
import com.matsuhiro.android.download.DownloadTaskListener;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import java.net.MalformedURLException;

public class MainActivity extends Activity {

    private int mTotalCount;
    private String mPath;
    private int mErrorNum;
    private int mSuccessNum;
    private int mQueuedNum;
    private DownloadManager mMgr;

    private TextView mText;
    private TextView mErrorText;
    private long mStartTime;
    private String mErrorUrls = "";
    
    private void updateText() {
        String text = "success : " + mSuccessNum + "/ error : " + mErrorNum + " / queued : " + mQueuedNum;
        mText.setText(text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mText = (TextView)findViewById(R.id.text_field);
        mErrorText = (TextView)findViewById(R.id.error_url);

        mErrorNum = 0;
        mSuccessNum = 0;
        mQueuedNum = 0;
        mTotalCount = ImageUrls.mUrls.length;
        updateText();
        mPath = this.getExternalFilesDir(null).getAbsolutePath();
        
        mMgr = new DownloadManager(this, new Handler());
        
        mStartTime = System.currentTimeMillis();
        for (int i = 0; i < mTotalCount; i++) {
            try {
                DownloadTask task = new DownloadTask(this, mMgr, ImageUrls.mUrls[i], mPath, mDownloadTaskListener);
                mMgr.addTask(task);
                mQueuedNum++;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        
    }

    DownloadTaskListener mDownloadTaskListener = new DownloadTaskListener() {

        @Override
        public void updateProcess(DownloadTask task) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void finishDownload(DownloadTask task) {
            mSuccessNum++;
            updateText();
            if ((mSuccessNum + mErrorNum) == mQueuedNum) {
                long endTime = System.currentTimeMillis();
                long time = endTime - mStartTime;
                Log.d("DownloadTest", "time : " + time);
                new AlertDialog.Builder(MainActivity.this)
                .setMessage("" + time + " msec")
                .show();
            }
        }

        @Override
        public void preDownload(DownloadTask task) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void queuedTask(DownloadTask task) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void pausedDownload(DownloadTask task) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void resumedDownload(DownloadTask task) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void deletedDownload(DownloadTask task) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void errorDownload(DownloadTask task, Throwable error) {
            mErrorNum++;
            updateText();
            mErrorUrls += "\n";
            mErrorUrls += task.getUrl();
            mErrorText.setText(mErrorUrls);
        }
        
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}
