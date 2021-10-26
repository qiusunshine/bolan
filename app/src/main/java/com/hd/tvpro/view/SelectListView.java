package com.hd.tvpro.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ListView;

public class SelectListView extends ListView {
    private DataChangedListener dataChangedListener;
    public int pos = 0;
    private int y;

    public SelectListView(Context context) {
        super(context);
    }

    public SelectListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SelectListView(Context context, AttributeSet attrs, int defStyleAttr) {
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


    @Override
    protected void handleDataChanged() {
        super.handleDataChanged();
        if (dataChangedListener != null) dataChangedListener.onSuccess();
    }

    public void setDataChangedListener(DataChangedListener dataChangedListener) {
        this.dataChangedListener = dataChangedListener;
    }

    public interface DataChangedListener {
        public void onSuccess();
    }
}
