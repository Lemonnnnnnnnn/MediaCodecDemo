package com.ethan.mediacodecdemo.util;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;


import com.ethan.mediacodecdemo.R;
import com.ethan.mediacodecdemo.bean.MediaCodecItem;

import java.util.ArrayList;
import java.util.List;

public class MediacodecItemAdapter extends BaseAdapter {
    private static final String TAG = MediacodecItemAdapter.class.getSimpleName();
    private List<MediaCodecItem> list = new ArrayList<>();
    private boolean isEncode = true;
    private int codecNum = 0;
    private int codecPix = 0;
    private int codecFps = 0;
    private final Context context;

    public MediacodecItemAdapter(Context context,List<MediaCodecItem> list) {
        this.context = context;
        this.list = list;
    }

    public void addData(){
        MediaCodecItem item = new MediaCodecItem();
        list.add(item);
        notifyDataSetChanged();
    }

    public void deleteData(){
        if (list.size() <= 1){
            return;
        }
        list.remove(list.size()-1);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    public List<MediaCodecItem> getList(){
        return list;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.mediacodec_items, null);
            holder = new ViewHolder();
            holder.spinnerFps = convertView.findViewById(R.id.framerate_spinner);
            holder.spinnerNum = convertView.findViewById(R.id.record_num_spinner);
            holder.spinnerType = convertView.findViewById(R.id.type_spinner);
            holder.spinnerPix = convertView.findViewById(R.id.record_resolution_spinner);
            holder.parent = convertView.findViewById(R.id.parent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        ArrayAdapter<CharSequence> mCodecTypeAdapter = ArrayAdapter.createFromResource(context, R.array.codec_type, R.layout.spinner_item_layout);
        mCodecTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        ArrayAdapter<CharSequence> mRecordNumberAdapter = ArrayAdapter.createFromResource(context, R.array.record_number, R.layout.spinner_item_layout);
        mRecordNumberAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        ArrayAdapter<CharSequence> mResolutionAdapter = ArrayAdapter.createFromResource(context, R.array.record_resolution, R.layout.spinner_item_layout);
        mResolutionAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        ArrayAdapter<CharSequence> mEncodeFrameRateAdapter = ArrayAdapter.createFromResource(context, R.array.encode_frameRate, R.layout.spinner_item_layout);
        mEncodeFrameRateAdapter.setDropDownViewResource(R.layout.spinner_dropdown_style);

        holder.spinnerFps.setAdapter(mEncodeFrameRateAdapter);
        holder.spinnerNum.setAdapter(mRecordNumberAdapter);
        holder.spinnerType.setAdapter(mCodecTypeAdapter);
        holder.spinnerPix.setAdapter(mResolutionAdapter);

        holder.spinnerFps.setSelection(mEncodeFrameRateAdapter.getPosition(String.valueOf(list.get(position).getCodecFps())));
        holder.spinnerPix.setSelection(list.get(position).getCodecPix());
        holder.spinnerNum.setSelection(mRecordNumberAdapter.getPosition(String.valueOf(list.get(position).getCodecNum())));
        if (list.get(position).isEncode()){
            holder.spinnerType.setSelection(mCodecTypeAdapter.getPosition("编码"));
        } else {
            holder.spinnerType.setSelection(mCodecTypeAdapter.getPosition("解码"));
        }
        if (position % 2 == 0){
            holder.parent.setBackgroundColor(Color.parseColor("#00FFFF"));
        }else {
            holder.parent.setBackgroundColor(Color.parseColor("#FFE4C4"));
        }

        holder.spinnerFps.setOnItemSelectedListener(new MySpinnerSelectedListener(position));
        holder.spinnerNum.setOnItemSelectedListener(new MySpinnerSelectedListener(position));
        holder.spinnerType.setOnItemSelectedListener(new MySpinnerSelectedListener(position));
        holder.spinnerPix.setOnItemSelectedListener(new MySpinnerSelectedListener(position));

        holder.parent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (list.size() <= 1){
                    return false;
                }
                list.remove(position);
                notifyDataSetChanged();
                Log.d(TAG,"删除 item" + position);
                return true;
            }
        });
        return convertView;
    }

    class MySpinnerSelectedListener implements AdapterView.OnItemSelectedListener{

        int dataPosition ;

        public MySpinnerSelectedListener(int dataPosition) {
            this.dataPosition = dataPosition;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            MediaCodecItem item = list.get(dataPosition);
            switch (parent.getId()) {
                case R.id.framerate_spinner:
                    item.setCodecFps(Integer.parseInt((String) (parent.getSelectedItem())));
                    break;
                case R.id.record_num_spinner:
                    if (position == 0) {
                        item.setCodecNum(0);
                    } else {
                        item.setCodecNum(Integer.parseInt((String) (parent.getSelectedItem())));
                    }
                    break;
                case R.id.type_spinner:
                    item.setEncode(position == 0);
                    break;
                case R.id.record_resolution_spinner:
                    item.setCodecPix(position);
                    break;
            }
            list.set(dataPosition,item);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    public interface onSelectItemCallBack {
        void itemSelect(List<MediaCodecItem> list);
    }

    static class ViewHolder {
        Spinner spinnerNum;
        Spinner spinnerType;
        Spinner spinnerFps;
        Spinner spinnerPix;
        LinearLayout parent;
    }
}
