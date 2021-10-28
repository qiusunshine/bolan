package com.pngcui.skyworth.dlna.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.pngcui.skyworth.dlna.center.DLNAGenaEventBrocastFactory;
import com.pngcui.skyworth.dlna.center.DMRCenter;
import com.pngcui.skyworth.dlna.center.DMRWorkThread;
import com.pngcui.skyworth.dlna.center.IBaseEngine;
import com.pngcui.skyworth.dlna.event.EngineCommand;
import com.pngcui.skyworth.dlna.util.CommonLog;
import com.pngcui.skyworth.dlna.util.DlnaUtils;
import com.pngcui.skyworth.dlna.util.LogFactory;
import com.hd.tvpro.R;
import com.hd.tvpro.app.App;
import com.pngcui.skyworth.dlna.jni.PlatinumReflection;
import com.pngcui.skyworth.dlna.util.CommonUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;


public class MediaRenderService extends Service implements IBaseEngine {

    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final CommonLog log = LogFactory.createLog();

    public static final String START_RENDER_ENGINE = "com.geniusgithub.start.engine";
    public static final String RESTART_RENDER_ENGINE = "com.geniusgithub.restart.engine";


    private DMRWorkThread mWorkThread;

    private PlatinumReflection.ActionReflectionListener mListener;
    private DLNAGenaEventBrocastFactory mMediaGenaBrocastFactory;

    private Handler mHandler;
    private static final int START_ENGINE_MSG_ID = 0x0001;
    private static final int RESTART_ENGINE_MSG_ID = 0x0002;

    private static final int DELAY_TIME = 1000;

    private WifiManager.MulticastLock mMulticastLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
            Notification notification = new NotificationCompat.Builder(App.INSTANCE, channelId)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setContentText("DLAN投屏")
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .build();
            startForeground(ONGOING_NOTIFICATION_ID, notification);
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        initRenderService();
        onCommand(new EngineCommand(MediaRenderService.START_RENDER_ENGINE));
        log.e("MediaRenderService onCreate");
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelId = getResources().getString(R.string.app_name);
        String channelName = "DLAN投屏";
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setImportance(NotificationManager.IMPORTANCE_NONE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) {
            service.createNotificationChannel(chan);
        }
        return channelId;
    }

    @Override
    public void onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        unInitRenderService();
        log.e("MediaRenderService onDestroy");
        super.onDestroy();
    }

    @Subscribe
    public void onCommand(EngineCommand command) {
        String actionString = command.getCommand();
        if (actionString.equalsIgnoreCase(START_RENDER_ENGINE)) {
            delayToSendStartMsg();
        } else if (actionString.equalsIgnoreCase(RESTART_RENDER_ENGINE)) {
            delayToSendRestartMsg();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String actionString = intent.getAction();
            if (actionString != null) {
                if (actionString.equalsIgnoreCase(START_RENDER_ENGINE)) {
                    delayToSendStartMsg();
                } else if (actionString.equalsIgnoreCase(RESTART_RENDER_ENGINE)) {
                    delayToSendRestartMsg();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    private void initRenderService() {
        mListener = new DMRCenter(this);
        PlatinumReflection.setActionInvokeListener(mListener);
        mMediaGenaBrocastFactory = new DLNAGenaEventBrocastFactory(this);
        mMediaGenaBrocastFactory.registerBrocast();
        mWorkThread = new DMRWorkThread(this);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case START_ENGINE_MSG_ID:
                        startEngine();
                        break;
                    case RESTART_ENGINE_MSG_ID:
                        restartEngine();
                        break;
                }
            }

        };

        mMulticastLock = CommonUtil.openWifiBrocast(this);
        log.e("openWifiBrocast = " + mMulticastLock != null ? true : false);
    }


    private void unInitRenderService() {
        stopEngine();
        removeStartMsg();
        removeRestartMsg();
        mMediaGenaBrocastFactory.unRegisterBrocast();
        if (mMulticastLock != null) {
            mMulticastLock.release();
            mMulticastLock = null;
            log.e("closeWifiBrocast");
        }
    }

    private void delayToSendStartMsg() {
        removeStartMsg();
        mHandler.sendEmptyMessageDelayed(START_ENGINE_MSG_ID, DELAY_TIME);
    }

    private void delayToSendRestartMsg() {
        removeStartMsg();
        removeRestartMsg();
        mHandler.sendEmptyMessageDelayed(RESTART_ENGINE_MSG_ID, DELAY_TIME);
    }

    private void removeStartMsg() {
        mHandler.removeMessages(START_ENGINE_MSG_ID);
    }

    private void removeRestartMsg() {
        mHandler.removeMessages(RESTART_ENGINE_MSG_ID);
    }


    @Override
    public boolean startEngine() {
        awakeWorkThread();
        return true;
    }

    @Override
    public boolean stopEngine() {
        mWorkThread.setParam("", "");
        exitWorkThread();
        return true;
    }

    @Override
    public boolean restartEngine() {
        String friendName = DlnaUtils.getDevName(this);
        String uuid = DlnaUtils.creat12BitUUID(this);
        mWorkThread.setParam(friendName, uuid);
        if (mWorkThread.isAlive()) {
            mWorkThread.restartEngine();
        } else {
            mWorkThread.start();
        }
        return true;
    }

    private void awakeWorkThread() {
        String friendName = DlnaUtils.getDevName(this);
        String uuid = DlnaUtils.creat12BitUUID(this);
        mWorkThread.setParam(friendName, uuid);


        if (mWorkThread.isAlive()) {
            mWorkThread.awakeThread();
        } else {
            mWorkThread.start();
        }
    }

    private void exitWorkThread() {
        if (mWorkThread != null && mWorkThread.isAlive()) {
            mWorkThread.exit();
            long time1 = System.currentTimeMillis();
            while (mWorkThread.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long time2 = System.currentTimeMillis();
            log.e("exitWorkThread cost time:" + (time2 - time1));
            mWorkThread = null;
        }
    }
}
