package com.hd.tvpro.util.http;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.hd.tvpro.util.StringUtil;
import com.hd.tvpro.util.ToastMgr;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 作者：By hdy
 * 日期：On 2018/11/1
 * 时间：At 19:17
 */
public class LocalServerParser {
    private static final String TAG = "LocalServerParser";
    private static String realUrl;

    public static String getUrlForPos(Context context, String url) {
        if (StringUtil.isEmpty(url)) {
            return url;
        }
        String[] s = url.split(":");
        if (s.length > 2 && StringUtil.isNotEmpty(realUrl) && s[2].startsWith("11111/")) {
            return realUrl;
        }
        return url;
    }

    public static String getIP(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            //判断wifi是否开启
            WifiInfo wifiInfo = null;
            if (wifiManager != null) {
                wifiInfo = wifiManager.getConnectionInfo();
            }
            int ipAddress = 0;
            if (wifiInfo != null) {
                ipAddress = wifiInfo.getIpAddress();
            }
            return intToIp(ipAddress);
        } catch (Exception e) {
            e.printStackTrace();
            ToastMgr.shortBottomCenter(context, "出错：" + e.toString());
            try {
                return getLocalIPAddress();
            } catch (Exception e1) {
                e1.printStackTrace();
                ToastMgr.shortBottomCenter(context, "出错：" + e.toString());
            }
        }
        return "127.0.0.1";
    }

    private static String getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> mEnumeration = NetworkInterface
                    .getNetworkInterfaces(); mEnumeration.hasMoreElements(); ) {
                NetworkInterface intf = mEnumeration.nextElement();
                for (Enumeration<InetAddress> enumIPAddr = intf
                        .getInetAddresses(); enumIPAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIPAddr.nextElement();
                    // 如果不是回环地址
                    if (!inetAddress.isLoopbackAddress()) {
                        // 直接返回本地IP地址
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            System.err.print("error");
        }
        return "127.0.0.1";
    }

    public static InetAddress getLocalINetAddress() throws SocketException {
        for (Enumeration<NetworkInterface> mEnumeration = NetworkInterface
                .getNetworkInterfaces(); mEnumeration.hasMoreElements(); ) {
            NetworkInterface intf = mEnumeration.nextElement();
            for (Enumeration<InetAddress> enumIPAddr = intf
                    .getInetAddresses(); enumIPAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIPAddr.nextElement();
                // 如果不是回环地址
                if (!inetAddress.isLoopbackAddress()) {
                    // 直接返回本地IP地址
                    return inetAddress;
                }
            }
        }
        throw new SocketException("获取本地IP地址失败！");
    }

    private static String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

}
