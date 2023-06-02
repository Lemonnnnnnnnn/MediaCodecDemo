package com.ethan.mediacodecdemo.biz;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import com.ethan.mediacodecdemo.R;
import com.ethan.mediacodecdemo.util.PickUtils;
import com.ethan.mediacodecdemo.util.VideoPlayer;
import com.ethan.mediacodecdemo.widgt.AutoFitTextureView;

import java.io.IOException;

public class MediaExtractorActivity extends AppCompatActivity {
    private final String TAG = MediaExtractorActivity.class.getSimpleName();
    private String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final int REQUEST_FILE_CODE = 0x11;
    private TextView tvVideo,tvAudio,tvFileName;
    private VideoPlayer videoPlayer = new VideoPlayer();
    private AutoFitTextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_extractor);
        if (!hasPermissionsGranted(permission)) {
            requestPermissions(permission, 1);
        }
        textureView = findViewById(R.id.surface);
        tvAudio = findViewById(R.id.tv_audioTrack);
        tvVideo = findViewById(R.id.tv_videoTrack);
        tvFileName = findViewById(R.id.tv_filename);
    }

    public void OpenFile() {
        videoPlayer.stop();
        // 指定类型
        String[] mimeTypes = {"*/*"};
        // String[] mimeTypes = {"application/octet-stream"}; // 指定bin类型
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        StringBuilder mimeTypesStr = new StringBuilder();
        for (String mimeType : mimeTypes) {
            mimeTypesStr.append(mimeType).append("|");
        }
        intent.setType(mimeTypesStr.substring(0, mimeTypesStr.length() - 1));
        startActivityForResult(Intent.createChooser(intent, "ChooseFile"), REQUEST_FILE_CODE);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        videoPlayer.stop();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_FILE_CODE && data != null) {
            textureView.setVisibility(View.VISIBLE);
            Uri uri = data.getData();
            tvFileName.setText(PickUtils.getFileName(MediaExtractorActivity.this,uri));
            String path = PickUtils.getPath(MediaExtractorActivity.this, uri);
            if (path == null){
                printLog("path ==  null");
                return;
            }
            printLog("path == " + path);
            printMediaCount(path);
            printAudioMeidaInfo(path);
            printVideoMeidaInfo(path);
            videoPlayer.stat(path,new Surface(textureView.getSurfaceTexture()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (checkSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void printMediaCount(String path) {
        try {
            MediaExtractor extractor = new MediaExtractor();//实例一个MediaExtractor
            extractor.setDataSource(path);
            int count = extractor.getTrackCount();//获取轨道数量
            printLog("printMediaInfo count=" + count);
            for (int i = 0; i < count; i++) {
                MediaFormat mediaFormat = extractor.getTrackFormat(i);
                printLog(i + "编号通道格式 = " + mediaFormat.getString(MediaFormat.KEY_MIME));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getTrackIndex(String targetTrack, String path) {
        MediaExtractor extractor = new MediaExtractor();//实例一个MediaExtractor
        try {
            extractor.setDataSource(path);//设置添加MP4文件路径
        } catch (IOException e) {
            e.printStackTrace();
        }
        int trackIndex = -1;
        int count = extractor.getTrackCount();//获取轨道数量
        for (int i = 0; i < count; i++) {
            MediaFormat mediaFormat = extractor.getTrackFormat(i);
            String currentTrack = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (currentTrack.startsWith(targetTrack)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;

    }


    private void printVideoMeidaInfo(String path) {
        int index = getTrackIndex("video", path);
        if (index < 0) {
            textureView.setVisibility(View.GONE);
            return;
        }
        try {
            MediaExtractor extractor = new MediaExtractor();//实例一个MediaExtractor
            extractor.setDataSource(path);
            MediaFormat mediaFormat = extractor.getTrackFormat(index);
            int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            textureView.setAspectRatio(width,height);
            printLog("printVideoMeidaInfo " + mediaFormat.toString().replaceAll(",",",\n"));
            tvVideo.setText(mediaFormat.toString().replaceAll(",",",\n"));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void printAudioMeidaInfo(String path) {
        int index = getTrackIndex("audio", path);
        if (index < 0) {
            return;
        }
        try {
            MediaExtractor extractor = new MediaExtractor();//实例一个MediaExtractor
            extractor.setDataSource(path);
            MediaFormat mediaFormat = extractor.getTrackFormat(index);
            printLog("printAudioMeidaInfo " + mediaFormat.toString().replaceAll(",",",\n"));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvAudio.setText(mediaFormat.toString().replaceAll(",",",\n"));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void printLog(String log) {
        Log.d(TAG, log);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.select:
                OpenFile();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}