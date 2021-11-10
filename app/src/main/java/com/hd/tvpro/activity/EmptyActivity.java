package com.hd.tvpro.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * 作者：By hdy
 * 日期：On 2018/11/16
 * 时间：At 10:51
 */

public class EmptyActivity extends Activity {

    @Override
    protected void onNewIntent(Intent intent) {
        finish();
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
}