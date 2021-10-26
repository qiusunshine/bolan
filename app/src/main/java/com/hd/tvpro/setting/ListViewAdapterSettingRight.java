package com.hd.tvpro.setting;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.hd.tvpro.R;
import com.hd.tvpro.constants.AppConfig;

import java.util.ArrayList;

public class ListViewAdapterSettingRight extends BaseAdapter {
    private Context mContext;
    private ArrayList<String> list;
    private int defaultPosition = -1;

    public ListViewAdapterSettingRight(Context mContext, ArrayList<String> list, int selection) {
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
        defaultPosition = pos;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list_setting_right, parent, false);
            viewHolder.txt_item = (TextView) convertView.findViewById(R.id.txt_item_setting_right);
            viewHolder.txt_selected = (TextView) convertView.findViewById(R.id.txt_item_setting_selected);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.txt_item.setText((String) getItem(position));
        viewHolder.txt_item.setTextSize(TypedValue.COMPLEX_UNIT_PX, AppConfig.INSTANCE.getFontSize());
        viewHolder.txt_selected.setTextSize(TypedValue.COMPLEX_UNIT_PX, AppConfig.INSTANCE.getFontSize());
        if (defaultPosition == position) {
            viewHolder.txt_selected.setText("");
            Drawable drawble = ContextCompat.getDrawable(mContext, R.drawable.ic_check_green);
            viewHolder.txt_selected.setCompoundDrawablesWithIntrinsicBounds(null, null, drawble, null);
            viewHolder.txt_selected.setPressed(true);
            viewHolder.txt_item.setPressed(true);
            viewHolder.txt_selected.setActivated(true);
            viewHolder.txt_item.setActivated(true);
        } else {
            viewHolder.txt_selected.setText("");
            viewHolder.txt_selected.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            viewHolder.txt_selected.setPressed(false);
            viewHolder.txt_item.setPressed(false);
            viewHolder.txt_selected.setActivated(false);
            viewHolder.txt_item.setActivated(false);
        }
        viewHolder.txt_item.setTextColor(Color.WHITE);
        viewHolder.txt_item.setPaintFlags(viewHolder.txt_item.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        viewHolder.txt_item.getPaint().setAntiAlias(true);
        LayoutParams lp = viewHolder.txt_item.getLayoutParams();
        lp.height = AppConfig.INSTANCE.getSettingHeight();
        viewHolder.txt_item.setLayoutParams(lp);
        return convertView;
    }

    class ViewHolder {
        TextView txt_selected;
        TextView txt_item;
    }

}
