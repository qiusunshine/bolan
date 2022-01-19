package com.hd.tvpro.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ListView;

public class ChannelListView extends ListView {
    public int pos = 0;
    private int y;

    public ChannelListView(Context context) {
        super(context);
    }

    public ChannelListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ChannelListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSelect(int position, int y) {
        super.setSelection(position);
        pos = position;
        this.y = y;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
                                  Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (gainFocus) {
            setSelectionFromTop(pos, y);
        }
    }
}
