package com.ethan.mediacodecdemo.biz;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;


import com.ethan.mediacodecdemo.R;
import com.ethan.mediacodecdemo.bean.MediaCodecItem;
import com.ethan.mediacodecdemo.util.CpuRateUtil;
import com.ethan.mediacodecdemo.util.DecodeUtil;
import com.ethan.mediacodecdemo.util.EncodeUtil;
import com.ethan.mediacodecdemo.util.EncodeUtil.IOnEncodingEnd;
import com.ethan.mediacodecdemo.util.LoadingUtil;
import com.ethan.mediacodecdemo.util.MediacodecItemAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "EncDecMain";

    private final static int MSG_DISABLE_START = 0X01;
    private final static int MSG_ENABLE_START = MSG_DISABLE_START + 1;
    private final static int MSG_ENABLE_TIMEOUT = MSG_DISABLE_START + 2;
    private final static int MSG_GET_CPU_RATE = MSG_DISABLE_START + 3;
    private final static int MSG_ENCDOE_DONE = MSG_DISABLE_START + 4;
    private final static int MSG_DECODE_DONE = MSG_DISABLE_START + 5;
    private final static int MSG_ENCODE_DONE_OUT = MSG_DISABLE_START + 6;
    private final static int MSG_DECODE_TIME_OUT = MSG_DISABLE_START + 7;

    private final static int ENABLE_TIMEOUT = 10 * 60 * 1000;
    private final static int GET_CPU_INTERVAL = 1000;

    private final static int[] ENCODING_FILE_NAME = {R.raw.r_1280_720, R.raw.r_1920_1080,
            R.raw.r_2560_1440, R.raw.r_3840_2160, R.raw.r_2560_1440};
    private final static int[] DECODING_FILE_NAME = {R.raw.p_1280_720, R.raw.p_1920_1080, R.raw.p_2560_1440, R.raw.p_3840_2160};
    private final static String OPEN_FILE_EXCEPTION = "IOException";

    //  widget encode parameter
    private Spinner mRecordNumberSpinner;
    private Spinner mResolutionSpinner;
    private Spinner mEncodeFormatSpinner;
    private Spinner mMainRateSpinner;
    private Spinner mFileFormatSpinner;
    private Spinner mSubRateSpinner;
    private Spinner mStreamResolutionSpinner;
    private Spinner mFileSizeSpinner;
    //  widget for decode parameter
    private Spinner mDecodeNumberSpinner;
    private Spinner mDecodeResolutionSpinner;
    private Spinner mDecodeFormatSpinner;
    private Spinner mEncodeFrameRateSpinner;
    private Spinner mDecodeFrameRateSpinner;
    //output related
    private Switch mStreamSwitchBn;
    private Switch mAudioSwitchBn;
    private LoadingUtil mLoadingUtil;
    private Button mStartButton;

    private TextView outputfpsContents;
    private TextView decodeOutputfpsContents;
    private TextView processText;
    private TextView outputCPUContents;

    private ArrayAdapter<CharSequence> mRecordNumberAdapter;
    private ArrayAdapter<CharSequence> mResolutionAdapter;
    private ArrayAdapter<CharSequence> mEncodeFormaAdapter;
    private ArrayAdapter<CharSequence> mMainRateAdapter;
    private ArrayAdapter<CharSequence> mFileFormatAdapter;
    private ArrayAdapter<CharSequence> mSubRateAdapter;
    private ArrayAdapter<CharSequence> mStreamResolutionAdapter;
    private ArrayAdapter<CharSequence> mFileSizeAdapter;

    private ArrayAdapter<CharSequence> mDecodeNumberAdapter;
    private ArrayAdapter<CharSequence> mDecodeResolutionAdapter;
    private ArrayAdapter<CharSequence> mDecodeFormaAdapter;
    private ArrayAdapter<CharSequence> mEncodeFrameRateAdapter;
    private ArrayAdapter<CharSequence> mDecodeFrameRateAdapter;

    private int recordNum;
    private int encodeFrameRateNum = 0;
    private int decodeFrameRateNum = 0;
    private int resolutionID;
    private int decodingNum;
    private int decodingResolutionID;
    CpuRateUtil mCpuRateUtil = new CpuRateUtil();

    private List<String> fpsEncodeList = new ArrayList<String>();
    private List<String> fpsDecodeList = new ArrayList<String>();
    private List<EncodeUtil> encodeUtils = new ArrayList<>();

    private MyHandler mHandler;
    private ImageView add,delete;
    private ListView listView;
    private MediacodecItemAdapter adapter;
    private List<MediaCodecItem> dataList = new ArrayList<>();

    /**
     * Update UI and display results via handler
     */

    private static class MyHandler extends Handler {
        private WeakReference<MainActivity> mActivity;

        private MyHandler(MainActivity activity) {
            this.mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            final MainActivity activity = mActivity.get();
            if (null == activity) {
                Log.e(TAG, "mActivity.get is null,return");
                return;
            }
            Log.d(TAG, "handleMessage||msg.what =" + msg.what);
            switch (msg.what) {
                case MSG_DISABLE_START:
                    activity.processText.setVisibility(View.VISIBLE);
                    activity.mLoadingUtil.setVisibility(View.VISIBLE);
                    activity.mLoadingUtil.drawLoading();

                    activity.mStartButton.setEnabled(false);
                    activity.outputfpsContents.setText("");
                    activity.decodeOutputfpsContents.setText("");
                    activity.processText.setVisibility(View.VISIBLE);
                    activity.processText.setText("处理中");
                    break;
                case MSG_ENCDOE_DONE:
                    removeMessages(MSG_GET_CPU_RATE);
                    removeMessages(MSG_ENCODE_DONE_OUT);

                    activity.processText.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.stopDraw();
                    activity.mStartButton.setEnabled(true);
                    activity.mStartButton.setText("START");
                    activity.add.setEnabled(true);
                    activity.delete.setEnabled(true);

                    StringBuilder stringBuilder = new StringBuilder();
                    if (null != activity.fpsEncodeList && !activity.fpsEncodeList.isEmpty()) {
                        for (int i = 0; i < activity.fpsEncodeList.size(); i++) {
                            stringBuilder.append("编码").append(i + 1).append("帧率:")
                                    .append(activity.fpsEncodeList.get(i)).append(";")
                                    .append("\n");
                        }
                        String result = stringBuilder.toString();
                        activity.outputfpsContents.setText(result);

                    }
                    break;
                case MSG_DECODE_DONE:
                    removeMessages(MSG_GET_CPU_RATE);
                    removeMessages(MSG_DECODE_TIME_OUT);

                    activity.processText.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.stopDraw();
                    activity.mStartButton.setEnabled(true);
                    activity.mStartButton.setText("START");
                    activity.add.setEnabled(true);
                    activity.delete.setEnabled(true);

                    StringBuilder decodeBuilder = new StringBuilder();
                    if (null != activity.fpsDecodeList && !activity.fpsDecodeList.isEmpty()) {
                        for (int i = 0; i < activity.fpsDecodeList.size(); i++) {
                            decodeBuilder.append("解码").append(i + 1).append("帧率:")
                                    .append(activity.fpsDecodeList.get(i)).append(";")
                                    .append("\n");
                        }
                        String result = decodeBuilder.toString();
                        activity.decodeOutputfpsContents.setText(result);
                    }
                    //解码一般比较快，解码结束时，通知停止编码
                    activity.stopEncoding();
                    break;
                case MSG_ENABLE_START:
                    removeMessages(MSG_GET_CPU_RATE);

                    activity.processText.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.stopDraw();
                    activity.mStartButton.setEnabled(true);
                    break;
                case MSG_GET_CPU_RATE:
                    activity.outputCPUContents.setText(activity.mCpuRateUtil.getCPURate());
                    sendEmptyMessageDelayed(MSG_GET_CPU_RATE, 1000);
                    break;
                case MSG_ENABLE_TIMEOUT:
                    removeMessages(MSG_DISABLE_START);
                    removeMessages(MSG_ENABLE_START);
                    removeMessages(MSG_GET_CPU_RATE);

                    activity.processText.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.setVisibility(View.INVISIBLE);
                    activity.mLoadingUtil.stopDraw();
                    activity.mStartButton.setEnabled(true);
                    activity.add.setEnabled(true);
                    activity.delete.setEnabled(true);
                    activity.outputfpsContents.setText("获取失败，请重试");
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);
        initViews();
        initAdapter();
    }


    private void initViews() {

        mRecordNumberSpinner = findViewById(R.id.record_num_spinner);
        mResolutionSpinner = findViewById(R.id.record_resolution_spinner);
        mEncodeFormatSpinner = findViewById(R.id.encode_format_spinner);
        mMainRateSpinner = findViewById(R.id.main_rate_spinner);
        mFileFormatSpinner = findViewById(R.id.file_format_spinner);
        mSubRateSpinner = findViewById(R.id.sub_rate_spinner);
        mStreamResolutionSpinner = findViewById(R.id.stream_resolution_spinner);
        mFileSizeSpinner = findViewById(R.id.file_size_spinner);

        mDecodeNumberSpinner = findViewById(R.id.decode_num_spinner);
        mDecodeResolutionSpinner = findViewById(R.id.decode_resolution_spinner);
        mDecodeFormatSpinner = findViewById(R.id.decode_format_spinner);
        mEncodeFrameRateSpinner = findViewById(R.id.framerate_spinner);
        mDecodeFrameRateSpinner = findViewById(R.id.decode_framerate_spinner);

        mStreamSwitchBn = findViewById(R.id.stream_switch);
        mAudioSwitchBn = findViewById(R.id.audio_switch);

        mStartButton = findViewById(R.id.start_bt);
        outputfpsContents = findViewById(R.id.output_fps_content);
        decodeOutputfpsContents = findViewById(R.id.decode_output_fps_content);
        processText = findViewById(R.id.processing_text);
        mLoadingUtil = findViewById(R.id.processing_loading);
        outputCPUContents = findViewById(R.id.output_cpu_content);

        mStreamSwitchBn.setOnCheckedChangeListener(new MyCheckedChangeListener());
        mAudioSwitchBn.setOnCheckedChangeListener(new MyCheckedChangeListener());
        mStartButton.setOnClickListener(new MyClickListener());

        listView = findViewById(R.id.list_item);
        MediaCodecItem item = new MediaCodecItem();
        dataList.add(item);
        adapter = new MediacodecItemAdapter(MainActivity.this, dataList);
        listView.setAdapter(adapter);
//        addData();
        add = findViewById(R.id.add);
        delete = findViewById(R.id.delete);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.addData();
                Log.d(TAG, "add item , list.size = " + adapter.getCount());
            }
        });
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.deleteData();
                Log.d(TAG, "delete item , list.size = " + adapter.getCount());
            }
        });
    }


    private void initAdapter() {
        mRecordNumberAdapter = ArrayAdapter.createFromResource(this, R.array.record_number, R.layout.spinner_item_layout);
        mRecordNumberAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mResolutionAdapter = ArrayAdapter.createFromResource(this, R.array.record_resolution, R.layout.spinner_item_layout);
        mResolutionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mEncodeFormaAdapter = ArrayAdapter.createFromResource(this, R.array.encode_format, R.layout.spinner_item_layout);
        mEncodeFormaAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mMainRateAdapter = ArrayAdapter.createFromResource(this, R.array.main_rate, R.layout.spinner_item_layout);
        mMainRateAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mFileFormatAdapter = ArrayAdapter.createFromResource(this, R.array.file_format, R.layout.spinner_item_layout);
        mFileFormatAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mSubRateAdapter = ArrayAdapter.createFromResource(this, R.array.sub_rate, R.layout.spinner_item_layout);
        mSubRateAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mStreamResolutionAdapter = ArrayAdapter.createFromResource(this, R.array.stream_resolution, R.layout.spinner_item_layout);
        mStreamResolutionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mFileSizeAdapter = ArrayAdapter.createFromResource(this, R.array.file_size, R.layout.spinner_item_layout);
        mFileSizeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mDecodeNumberAdapter = ArrayAdapter.createFromResource(this, R.array.decode_number, R.layout.spinner_item_layout);
        mDecodeNumberAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mDecodeResolutionAdapter = ArrayAdapter.createFromResource(this, R.array.decode_resolution, R.layout.spinner_item_layout);
        mDecodeResolutionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mDecodeFormaAdapter = ArrayAdapter.createFromResource(this, R.array.decode_format, R.layout.spinner_item_layout);
        mDecodeFormaAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mEncodeFrameRateAdapter = ArrayAdapter.createFromResource(this, R.array.encode_frameRate, R.layout.spinner_item_layout);
        mEncodeFrameRateAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mDecodeFrameRateAdapter = ArrayAdapter.createFromResource(this, R.array.encode_frameRate, R.layout.spinner_item_layout);
        mDecodeFrameRateAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        mRecordNumberSpinner.setAdapter(mRecordNumberAdapter);
        mResolutionSpinner.setAdapter(mResolutionAdapter);
        mEncodeFormatSpinner.setAdapter(mEncodeFormaAdapter);
        mMainRateSpinner.setAdapter(mMainRateAdapter);
        mFileFormatSpinner.setAdapter(mFileFormatAdapter);
        mSubRateSpinner.setAdapter(mSubRateAdapter);
        mStreamResolutionSpinner.setAdapter(mStreamResolutionAdapter);
        mFileSizeSpinner.setAdapter(mFileSizeAdapter);

        mDecodeNumberSpinner.setAdapter(mDecodeNumberAdapter);
        mDecodeResolutionSpinner.setAdapter(mDecodeResolutionAdapter);
        mDecodeFormatSpinner.setAdapter(mDecodeFormaAdapter);
        mEncodeFrameRateSpinner.setAdapter(mEncodeFrameRateAdapter);
        mDecodeFrameRateSpinner.setAdapter(mDecodeFrameRateAdapter);

        mRecordNumberSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mResolutionSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mEncodeFormatSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mMainRateSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mFileFormatSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mSubRateSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mStreamResolutionSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mFileSizeSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());

        mDecodeNumberSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mDecodeResolutionSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mDecodeFormatSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mEncodeFrameRateSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());
        mDecodeFrameRateSpinner.setOnItemSelectedListener(new MySpinnerSelectedListener());


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mHandler) {
            mHandler.removeMessages(MSG_DISABLE_START);
            mHandler.removeMessages(MSG_ENABLE_START);
            mHandler.removeMessages(MSG_ENABLE_TIMEOUT);
        }
    }

    /**
     * Start encoding with selected resolution and recording number
     */
    private void startEncoding(int recordNum, int resolution, int frameRate) {
        Log.d(TAG, "startRecording||recordNum =" + recordNum + "||resolution =" + resolution);
        if (recordNum == 0) {
            Log.d(TAG, "encoding not selected");
            return;
        }
        int resol;
        switch (resolution) {
            case 0:
                resol = ENCODING_FILE_NAME[0];
                break;
            case 1:
                resol = ENCODING_FILE_NAME[1];
                break;
            case 2:
                resol = ENCODING_FILE_NAME[2];
                break;
            case 3:
                resol = ENCODING_FILE_NAME[3];
                break;
            case 4:
                resol = ENCODING_FILE_NAME[4];
                break;
            default:
                resol = ENCODING_FILE_NAME[0];
                break;
        }
        for (int i = 0; i < recordNum; i++) {
            EncodeUtil mEncodeThread = new EncodeUtil(MainActivity.this, i);
            mEncodeThread.setFrameRate(frameRate);
            mEncodeThread.setInputFile(resol);
            mEncodeThread.setMimeType("video/avc");
            //mEncodeThread.setMineType("video/mp4v-es");
//            mEncodeThread.setMimeType("video/hevc");
            mEncodeThread.setOutFileName("out_" + i + ".h264");
            //mEncodeThread.setOutFileName("out_" + i + ".es");
            //mEncodeThread.setOutFileName("out_" + i + ".h265");
            new Thread(mEncodeThread).start();
            mEncodeThread.setOnEncodingEnd(new IOnEncodingEnd() {
                @Override
                public void OnEncodingEnd(String fps) {
                    Log.d(TAG, "OnEncodingEnd||fps =" + fps);
                    if (TextUtils.equals(fps, OPEN_FILE_EXCEPTION)) {
                        mHandler.obtainMessage(MSG_ENABLE_TIMEOUT).sendToTarget();
                        if (mHandler.hasMessages(MSG_ENABLE_START)) {
                            mHandler.removeMessages(MSG_ENABLE_START);
                        }
                        return;
                    }
                    fpsEncodeList.add(fps);
                    mHandler.obtainMessage(MSG_ENCDOE_DONE).sendToTarget();
                    if (mHandler.hasMessages(MSG_ENABLE_TIMEOUT)) {
                        mHandler.removeMessages(MSG_ENABLE_TIMEOUT);
                    }
                }
            });
            encodeUtils.add(mEncodeThread);
        }
        //1080p编码
        /*int  encodeNum= 1;
        for (int j = 0; j < encodeNum; j++) {
            resol = ENCODING_FILE_NAME[1];
            EncodeUtil mEncodeThread = new EncodeUtil(MainActivity.this,j);
            mEncodeThread.setInputFile(resol);
            //mEncodeThread.setMineType("video/avc");
            //mEncodeThread.setMineType("video/mp4v-es");
            mEncodeThread.setMineType("video/hevc");
            //mEncodeThread.setOutFileName("out_" + ".h264");
            //mEncodeThread.setOutFileName("out_" + i + ".es");
            mEncodeThread.setOutFileName("out_" + j + ".h265");
            new Thread(mEncodeThread).start();
            mEncodeThread.setOnEncodingEnd(new IOnEncodingEnd() {
                @Override
                public void OnEncodingEnd(String fps) {
                    Log.d(TAG, "OnEncodingEnd||fps =" + fps);
                    if (TextUtils.equals(fps, OPEN_FILE_EXCEPTION)) {
                        mHandler.obtainMessage(MSG_ENABLE_TIMEOUT).sendToTarget();
                        if (mHandler.hasMessages(MSG_ENABLE_START)) {
                            mHandler.removeMessages(MSG_ENABLE_START);
                        }
                        return;
                    }
                    fpsEncodeList.add(fps);
                    mHandler.obtainMessage(MSG_ENCDOE_DONE).sendToTarget();
                    if (mHandler.hasMessages(MSG_ENABLE_TIMEOUT)) {
                        mHandler.removeMessages(MSG_ENABLE_TIMEOUT);
                    }
                }
            });
            encodeUtils.add(mEncodeThread);
        }*/
    }

    @Override
    public void openContextMenu(View view) {
        super.openContextMenu(view);
    }

    /**
     * Start decoding with selected resolution and decoding number
     */
    private void startDecoding(int decodeNum, int resolution, int frameRate) {

        Log.d(TAG, "startDecoding||decodeNum =" + decodeNum + "resolution =" + resolution);
        if (decodeNum == 0) {
            Log.d(TAG, "nothing selected");
            return;
        }

        int resol = 0;
        switch (resolution) {
            case 0:
                resol = DECODING_FILE_NAME[0];
                break;
            case 1:
                resol = DECODING_FILE_NAME[1];
                break;
            case 2:
                resol = DECODING_FILE_NAME[2];
                break;
            case 3:
                resol = DECODING_FILE_NAME[3];
                break;
            default:
                break;
        }
        Log.d(TAG, "startDecoding||resol =" + resol);
        for (int i = 0; i < decodeNum; i++) {
            Log.d(TAG, "Les decodeNum =" + decodeNum + ", i = " + i);
            DecodeUtil mDecodeUtil = new DecodeUtil(MainActivity.this);
            mDecodeUtil.setInputFile(resol);
            mDecodeUtil.setFrameRate(frameRate);
            mDecodeUtil.setOutputFile("uhd" + i + ".yuv");
            new Thread(mDecodeUtil).start();
            mDecodeUtil.setOnDecodingEnd(new DecodeUtil.IOnDecodingEnd() {
                @Override
                public void OnDecodingEnd(String fps) {
                    if (TextUtils.equals(fps, OPEN_FILE_EXCEPTION)) {
                        mHandler.obtainMessage(MSG_ENABLE_TIMEOUT).sendToTarget();
                        if (mHandler.hasMessages(MSG_DECODE_DONE)) {
                            mHandler.removeMessages(MSG_DECODE_DONE);
                        }
                        return;
                    }
                    fpsDecodeList.add(fps);
                    mHandler.obtainMessage(MSG_DECODE_DONE).sendToTarget();
                    if (mHandler.hasMessages(MSG_ENABLE_TIMEOUT)) {
                        mHandler.removeMessages(MSG_ENABLE_TIMEOUT);
                    }
                }
            });
        }
    }

    public void stopEncoding() {
        for (EncodeUtil e : encodeUtils) {
            e.stopEncode();
        }
    }

    //Update start button state based on decode&encode parameters,set disabled when nothing selected
    private void updateStartState() {
        mStartButton.setEnabled((recordNum + decodingNum) > 0);
    }


    class MySpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "onItemSelected||position =" + position);
            switch (parent.getId()) {
                case R.id.record_num_spinner:
                    if (position == 0) {
                        recordNum = 0;
                    } else {
                        recordNum = Integer.parseInt((String) (parent.getSelectedItem()));
                    }
                    updateStartState();
                    break;
                case R.id.record_resolution_spinner:
                    resolutionID = position;
                    break;
                case R.id.decode_num_spinner:
                    if (position == 0) {
                        decodingNum = 0;
                    } else {
                        decodingNum = Integer.parseInt((String) (parent.getSelectedItem()));
                    }
                    updateStartState();
                    break;
                case R.id.decode_resolution_spinner:
                    decodingResolutionID = position;
                    break;
                case R.id.framerate_spinner:
                    encodeFrameRateNum = Integer.parseInt((String) (parent.getSelectedItem()));
                    break;
                case R.id.decode_framerate_spinner:
                    decodeFrameRateNum = Integer.parseInt((String) (parent.getSelectedItem()));
                    break;
                case R.id.decode_format_spinner:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    class MyCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Log.d(TAG, "onCheckedChanged||buttonView =" + buttonView.getId() + "||isChecked =" + isChecked);
            switch (buttonView.getId()) {
                case R.id.stream_switch:
                    //todo
                    break;
                case R.id.audio_switch:
                    //todo
                    break;
                default:
                    break;
            }
        }
    }

    class MyClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.start_bt:
//                    if (mStartButton.getText().equals("START")) {
                    List<MediaCodecItem> list = adapter.getList();
                    if (list.size() <= 0) {
                        Log.d(TAG, "list.size < 0 ");
                        return;
                    }
                    mStartButton.setText("STOP");
                    fpsEncodeList.clear();
                    fpsDecodeList.clear();
                    add.setEnabled(false);
                    delete.setEnabled(false);
                    Log.d(TAG, "start_bt clicked");
                    mHandler.obtainMessage(MSG_DISABLE_START).sendToTarget();
                    mHandler.sendEmptyMessageDelayed(MSG_ENABLE_TIMEOUT, ENABLE_TIMEOUT);
                    for (MediaCodecItem item : list) {
                        if (item.isEncode()) {
                            startEncoding(item.getCodecNum(), item.getCodecPix(), item.getCodecFps());
                        } else {
                            startDecoding(item.getCodecNum(), item.getCodecPix(), item.getCodecFps());
                        }
                    }
                    mHandler.obtainMessage(MSG_GET_CPU_RATE).sendToTarget();
//                    startEncoding(recordNum, resolutionID, encodeFrameRateNum);
//                        startDecoding(decodingNum, decodingResolutionID,decodeFrameRateNum);
//                        if (recordNum > 0 || decodingNum > 0) {
//                            mHandler.obtainMessage(MSG_GET_CPU_RATE).sendToTarget();
//                        }
//                    } else {
//                        mStartButton.setText("START");
//                        stopEncoding();
//
//                    }
                    break;
                default:
                    break;
            }
        }
    }
}
