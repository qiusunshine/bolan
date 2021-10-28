package com.pngcui.skyworth.dlna.jni;

public class PlatinumJniProxy {

    static {
        System.loadLibrary("git-platinum");
    }

    public static native int startDlnaMediaRender(byte[] friendname, byte[] uuid);

    public static native void stopDlnaMediaRender();

    public static native boolean responseGenaEvent(int cmd, byte[] value, byte[] data);

    public static native boolean enableLogPrint(boolean flag);


    public static int startMediaRender(String friendname, String uuid) {
        int ret = -1;
        try {
            if (friendname == null) friendname = "";
            if (uuid == null) uuid = "";
            ret = startDlnaMediaRender(friendname.getBytes("utf-8"), uuid.getBytes("utf-8"));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static boolean responseGenaEvent(int cmd, String value, String data) {
        try {
            if (value == null) value = "";
            if (data == null) data = "";
            boolean ret = false;
            ret = responseGenaEvent(cmd, value.getBytes("utf-8"), data.getBytes("utf-8"));
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
