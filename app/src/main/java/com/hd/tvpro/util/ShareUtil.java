package com.hd.tvpro.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * 作者：By hdy
 * 日期：On 2017/10/24
 * 时间：At 19:44
 */

public class ShareUtil {

    public static void shareText(Context context, String str) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        if (str != null) {
            intent.putExtra(Intent.EXTRA_TEXT, str);
        } else {
            intent.putExtra(Intent.EXTRA_TEXT, "");
        }
        intent.setType("text/plain");
        try {
            context.startActivity(Intent.createChooser(intent, "分享"));
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "系统故障：" + e.getMessage());
        }
    }

    public static void startUrl(Context context, String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(intent);
        } catch (Exception e) {
            ToastMgr.shortBottomCenter(context, "打开失败！");
        }
    }
}
