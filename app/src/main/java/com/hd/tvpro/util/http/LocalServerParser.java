package com.hd.tvpro.util.http;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.hd.tvpro.util.StringUtil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

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
    /**
     * Ipv4 address check.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(" + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                    "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");

    /**
     * Check if valid IPV4 address.
     *
     * @param input the address string to check for validity.
     *
     * @return True if the input parameter is a valid IPv4 address.
     */
    public static boolean isIPv4Address(String input) {
        return IPV4_PATTERN.matcher(input).matches();
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
            String ip = intToIp(ipAddress);
            if (ip.length() <= 0 || ip.startsWith("0.")) {
                try {
                    return getLocalIPAddress();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                return ip;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                return getLocalIPAddress();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return "127.0.0.1";
    }

    private static String getLocalIPAddress() {
        try {
            String eth0 = getLocalEth0Ip();
            if (eth0 != null && eth0.length() > 0 && !eth0.startsWith("0.")) {
                //优先取有线网卡的IP
                return eth0;
            }
            List<String> ipList = new ArrayList<>();
            for (Enumeration<NetworkInterface> mEnumeration = NetworkInterface
                    .getNetworkInterfaces(); mEnumeration.hasMoreElements(); ) {
                NetworkInterface intf = mEnumeration.nextElement();
                for (Enumeration<InetAddress> enumIPAddr = intf
                        .getInetAddresses(); enumIPAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIPAddr.nextElement();
                    // 如果不是回环地址
                    if (!inetAddress.isLoopbackAddress() && isIPv4Address(inetAddress.getHostAddress())) {
                        // 直接返回本地IP地址
                        ipList.add(inetAddress.getHostAddress());
                    }
                }
            }
            if (!ipList.isEmpty()) {
                List<String> ip2List = new ArrayList<>();
                for (String s : ipList) {
                    if (!s.startsWith("0.")) {
                        ip2List.add(s);
                    }
                }
                if (ip2List.isEmpty()) {
                    //只有0.0.0.0
                    return ipList.get(0);
                } else {
                    //取非0.0.0.0
                    return ip2List.get(0);
                }
            }
        } catch (SocketException ex) {
            System.err.print("error");
        }
        return "127.0.0.1";
    }


    /**
     * 得到有限网关的IP地址
     *
     * @return
     */
    private static String getLocalEth0Ip() {
        try {
            // 获取本地设备的所有网络接口
            Enumeration<NetworkInterface> enumerationNi = NetworkInterface
                    .getNetworkInterfaces();
            while (enumerationNi.hasMoreElements()) {
                NetworkInterface networkInterface = enumerationNi.nextElement();
                String interfaceName = networkInterface.getDisplayName();
                // 如果是有限网卡
                if (interfaceName.equals("eth0")) {
                    Enumeration<InetAddress> enumIpAddr = networkInterface
                            .getInetAddresses();
                    while (enumIpAddr.hasMoreElements()) {
                        // 返回枚举集合中的下一个IP地址信息
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        // 不是回环地址，并且是ipv4的地址
                        if (!inetAddress.isLoopbackAddress()
                                && isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
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
