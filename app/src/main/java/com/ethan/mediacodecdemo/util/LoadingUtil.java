/*
* Copyright (c) 2021, Quectel Wireless Solutions Co., Ltd. All rights reserved.
* Quectel Wireless Solutions Proprietary and Confidential.
*/

package com.ethan.mediacodecdemo.util;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


import java.lang.ref.WeakReference;

/*
 * for three points...
 */

public class LoadingUtil extends AppCompatTextView {
    private static String TAG = "Loading";
    private boolean isLoading = false;
    private int count = 0;
    private static final long delay = 500;
    private static final long period = 500;
    private final static int REFRESH = 0;

    private LoadingHandler mHandler;

    private static class LoadingHandler extends Handler {
        private WeakReference<LoadingUtil> mWeakReference;

        private LoadingHandler(LoadingUtil loadingUtil) {
            this.mWeakReference = new WeakReference<>(loadingUtil);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (null == mWeakReference.get()) {
                Log.e(TAG, "mWeakReference.get() is null,return");
                return;
            }
            switch (msg.what) {
                case REFRESH:
                    mWeakReference.get().startLoading();
                    break;
                default:
                    break;
            }
        }
    }

    public LoadingUtil(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHandler = new LoadingHandler(this);
    }

    public void drawLoading() {
        isLoading = true;
        Log.d(TAG, "drawLoading()");
        count = 0;
        mHandler.removeMessages(REFRESH);
        mHandler.sendEmptyMessageDelayed(REFRESH, delay);
    }

    private void startLoading() {
        if (getVisibility() != View.VISIBLE) {
            Log.d(TAG, "drawLoading() setVisibility");
            setVisibility(View.VISIBLE);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count % 4; i++) {
            builder.append(" .");
        }
        final String temp = builder.toString();
        LoadingUtil.this.post(new Runnable() {
            public void run() {
                setText(temp);
            }
        });
        count++;

        mHandler.sendEmptyMessageDelayed(REFRESH, period);
    }

    // stop drawing
    public void stopDraw() {
        isLoading = false;
        Log.d(TAG, "stopDraw()");
        mHandler.removeMessages(REFRESH);
        count = 0;
        setText("");
    }

    public boolean isLoading() {
        return this.isLoading;
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

}