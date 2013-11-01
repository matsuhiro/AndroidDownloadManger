
package com.matsuhiro.android.sample.app;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class VolleySampleActivity extends Activity {
    private int mTotalCount;
    private String mPath;
    private int mErrorNum;
    private int mSuccessNum;
    private int mQueuedNum;
    private TextView mText;
    private long mStartTime;
    private RequestQueue mRequestQueue;
    
    private void updateText() {
        String text = "success : " + mSuccessNum + "/ error : " + mErrorNum + " / queued : " + mQueuedNum;
        mText.setText(text);
    }
    
    private void showDialog() {
        if ((mSuccessNum + mErrorNum) == mQueuedNum) {
            android.os.Debug.stopMethodTracing();
            long endTime = System.currentTimeMillis();
            long time = endTime - mStartTime;
            Log.d("DownloadTest", "time : " + time);
            new AlertDialog.Builder(VolleySampleActivity.this)
            .setMessage("" + time + " msec ")
            .show();
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mText = (TextView) findViewById(R.id.text_field);

        mErrorNum = 0;
        mSuccessNum = 0;
        mQueuedNum = 0;
        mTotalCount = ImageUrls.mUrls.length;
        mPath = this.getExternalFilesDir(null).getAbsolutePath();
        
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        
        mStartTime = System.currentTimeMillis();
        for (int i = 0; i < mTotalCount; i++) {
            URL url;
            try {
                url = new URL(ImageUrls.mUrls[i]);
                String fileName = new File(url.getFile()).getName();
                final File file = new File(mPath, fileName);
                ImageRequest request = new ImageRequest(ImageUrls.mUrls[i], new Listener<Bitmap>() {
                    @Override
                    public void onResponse(final Bitmap response) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    FileOutputStream out = new FileOutputStream(file);
                                    response.compress(CompressFormat.JPEG, 100, out);
                                    out.flush();
                                    out.close();
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        mSuccessNum++;
                        Log.d("VolleySample", file.getName() + "is downloaded");
                        Log.d("VolleySample", "success count " + mSuccessNum);
                        updateText();
                        showDialog();
                    }
                }, 0, 0, Config.ARGB_8888, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        mErrorNum++;
                        Log.d("VolleySample", "error count " + mErrorNum);
                        updateText();
                        showDialog();
                    }
                });
                
                request.setTag(ImageUrls.mUrls[i]);
                mRequestQueue.add(request);
                mQueuedNum++;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        mRequestQueue.start();
    }
}
