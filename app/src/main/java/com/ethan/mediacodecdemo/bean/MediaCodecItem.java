package com.ethan.mediacodecdemo.bean;

public class MediaCodecItem {
    private boolean isEncode = true;
    private int codecNum = 1;
    private int codecPix = 0;
    private int codecFps = 30;

    public boolean isEncode() {
        return isEncode;
    }

    public void setEncode(boolean encode) {
        isEncode = encode;
    }

    public int getCodecNum() {
        return codecNum;
    }

    public void setCodecNum(int codecNum) {
        this.codecNum = codecNum;
    }

    public int getCodecPix() {
        return codecPix;
    }

    public void setCodecPix(int codecPix) {
        this.codecPix = codecPix;
    }

    public int getCodecFps() {
        return codecFps;
    }

    public void setCodecFps(int codecFps) {
        this.codecFps = codecFps;
    }
}
