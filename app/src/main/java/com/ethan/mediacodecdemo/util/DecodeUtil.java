package com.ethan.mediacodecdemo.util;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;


import com.ethan.mediacodecdemo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Vector;

public class DecodeUtil implements Runnable {
    private final static String TAG = "DecodeUtil";
    private MediaCodec mCodec;
    private MediaFormat mediaFormat;
    private InputStream mInputStream = null;
    private FileOutputStream mOutputStream = null;
    private boolean mStopFlag = true;
    private int INPUT_WIDTH = 1920;
    private int INPUT_HEIGHT = 1080;
    private int inFileName = R.raw.p_1920_1080;
    //private int FRAME_MIN_LEN = 1024; //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 60; //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MAX_LEN = 5000 * 1024; //一般H264帧大小不超过1M,如果解码失败可以尝试增大这个值
    boolean isStorageOutFile = true;

    private IOnDecodingEnd mIOnDecodingEnd;
    private final static String OPEN_FILE_EXCEPTION = "IOException";

    private final static String rootPath = "/storage/emulated/0/";
    private static String MIME_TYPE = "video/avc";
    private final static int[] DECODING_FILE_NAME = {R.raw.p_1280_720, R.raw.p_1920_1080, R.raw.p_2560_1440, R.raw.p_3840_2160};
    private final int[] resolution720 = {1280, 720};
    private final int[] resolution1080 = {1600, 1200};
    private final int[] resolution2k = {2560, 1440};
    private final int[] resolution4K = {3840, 2160};
    private String outFileName = "1080_123.yuv";
    byte[] mSps;
    byte[] mPps;
    long fps;
    private int frameRate;

    private Context context;

    public DecodeUtil(Context context) {
        this.context = context;
    }

    public void setInputFile(int inFile) {
        inFileName = inFile;
    }
    public void setFrameRate(int rate){
        frameRate = rate;
    }

    public void setOutputFile(String outFile) {
        outFileName = outFile;
    }

    // select codec according to mimeType
    private MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        //int numCodecs = MediaCodecList.getCodecInfos();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (codecInfo.isEncoder())
                continue;
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                Log.d(TAG,"selectCodec||types[j] =" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    Log.d(TAG,"selectCodec||codecInfo =" + codecInfo.getName());
                    return codecInfo;
                }
            }
        }
        return null;
    }


    // create video decoder
    private void createDecodeMediaCodec() {
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);

        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(MIME_TYPE);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int format = capabilities.colorFormats[i];
        }

        if (inFileName == DECODING_FILE_NAME[0]) {
            INPUT_WIDTH = resolution720[0];
            INPUT_HEIGHT = resolution720[1];
        } else if (inFileName == DECODING_FILE_NAME[1]) {
            INPUT_WIDTH = resolution1080[0];
            INPUT_HEIGHT = resolution1080[1];
        } else if (inFileName == DECODING_FILE_NAME[2]) {
            INPUT_WIDTH = resolution2k[0];
            INPUT_HEIGHT = resolution2k[1];
        } else if (inFileName == DECODING_FILE_NAME[3]) {
            INPUT_WIDTH = resolution4K[0];
            INPUT_HEIGHT = resolution4K[1];
        }
        Log.d(TAG, "createDecodeMediaCodec||INPUT_WIDTH ="
                + INPUT_WIDTH+"INPUT_HEIGHT =" + INPUT_HEIGHT + "framerate = " + frameRate);
        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, INPUT_WIDTH, INPUT_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, INPUT_HEIGHT * INPUT_WIDTH);
        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, INPUT_HEIGHT);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, INPUT_WIDTH);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        InputStream mStream = null;
        byte[] frame = new byte[FRAME_MAX_LEN];  //保存完整数据帧
        int bytes_cnt = 0;
        //open file
        mStream = context.getResources().openRawResource(inFileName);

        try {
            bytes_cnt = mStream.read(frame, 0, FRAME_MAX_LEN);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // find sps, and get the data of sps
        if (bytes_cnt > 0) {
            int headFirstIndex;  //寻找第一个帧头
            for (headFirstIndex = 0; headFirstIndex <= FRAME_MAX_LEN; headFirstIndex++) {
                //发现帧头
                if (isSPS(frame, headFirstIndex))
                    break;
            }
            int headSecondIndex = findHeadForSPS_PPS(frame, headFirstIndex + 4, FRAME_MAX_LEN);  //寻找第二个帧头
            mSps = new byte[headSecondIndex - headFirstIndex];
            System.arraycopy(frame, headFirstIndex, mSps, 0, headSecondIndex - headFirstIndex);  //将readData拷贝到frame
        }

        // find pps, and get the data of pps
        if (bytes_cnt > 0) {
            int headFirstIndex;  //寻找第一个帧头
            for (headFirstIndex = 0; headFirstIndex <= FRAME_MAX_LEN; headFirstIndex++) {
                //发现帧头
                if (isPPS(frame, headFirstIndex))
                    break;
            }
            int headSecondIndex = findHeadForSPS_PPS(frame, headFirstIndex + 4, FRAME_MAX_LEN);  //寻找第二个帧头
            mPps = new byte[headSecondIndex - headFirstIndex];
            System.arraycopy(frame, headFirstIndex, mPps, 0, headSecondIndex - headFirstIndex);  //copy from frame to mPps
        }

        try {
            mStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(mSps));
        mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(mPps));

        try {
//            mCodec = MediaCodec.createByCodecName("OMX.qcom.video.decoder.avc");
//            mCodec = MediaCodec.createByCodecName("OMX.google.h264.decoder");
            mCodec = MediaCodec.createByCodecName(codecInfo.getName());
//            Log.i(TAG, "mCodec =" + codecInfo.getName());
           // mCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            Log.d(TAG,"mCodec =" + mCodec.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

//        mCodec.configure(mediaFormat, null, null, 0);
//        mCodec.start();

        if (mCodec != null) {
            mCodec.configure(mediaFormat, null, null, 0);
            mCodec.start();
        }

        //open file
        mInputStream = context.getResources().openRawResource(inFileName);

        try {
            if (isStorageOutFile) {
                String outputPath = context.getExternalFilesDir(null) + "/" + outFileName;
                Log.d(TAG, "outputPath =" + outputPath);
                mOutputStream = new FileOutputStream(new File(outputPath));  //获取文件输出流
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        mStopFlag = false;
    }

    // release video decoder
    private void releaseDecodeMediaCodec() {
        if (mCodec != null) {
            mCodec.stop(); //停止解码，此时可以再次调用configure()方法
            mCodec.release();//释放内存
        }
    }

    @Override
    public void run() {
        createDecodeMediaCodec();
        try {
            decodeLoop();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        releaseDecodeMediaCodec();

        if (null != mIOnDecodingEnd) {
            if (fps == 0) {
                mIOnDecodingEnd.OnDecodingEnd(OPEN_FILE_EXCEPTION);
            } else {
                mIOnDecodingEnd.OnDecodingEnd(String.valueOf(fps));
            }
        }
    }

    private void decodeLoop() {
        ByteBuffer[] outputBuffers = mCodec.getOutputBuffers();
        byte[] frame = new byte[FRAME_MAX_LEN];  //保存完整数据帧
        byte[] readData = new byte[10 * 1024];  //每次从文件读取的数据

        //解码后的数据，包含每一个buffer的元数据信息，例如偏差，在相关解码器中有效的数据大小
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long timeoutUs = 17*1000;
        long count = 0;
        int input_count = 0;
        int bytes_cnt = 0;
        int out_count = 0;
        int frameLen = 0;  //当前帧长度
        Vector<byte[]> inputeStreams = new Vector<byte[]>();

        while (!mStopFlag) {
            try {
//                Thread.sleep(29);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//
//            try {
                bytes_cnt = mInputStream.read(readData, 0, 10 * 1024);
                //Log.e(TAG,"bytes_cnt = " + bytes_cnt);
                if (bytes_cnt < 0)
                    break;
                //当前长度小于最大值
                if (frameLen + bytes_cnt < FRAME_MAX_LEN) {
                    System.arraycopy(readData, 0, frame, frameLen, bytes_cnt);  //将readData拷贝到frame
                    frameLen += bytes_cnt;   //修改frameLen
                    int headFirstIndex = findHead(frame, 0, frameLen);  //寻找第一个帧头
//                    Log.e(TAG," Quentin headFirstIndex = " + headFirstIndex);
                    while (headFirstIndex >= 0 && isHead(frame, headFirstIndex)) {
                        int headSecondIndex = findHead(frame, headFirstIndex + FRAME_MIN_LEN, frameLen);  //寻找第二个帧头
//                        Log.e(TAG," Quentin headSecondIndex = " + headSecondIndex);
                        //如果第二个帧头存在，则两个帧头之间的就是一帧完整的数据
                        if (headSecondIndex > 0 && isHead(frame, headSecondIndex)) {
                            byte[] srcData = new byte[headSecondIndex - headFirstIndex];
                            System.arraycopy(frame, headFirstIndex, srcData, 0, headSecondIndex - headFirstIndex);
                            inputeStreams.add(srcData);
                            Log.d(TAG, "headSecondIndex =" + headSecondIndex + "frameLen" + frameLen);
                            byte[] temp = Arrays.copyOfRange(frame, headSecondIndex, frameLen); //截取headSecondIndex之后到frame的有效数据,并放到frame最前面
                            System.arraycopy(temp, 0, frame, 0, temp.length);
                            frameLen = temp.length; //修改frameLen的值
                            headFirstIndex = findHead(frame, 0, frameLen); //继续寻找数据帧
                        } else {
                            //找不到第二个帧头
                            headFirstIndex = -1;
                        }
                    }
                } else {
                    //如果长度超过最大值，frameLen置0
                    frameLen = 0;
                }

                if (inputeStreams.size() > 300)
                    break;
            } catch (IOException ex) {
                ex.printStackTrace();
                if (null != mIOnDecodingEnd) {
                    mIOnDecodingEnd.OnDecodingEnd(OPEN_FILE_EXCEPTION);
                }
            }
        }

        try {
            mInputStream.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        long startMills = calendar.getTimeInMillis();
        long startTime = System.nanoTime();
        long decodeTime;
        while (!mStopFlag) {
            int inputBufferId = mCodec.dequeueInputBuffer(timeoutUs); // 获取一个InputBufferId
            if (input_count == 1000)
                break;

            /*try {
                Thread.sleep(25);
            }catch (Exception ex) {
                ex.printStackTrace();
            }*/
//            Log.d(TAG,"harrison||inputBufferId =" + inputBufferId);
            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = mCodec.getInputBuffer(inputBufferId); // 获取一个 InputBuffer，用于填充未解码的数据
                inputBuffer.clear();
                byte[] data = inputeStreams.get(input_count % 300);
                inputBuffer.put(data, 0, data.length);
//                decodeTime = (System.nanoTime() - startTime) / 1000 + 100;
                mCodec.queueInputBuffer(inputBufferId, 0, data.length, input_count * 2000, 0);
            }
            input_count++;
            int outputBufferId = mCodec.dequeueOutputBuffer(info, timeoutUs);
//            Log.d(TAG,"harrison||outputBufferId =" + outputBufferId);
            if (outputBufferId >= 0) {
                //if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                //mCodec.releaseOutputBuffer(outputBufferId, false);
                //continue;
                //}
                Log.d(TAG, "harrison||outputBufferId =" + outputBufferId + " info.size = " + info.size);
                if (isStorageOutFile) {
                    ByteBuffer outputBuffer = mCodec.getOutputBuffer(outputBufferId);
                    if (out_count % 10 == 0) {
                        byte[] outStream = new byte[info.size];
                        outputBuffer.get(outStream, 0, info.size);
                        if (mOutputStream != null) {
                            try {
                                mOutputStream.write(outStream);
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }
                    }
                }
                out_count++;
                if (out_count % 60 == 0)
                    Log.d(TAG, " decode output count " + out_count);
                mCodec.releaseOutputBuffer(outputBufferId, false);
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                outputBuffers = mCodec.getOutputBuffers();
                Log.d(TAG, "decoder output buffer have changed");
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat tmp = mCodec.getOutputFormat();
                Log.d(TAG, "decoder output format change to " + tmp.toString());
            }
        }

        if (isStorageOutFile) {
            try {
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        }

        calendar = Calendar.getInstance();
        long endMills = calendar.getTimeInMillis();
        fps = out_count / ((endMills - startMills) / 1000);
        Log.e(TAG, " Decoder time use " + (endMills - startMills) + " frame fps = " + fps);
    }

    public void setOnDecodingEnd(IOnDecodingEnd threadEnd) {
        mIOnDecodingEnd = threadEnd;
    }

    public interface IOnDecodingEnd {
        void OnDecodingEnd(String fps);
    }

    /***************************** h.264 parser ************************************/
    /**
     * 寻找指定buffer中h264头的开始位置
     *
     * @param data   数据
     * @param offset 偏移量
     * @param max    需要检测的最大值
     * @return h264头的开始位置 ,-1表示未发现
     */
    private int findHead(byte[] data, int offset, int max) {
        int i;
        for (i = offset; i <= max; i++) {
            if (isHead(data, i))
                break;
        }
        if (i == max)
            i = -1;
        return i;
    }

    /**
     * 寻找指定buffer中h264头的开始位置
     *
     * @param data   数据
     * @param offset 偏移量
     * @param max    需要检测的最大值
     * @return h264头的开始位置 ,-1表示未发现
     */
    private int findHeadForSPS_PPS(byte[] data, int offset, int max) {
        int i;
        for (i = offset; i <= max; i++) {
            //发现帧头
            if (isHeadForSPS_PPS(data, i))
                break;
        }
        //检测到最大值，未发现帧头
        if (i == max) {
            i = -1;
        }
        return i;
    }

    /**
     * 判断是否是帧头:
     *
     * @param data
     * @param offset
     * @return 是否是帧头
     */
    private boolean isHeadForSPS_PPS(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[offset + 3] == 0x01) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01) {
            result = true;
        }
        return result;
    }

    /**
     * 判断是否是I帧/P帧头:
     * 00 00 00 01 65    (I帧)
     * 00 00 00 01 61 / 41   (P帧)
     *
     * @param data
     * @param offset
     * @return 是否是帧头
     */
    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[offset + 3] == 0x01 && isVideoFrameHeadType(data[offset + 4])) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && isVideoFrameHeadType(data[offset + 3])) {
            result = true;
        }
        return result;
    }

    /**
     * I帧或者P帧
     */
    private boolean isVideoFrameHeadType(byte head) {
        return head == (byte) 0x65 || head == (byte) 0x61 || head == (byte) 0x41;
    }

    /**
     * 判断是否是PPS帧头:
     * 00 00 00 01 68
     *
     * @param data
     * @param offset
     * @return 是否是PPS帧头
     */
    private boolean isPPS(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[offset + 3] == 0x01 && data[offset + 4] == (byte) 0x68) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && data[offset + 3] == (byte) 0x68) {
            result = true;
        }
        return result;
    }

    /**
     * 判断是否是SPS帧头:
     * 00 00 00 01 67
     *
     * @param data
     * @param offset
     * @return 是否是SPS帧头
     */
    private boolean isSPS(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[offset + 3] == 0x01 && data[offset + 4] == (byte) 0x67) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && data[offset + 3] == (byte) 0x67) {
            result = true;
        }
        return result;
    }
}
