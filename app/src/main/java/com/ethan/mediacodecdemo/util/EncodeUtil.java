package com.ethan.mediacodecdemo.util;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;

import com.ethan.mediacodecdemo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Vector;

public class EncodeUtil implements Runnable {
    private final static String TAG = "EncodeUtil";

    private final static String rootPath = "/storage/emulated/0";
    private IOnEncodingEnd mIOnEncodingEnd;
    private final static String OPEN_FILE_EXCEPTION = "IOException";

    private MediaCodec mCodec;
    private MediaFormat mediaFormat;
    private boolean mStopFlag = true;
    private InputStream mInputStream;
    private FileOutputStream mOutputStream = null;
    private int INPUT_WIDTH = 1920;
    private int INPUT_HEIGHT = 1080;
    private int BITRATE = 4 * 1024 * 1024;
    private int inFileName;
    private String outFileName;
    private String mimeType;
    private int frameRate; //60 设置成60的时候 4K的出帧速率不一样
    long fps;
    private final int ENCODE_FRAME_COUNT = 10;

    boolean isStorageOutFile = true;

    private final static int[] ENCODING_FILE_NAME = {R.raw.r_1280_720, R.raw.r_1920_1080,
            R.raw.r_2560_1440, R.raw.r_3840_2160, R.raw.r_2560_1440};
    private final int[] resolution720 = {1280, 720};
    private final int[] resolution1080 = {1920, 1080};
    private final int[] resolution2K = {2560, 1440};
    private final int[] resolution4K = {3840, 2160};
    private final int[] resolution2560 = {2560, 1440};

    private int time_total = 0;
    private int time_10 =0;
    private int time_20 = 0;
    private int time_30 = 0;
    private long tempMax = 0;
    private int encoderID = 0;

    Thread keyFrameSThread;
    int out_count = 0;

    private Context context;

    public EncodeUtil(Context context, int encoderId) {
        this.context = context;
        this.encoderID = encoderId;
    }

    public void setInputFile(int inFile) {
        inFileName = inFile;
    }

    public void setMimeType(String mType) {
        mimeType = mType;
    }

    public void setOutFileName(String outFile) {
        outFileName = outFile;
    }

    public void setFrameRate(int frameRate){
        this.frameRate = frameRate;
    }

    private MediaCodecInfo selectCodec(String mimeType) {
        Log.d(TAG, "selectCodec||mimeType =" + mimeType);
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private void createEncodeMediaCodec() {

        keyFrameSThread = new Thread() {
            public void run() {
                while (out_count < 1500) {
                    Bundle bundle = new Bundle();
                    bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                    mCodec.setParameters(bundle);
                    try {
                        Thread.sleep(300);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };
        MediaCodecInfo codecInfo = selectCodec(mimeType);

        if (inFileName == ENCODING_FILE_NAME[0]) {
            INPUT_WIDTH = resolution720[0];
            INPUT_HEIGHT = resolution720[1];
        } else if (inFileName == ENCODING_FILE_NAME[1]) {
            INPUT_WIDTH = resolution1080[0];
            INPUT_HEIGHT = resolution1080[1];
        } else if (inFileName == ENCODING_FILE_NAME[2]) {
            INPUT_WIDTH = resolution2K[0];
            INPUT_HEIGHT = resolution2K[1];
        } else if (inFileName == ENCODING_FILE_NAME[3]) {
            INPUT_WIDTH = resolution4K[0];
            INPUT_HEIGHT = resolution4K[1];
            BITRATE = 4 * 1024 * 1024;
            frameRate = 30;
        } else if (inFileName == ENCODING_FILE_NAME[4]) {
            INPUT_WIDTH = resolution2560[0];
            INPUT_HEIGHT = resolution2560[1];
        }
        Log.d(TAG, "createEncodeMediaCodec||INPUT_WIDTH ="
                + INPUT_WIDTH+"INPUT_HEIGHT =" + INPUT_HEIGHT + "framerate = " + frameRate);

        mediaFormat = MediaFormat.createVideoFormat(mimeType, INPUT_WIDTH, INPUT_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, 21);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 4 * 1024 * 1024);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
        //mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate); //帧率
        try {
            mCodec = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mCodec.start();

        //open file
        //获取文件输入流
        mInputStream = context.getResources().openRawResource(inFileName);
        try {
            if (isStorageOutFile) {
                String outputPath = context.getExternalFilesDir(null) + "/" + outFileName;
                Log.d(TAG,"outputPath =" + outputPath);
                mOutputStream = new FileOutputStream(new File(outputPath));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        mStopFlag = false;
    }

    private void releaseEncodeMediaCodec() {
        mCodec.stop();
        mCodec.release();
    }

    @Override
    public void run() {
        createEncodeMediaCodec();

        try {
            encodeLoop();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        releaseEncodeMediaCodec();
        if (null != mIOnEncodingEnd) {
            if (fps == 0) {
                mIOnEncodingEnd.OnEncodingEnd(OPEN_FILE_EXCEPTION);
            } else {
                mIOnEncodingEnd.OnEncodingEnd(String.valueOf(fps));
            }
        }
    }

    private void encodeLoop() {
        out_count = 0;
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        //ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
        //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        //timeoutUs为-1会无限等待，一旦某一帧有问题就会卡住，适当增大可以降低出帧的延迟
        long timeoutUs = 10*1000;

        long mStartTime = 0L;
        long mEndTime = 0L;
        long currentTime = 0L;
        boolean isFirstFrame = true;

        Vector<byte[]> inputeStreams = new Vector<byte[]>();
        try {
            for (int i = 0; i < ENCODE_FRAME_COUNT; i++) {
                byte[] inputStream = new byte[INPUT_WIDTH * INPUT_HEIGHT * 3 / 2];
                mInputStream.read(inputStream, 0, INPUT_WIDTH * INPUT_HEIGHT * 3 / 2);
                inputeStreams.add(inputStream);
            }
            mInputStream.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            if (null != mIOnEncodingEnd) {
                mIOnEncodingEnd.OnEncodingEnd(OPEN_FILE_EXCEPTION);
            }
        }

        //keyFrameSThread.start();
        Calendar calendar = Calendar.getInstance();
        long startMills = calendar.getTimeInMillis();
        Log.e(TAG, "time in mills " + startMills);
        int input_count = 0;
        long startTime = System.nanoTime();
        long encodeTime;
        //long inputTime = -1, lastInputTime = -1;
        while (!mStopFlag) {
//            if (inFileName == ENCODING_FILE_NAME[1]) {
//                try {
//                    Thread.sleep(27);
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
            int inputBufferId = mCodec.dequeueInputBuffer(timeoutUs);
            Log.d(TAG, "inputBufferId =" + inputBufferId);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = mCodec.getInputBuffer(inputBufferId);
                if (input_count++ == 1000)
                    break;
                byte[] data = inputeStreams.get(input_count % ENCODE_FRAME_COUNT);

                inputBuffer.put(data, 0, data.length);
                encodeTime = (System.nanoTime() - startTime) / 1000 + 100;
                Log.d(TAG, "encodeLoop||encoderId =" + encoderID);
                mCodec.queueInputBuffer(inputBufferId, 0, data.length, encodeTime, 0);
                inputBuffer.clear();
            }

            int outputBufferId = mCodec.dequeueOutputBuffer(info, timeoutUs);
            if (outputBufferId >= 0) {
                if (isStorageOutFile) {
                    mStartTime = System.currentTimeMillis();
                    ByteBuffer outputBuffer = mCodec.getOutputBuffer(outputBufferId);

                    byte[] outStream = new byte[info.size];
                    outputBuffer.get(outStream, 0, info.size);
                    try {
                        mOutputStream.write(outStream);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                out_count++;
                if (out_count % 60 == 0)
                    Log.d(TAG, " encode input count = " + input_count + " out_count = " + out_count);
                mCodec.releaseOutputBuffer(outputBufferId, 0);
                currentTime = System.currentTimeMillis();
                if (isFirstFrame) {
                    isFirstFrame = false;
                    mEndTime = currentTime;
                } else {
                    long elapsedTime = currentTime - mEndTime;
                    //Log.d(TAG, "harrison||elapsedTime =" + elapsedTime);
                    time_total ++;
                    if (elapsedTime < 10) {
                        time_10++;
                    } else if(elapsedTime < 20) {
                        time_20++;
                    }else if(elapsedTime < 30) {
                        time_30++;
                    } else {
                        if (elapsedTime > tempMax) {
                            tempMax = elapsedTime;
                        }
                    }
                    mEndTime = currentTime;
                }

            }
        }

        calendar = Calendar.getInstance();
        long endMills = calendar.getTimeInMillis();
        fps = out_count / ((endMills - startMills) / 1000);
        Log.e(TAG, "end time use " + (endMills - startMills) + " frame fps = " + fps);

        if (isStorageOutFile) {
            try {
                mOutputStream.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public void stopEncode() {
        mStopFlag = true;
    }

    public void setOnEncodingEnd(IOnEncodingEnd threadEnd) {
        mIOnEncodingEnd = threadEnd;
    }

    public interface IOnEncodingEnd {
        void OnEncodingEnd(String fps);
    }
}
