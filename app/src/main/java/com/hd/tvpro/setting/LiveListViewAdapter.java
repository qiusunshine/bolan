package com.hd.tvpro.setting;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hd.tvpro.R;
import com.hd.tvpro.constants.AppConfig;

import java.util.ArrayList;

public class LiveListViewAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<String> list;

    public void setSelectedPos(int selectedPos) {
        this.selectedPos = selectedPos;
    }

    private int selectedPos = 0;

    public int getSelectedPos() {
        return selectedPos;
    }

    public LiveListViewAdapter(Context mContext, ArrayList<String> list, int selection) {
        this.mContext = mContext;
        this.list = list;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list == null ? 0 : list.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public void setSelection(int pos) {
        selectedPos = pos;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list_left, parent, false);
            viewHolder.txt_item = convertView.findViewById(R.id.txt_item);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        String v = (String) getItem(position);
        viewHolder.txt_item.setText(v);
        viewHolder.txt_item.setTextSize(TypedValue.COMPLEX_UNIT_PX, AppConfig.INSTANCE.getFontSize());

        if (position == selectedPos) {
            viewHolder.txt_item.setPressed(true);
            viewHolder.txt_item.setActivated(true);
        } else {
            viewHolder.txt_item.setPressed(false);
            viewHolder.txt_item.setActivated(false);
            viewHolder.txt_item.setBackgroundColor(Color.TRANSPARENT);
        }

        LayoutParams lp = viewHolder.txt_item.getLayoutParams();
        lp.height = AppConfig.INSTANCE.getLiveHeight();
        viewHolder.txt_item.setLayoutParams(lp);
        return convertView;
    }

    class ViewHolder {
        TextView txt_item;
    }

}
