package com.pngcui.skyworth.dlna.center;

import android.content.Context;

import com.pngcui.skyworth.dlna.jni.PlatinumReflection;
import com.pngcui.skyworth.dlna.util.CommonLog;
import com.pngcui.skyworth.dlna.util.CommonUtil;
import com.pngcui.skyworth.dlna.util.DlnaUtils;
import com.pngcui.skyworth.dlna.util.LogFactory;

import org.greenrobot.eventbus.EventBus;

public class DMRCenter implements PlatinumReflection.ActionReflectionListener, IDMRAction {

    private static final CommonLog log = LogFactory.createLog();

    private Context mContext;

    public DMRCenter(Context context) {
        mContext = context;
    }

    @Override
    public synchronized void onActionInvoke(int cmd, String value, String data) {

        switch (cmd) {
            case PlatinumReflection.MEDIA_RENDER_CTL_MSG_SET_AV_URL:
                onRenderAvTransport(value, data);
                break;
            case PlatinumReflection.MEDIA_RENDER_CTL_MSG_PLAY:
                onRenderPlay(value, data);
                break;
            case PlatinumReflection.MEDIA_RENDER_CTL_MSG_PAUSE:
                onRenderPause(value, data);
                break;
            case PlatinumReflection.MEDIA_RENDER_CTL_MSG_STOP:
                onRenderStop(value, data);
                break;
            case PlatinumReflection.MEDIA_RENDER_CTL_MSG_SEEK:
                onRenderSeek(value, data);
                break;
            case PlatinumReflection.MEDIA_RENDER_CTL_MSG_SETMUTE:
                onRenderSetMute(value, data);
                break;
            case PlatinumReflection.MEDIA_RENDER_CTL_MSG_SETVOLUME:
                onRenderSetVolume(value, data);
                break;
            default:
                log.e("unrognized cmd!!!");
                break;
        }
    }

    @Override
    public void onRenderAvTransport(String value, String data) {
        if (data == null) {
            log.e("meteData = null!!!");
            return;
        }

        if (value == null || value.length() < 2) {
            log.e("url = " + value + ", it's invalid...");
            return;
        }

        DlnaMediaModel mediaInfo = DlnaMediaModelFactory.createFromMetaData(data);
        mediaInfo.setUrl(value);
        EventBus.getDefault().post(mediaInfo);
    }

    @Override
    public void onRenderPlay(String value, String data) {
        MediaControlBrocastFactory.sendPlayBrocast(mContext);
    }

    @Override
    public void onRenderPause(String value, String data) {
        MediaControlBrocastFactory.sendPauseBrocast(mContext);
    }

    @Override
    public void onRenderStop(String value, String data) {
        MediaControlBrocastFactory.sendStopBorocast(mContext);
    }

    @Override
    public void onRenderSeek(String value, String data) {
        int seekPos = 0;
        try {
            seekPos = DlnaUtils.parseSeekTime(value);
            MediaControlBrocastFactory.sendSeekBrocast(mContext, seekPos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRenderSetMute(String value, String data) {
        if ("1".equals(value)) {
            CommonUtil.setVolumeMute(mContext);
        } else if ("0".equals(value)) {
            CommonUtil.setVolumeUnmute(mContext);
        }
    }

    @Override
    public void onRenderSetVolume(String value, String data) {
        try {
            int volume = Integer.parseInt(value);
            if (volume < 101) {
                CommonUtil.setCurrentVolume(volume, mContext);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
