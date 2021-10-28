package com.pngcui.skyworth.dlna.center;

import android.content.Context;
import android.content.Intent;

import com.pngcui.skyworth.dlna.event.EngineCommand;
import com.pngcui.skyworth.dlna.util.CommonLog;
import com.hd.tvpro.app.App;
import com.pngcui.skyworth.dlna.service.MediaRenderService;
import com.pngcui.skyworth.dlna.util.LogFactory;

import org.greenrobot.eventbus.EventBus;


public class MediaRenderProxy implements IBaseEngine {

    private static final CommonLog log = LogFactory.createLog();

    private static MediaRenderProxy mInstance;
    private Context mContext;

    private MediaRenderProxy(Context context) {
        mContext = context;
    }

    public static synchronized MediaRenderProxy getInstance() {
        if (mInstance == null) {
            mInstance = new MediaRenderProxy(App.INSTANCE);
        }
        return mInstance;
    }

    @Override
    public boolean startEngine() {
        EventBus.getDefault().post(new EngineCommand(MediaRenderService.START_RENDER_ENGINE));
        return true;
    }

    @Override
    public boolean stopEngine() {
        mContext.stopService(new Intent(mContext, MediaRenderService.class));
        return true;
    }

    @Override
    public boolean restartEngine() {
        EventBus.getDefault().post(new EngineCommand(MediaRenderService.RESTART_RENDER_ENGINE));
        return true;
    }

}
