package chuangyuan.ycj.videolibrary.video;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.Util;

import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import chuangyuan.ycj.videolibrary.listener.DataSourceListener;
import chuangyuan.ycj.videolibrary.listener.ExoPlayerListener;
import chuangyuan.ycj.videolibrary.listener.ExoPlayerViewListener;
import chuangyuan.ycj.videolibrary.listener.ItemVideo;
import chuangyuan.ycj.videolibrary.listener.LoadModelType;
import chuangyuan.ycj.videolibrary.listener.VideoInfoListener;
import chuangyuan.ycj.videolibrary.listener.VideoWindowListener;
import chuangyuan.ycj.videolibrary.utils.VideoPlayUtils;
import chuangyuan.ycj.videolibrary.widget.VideoPlayerView;

/**
 * The type Exo user player.
 * author yangc   date 2017/2/28
 * E-Mail:1007181167@qq.com
 * Description?????????
 */
public class ExoUserPlayer {
    private static final String TAG = ExoUserPlayer.class.getName();
    /***????????????*/
    Activity activity;
    /*** ??????view??????***/
    protected VideoPlayerView videoPlayerView;
    /*** ??????????????????,????????????????????????,?????????????????? ***/
    private Long lastTotalRxBytes = 0L, lastTimeStamp = 0L, resumePosition = 0L;
    /*** ??????????????????  0 ?????????,??????????????????????????????***/
    private int resumeWindow = 0;
    /*** ??????????????????,???????????????????????????,,????????????,*/
    boolean handPause;
    boolean isPause;

    public boolean isLoad() {
        return isLoad;
    }

    boolean isLoad;
    /**
     * ????????????,????????????????????????
     **/
    private boolean isEnd, isSwitch;
    /*** ??????????????? ***/
    private ScheduledExecutorService timer;
    /*** ??????????????????***/
    private NetworkBroadcastReceiver mNetworkBroadcastReceiver;
    /*** view?????????????????? ***/
    private PlayComponentListener playComponentListener;
    /*** ???????????????????????? ***/
    private final CopyOnWriteArraySet<VideoInfoListener> videoInfoListeners;
    /*** ??????????????????***/
    private final CopyOnWriteArraySet<VideoWindowListener> videoWindowListeners;
    /*** ??????view???????????? ***/
    private ExoPlayerViewListener mPlayerViewListener;
    /*** ??????????????????*/
    SimpleExoPlayer player;
    /***??????????????????*/
    private MediaSourceBuilder mediaSourceBuilder;
    /*** ??????????????????***/
    private PlaybackParameters playbackParameters;
    private View.OnClickListener onClickListener;
    private String playUrl;
    private SwitchListener switchListener;
    private int networkMode = ConnectivityManager.TYPE_WIFI;

    /****
     * @param activity ????????????
     * @param reId ????????????id
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public ExoUserPlayer(@NonNull Activity activity, @IdRes int reId) {
        this(activity, reId, null);
    }

    /****
     * ?????????
     * @param activity ????????????
     * @param playerView ????????????
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public ExoUserPlayer(@NonNull Activity activity, @NonNull VideoPlayerView playerView) {
        this(activity, playerView, null);
    }

    /****
     * ?????????
     * @param activity ????????????
     * @param reId ????????????id
     * @param listener ?????????????????????
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public ExoUserPlayer(@NonNull Activity activity, @IdRes int reId, @Nullable DataSourceListener listener) {
        this(activity, (VideoPlayerView) activity.findViewById(reId), listener);
    }

    /***
     * ?????????
     * @param activity ????????????
     * @param playerView ????????????
     * @param listener ?????????????????????
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public ExoUserPlayer(@NonNull Activity activity, @NonNull VideoPlayerView playerView, @Nullable DataSourceListener listener) {
        this.activity = activity;
        this.videoPlayerView = playerView;
        videoInfoListeners = new CopyOnWriteArraySet<>();
        videoWindowListeners = new CopyOnWriteArraySet<>();
        try {
            Class<?> clazz = Class.forName("chuangyuan.ycj.videolibrary.whole.WholeMediaSource");
            Constructor<?> constructor = clazz.getConstructor(Context.class, DataSourceListener.class);
            this.mediaSourceBuilder = (MediaSourceBuilder) constructor.newInstance(activity, listener);
        } catch (Exception e) {
            this.mediaSourceBuilder = new MediaSourceBuilder(activity, listener);
        } finally {
            initView();
        }
    }

    /****
     * ?????????
     * @param activity ????????????
     * @param mediaSourceBuilder ?????????????????????
     * @param playerView ????????????
     * @deprecated Use {@link VideoPlayerManager.Builder} instead.
     */
    public ExoUserPlayer(@NonNull Activity activity, @NonNull MediaSourceBuilder mediaSourceBuilder, @NonNull VideoPlayerView playerView) {
        this.activity = activity;
        this.videoPlayerView = playerView;
        this.mediaSourceBuilder = mediaSourceBuilder;
        videoInfoListeners = new CopyOnWriteArraySet<>();
        videoWindowListeners = new CopyOnWriteArraySet<>();
        initView();
    }


    private void initView() {
        playComponentListener = new PlayComponentListener();
        videoPlayerView.setExoPlayerListener(playComponentListener);
        getPlayerViewListener().setPlayerBtnOnTouch(true);
        player = createFullPlayer();
    }

    /*****
     * ??????????????????view  ??????????????????????????????????????????
     * @param videoPlayerView videoPlayerView
     * **/
    void setVideoPlayerView(@NonNull VideoPlayerView videoPlayerView) {
        mPlayerViewListener = null;
        if (player != null) {
            player.removeListener(componentListener);
        }
        this.videoPlayerView = videoPlayerView;
        videoPlayerView.setExoPlayerListener(playComponentListener);
        if (player == null) {
            player = createFullPlayer();
        }
        player.addListener(componentListener);
        getPlayerViewListener().hideController(false);
        getPlayerViewListener().setControllerHideOnTouch(true);
        isEnd = false;
        isLoad = true;
    }

    /***
     * ????????????view????????????
     * @return ExoPlayerViewListener player view listener
     */
    @NonNull
    ExoPlayerViewListener getPlayerViewListener() {
        if (mPlayerViewListener == null) {
            mPlayerViewListener = videoPlayerView.getComponentListener();
        }
        return mPlayerViewListener;
    }

    /***
     * ??????????????????
     */
    public void onResume() {
        boolean is = (Util.SDK_INT <= Build.VERSION_CODES.M || null == player) && isLoad && !isEnd;
        if (is) {
            createPlayers();
        }
    }

    /***
     * ??????????????????
     */
    @CallSuper
    public void onPause() {
        isPause = true;
        if (player != null) {
            handPause = !player.getPlayWhenReady();
            releasePlayers();
        }
    }

    @CallSuper
    public void onStop() {
        onPause();
    }

    /**
     * ??????????????????
     */
    @CallSuper
    public void onDestroy() {
        releasePlayers();
    }

    /***
     * ????????????
     */
    public void releasePlayers() {
        updateResumePosition();
        unNetworkBroadcastReceiver();
        if (player != null) {
            player.removeListener(componentListener);
            player.stop();
            player.release();
            player = null;
        }
        if (timer != null && !timer.isShutdown()) {
            timer.shutdown();
        }
        if (activity == null || activity.isFinishing()) {
            if (mediaSourceBuilder != null) {
                mediaSourceBuilder.destroy();
            }
            videoInfoListeners.clear();
            videoWindowListeners.clear();
            isEnd = false;
            isPause = false;
            handPause = false;
            timer = null;
            activity = null;
            mPlayerViewListener = null;
            mediaSourceBuilder = null;
            componentListener = null;
            playComponentListener = null;
            onClickListener = null;
        }
    }

    /****
     * ?????????????????????
     */
    public <R extends ExoUserPlayer> R startPlayer() {
        getPlayerViewListener().setPlayerBtnOnTouch(false);
        createPlayers();
        registerReceiverNet();
        return (R) this;
    }

    /****
     * ????????????
     */
    void createPlayers() {
        if (player == null) {
            player = createFullPlayer();
        }
        startVideo();
    }

    /***
     * ????????????
     **/
    public void startVideo() {
        //??????????????????WIFI??????????????????????????????????????????????????????????????????????????????????????????????????????wifi
        if (networkMode == ConnectivityManager.TYPE_MOBILE || getVideoPlayerView() != null && !getVideoPlayerView().isNetworkNotify()) {
            onPlayNoAlertVideo();
            return;
        }
        boolean iss = VideoPlayUtils.isWifi(activity) || VideoPlayerManager.getInstance().isClick() || isPause;
        if (iss) {
            if (!VideoPlayUtils.isWifi(activity)) {
                networkMode = ConnectivityManager.TYPE_MOBILE;
                getVideoPlayerView().showBtnContinueHint(View.VISIBLE);
                return;
            }
            onPlayNoAlertVideo();
        } else {
            networkMode = ConnectivityManager.TYPE_MOBILE;
            getVideoPlayerView().showBtnContinueHint(View.VISIBLE);
        }
    }


    /***
     * ?????????????????????????????????????????????
     **/
    public SimpleExoPlayer createFullPlayer() {
        setDefaultLoadModel();
        DefaultRenderersFactory rf = new DefaultRenderersFactory(activity, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        SimpleExoPlayer player = new SimpleExoPlayer.Builder(activity, rf).build();
        getPlayerViewListener().setPlayer(player);
        return player;
    }

    void onPlayNoAlertVideo() {
        onPlayNoAlertVideo(handPause);
    }

    /***
     * ???????????????????????????????????????
     */
    void onPlayNoAlertVideo(boolean handPause) {
        if (player == null) {
            player = createFullPlayer();
        }
        boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
        if (handPause) {
            player.setPlayWhenReady(false);
        } else {
            player.setPlayWhenReady(true);
        }
        player.setPlaybackParameters(playbackParameters);
        if (mPlayerViewListener != null) {
            mPlayerViewListener.showPreview(View.GONE, true);
            mPlayerViewListener.hideController(false);
            mPlayerViewListener.setControllerHideOnTouch(true);
        }
        player.addListener(componentListener);
        if (haveResumePosition) {
            player.seekTo(resumeWindow, resumePosition);
        }
        player.prepare(mediaSourceBuilder.getMediaSource(), !haveResumePosition, false);
        isEnd = false;
        isLoad = true;
    }

    /*****************??????????????????*************************/


    /***
     * ??????????????????
     * @param uri ??????
     */
    public void setPlayUri(@NonNull String uri) {
        setPlayUri(Uri.parse(uri));
    }

    /***
     * ?????????????????????????????????
     * @param uri ??????
     */
    public void setPlayUri(@NonNull String uri, Map<String, String> headers) {
        setPlayUri(uri, headers, null);
    }

    /***
     * ?????????????????????????????????
     * @param uri ??????
     */
    public void setPlayUri(@NonNull String uri, Map<String, String> headers, String subtitle) {
        mediaSourceBuilder.setHeaders(headers);
        mediaSourceBuilder.setSubtitle(subtitle);
        mediaSourceBuilder.setMediaUri(Uri.parse(uri));
        playUrl = uri;
    }

    /***
     * ?????????????????????????????????
     */
    public void setPlayUri(int index, @NonNull String[] videoUri, @NonNull String[] name, Map<String, String> headers) {
        setPlayUri(index, videoUri, name, headers, null);
    }

    /***
     * ?????????????????????????????????
     */
    public void setPlayUri(int index, @NonNull String[] videoUri, @NonNull String[] name, Map<String, String> headers, String subtitle) {
        mediaSourceBuilder.setHeaders(headers);
        mediaSourceBuilder.setSubtitle(subtitle);
        setPlaySwitchUri(index, videoUri, name);
    }

    public void setAudioUrls(List<String> audioUrls){
        mediaSourceBuilder.setAudioUrls(audioUrls);
    }

    /****
     * @param indexType ????????????????????????????????????
     * @param firstVideoUri ???????????????
     * @param secondVideoUri ???????????????
     */
    public void setPlayUri(@Size(min = 0) int indexType, @NonNull String firstVideoUri, @NonNull String secondVideoUri) {
        setPlayUri(indexType, Uri.parse(firstVideoUri), Uri.parse(secondVideoUri));

    }

    /***
     * ?????????????????????
     * @param index ??????????????????
     * @param videoUri ????????????
     * @param name ????????????????????????
     */
    public void setPlaySwitchUri(int index, @NonNull String[] videoUri, @NonNull String[] name) {
        setPlaySwitchUri(index, Arrays.asList(videoUri), Arrays.asList(name));
    }


    /***
     * ?????????????????????
     * @param switchIndex ????????????????????????
     * @param videoUri ????????????
     * @param name ????????????????????????
     */
    public void setPlaySwitchUri(int switchIndex, @NonNull List<String> videoUri, @NonNull List<String> name) {
        playUrl = videoUri.get(switchIndex);
        mediaSourceBuilder.setMediaSwitchUri(videoUri, switchIndex);
        getPlayerViewListener().setSwitchName(name, switchIndex);
    }

    /****
     * @param indexType ????????????????????????????????????
     * @param switchIndex the switch index
     * @param firstVideoUri ????????????
     * @param secondVideoUri ???????????????????????????
     * @param name the name
     */
    public void setPlaySwitchUri(@Size(min = 0) int indexType, @Size(min = 0) int switchIndex, @NonNull String firstVideoUri, String[] secondVideoUri, @NonNull String[] name) {
        setPlaySwitchUri(indexType, switchIndex, firstVideoUri, Arrays.asList(secondVideoUri), Arrays.asList(name));

    }

    /****
     * @param indexType ????????????????????????????????????
     * @param switchIndex the switch index
     * @param firstVideoUri ????????????
     * @param secondVideoUri ???????????????????????????
     * @param name the name
     */
    public void setPlaySwitchUri(@Size(min = 0) int indexType, @Size(min = 0) int switchIndex, @NonNull String firstVideoUri, List<String> secondVideoUri, @NonNull List<String> name) {
        playUrl = firstVideoUri;
        mediaSourceBuilder.setMediaUri(indexType, switchIndex, Uri.parse(firstVideoUri), secondVideoUri);
        getPlayerViewListener().setSwitchName(name, switchIndex);
    }

    /**
     * ??????????????????
     *
     * @param uri ??????
     */
    public void setPlayUri(@NonNull Uri uri) {
        mediaSourceBuilder.setMediaUri(uri);
    }

    /****
     * ????????????????????????
     * @param indexType ????????????????????????????????????
     * @param firstVideoUri ???????????????
     * @param secondVideoUri ???????????????
     */
    public void setPlayUri(@Size(min = 0) int indexType, @NonNull Uri firstVideoUri, @NonNull Uri secondVideoUri) {
        mediaSourceBuilder.setMediaUri(indexType, firstVideoUri, secondVideoUri);
    }


    /****
     * ????????????????????????
     * @param <T>     ???????????????
     * @param uris ??????
     */
    public <T extends ItemVideo> void setPlayUri(@NonNull List<T> uris) {
        mediaSourceBuilder.setMediaUri(uris);
    }


    /***
     * ??????????????????  ?????? LoadModelType.SPEED
     * @param loadModelType ??????
     *@deprecated
     */
    public void setLoadModel(@NonNull LoadModelType loadModelType) {
    }

    /***
     * ????????????
     * @param resumePosition ??????
     */
    public void setPosition(long resumePosition) {
        this.resumePosition = resumePosition;
    }

    /***
     * ????????????
     * @param currWindowIndex ????????????
     * @param currPosition ??????
     */
    public void setPosition(int currWindowIndex, long currPosition) {
        this.resumeWindow = currWindowIndex;
        this.resumePosition = currPosition;
    }

    /***
     * ????????????
     * @param  positionMs  positionMs
     */
    public void seekTo(long positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
            videoPlayerView.seekFromPlayer(positionMs);
        }
    }

    /***
     * ????????????
     * @param  windowIndex  windowIndex
     * @param  positionMs  positionMs
     */
    public void seekTo(int windowIndex, long positionMs) {
        if (player != null) {
            player.seekTo(windowIndex, positionMs);
        }
    }

    /***
     * ????????????????????????   Integer.MAX_VALUE ????????????
     *
     * @param loopingCount ????????????0
     */
    public void setLooping(@Size(min = 1) int loopingCount) {
        mediaSourceBuilder.setLooping(loopingCount);
    }

    /***
     * ??????????????????????????????????????????
     *
     * @param speed ??????????????????   1f ??????????????? ??????1 ??????
     * @param pitch ???????????????  1f ??????????????? ??????1 ??????
     */
    public void setPlaybackParameters(@Size(min = 0) float speed, @Size(min = 0) float pitch) {
        playbackParameters = new PlaybackParameters(speed, pitch);
        player.setPlaybackParameters(playbackParameters);
    }

    /***
     * ?????????????????????
     * @param value true ??????  false  ??????
     */
    public void setStartOrPause(boolean value) {
        if (player != null) {
            if (!isLoad && value) {
                playVideoUri();
            } else {
                player.setPlayWhenReady(value);
            }
        }
    }

    /***
     * ???????????????????????????
     * @param showVideoSwitch true ?????? false ?????????
     */
    public void setShowVideoSwitch(boolean showVideoSwitch) {
        getPlayerViewListener().setShowWitch(showVideoSwitch);
    }

    /***
     * ???????????????????????????
     * @param isOpenSeek true ?????? false ?????????
     */
    public void setSeekBarSeek(boolean isOpenSeek) {
        getPlayerViewListener().setSeekBarOpenSeek(isOpenSeek);
    }

    /***
     * ????????????????????????
     * @param videoInfoListener ??????
     * @deprecated {@link #addVideoInfoListener(VideoInfoListener)}
     */
    public void setVideoInfoListener(VideoInfoListener videoInfoListener) {
        videoInfoListeners.add(videoInfoListener);
        if (videoInfoListener != null) {
            addVideoInfoListener(videoInfoListener);
        }
    }

    /***
     * ????????????????????????
     * @param videoInfoListener ??????
     */
    public void addVideoInfoListener(@NonNull VideoInfoListener videoInfoListener) {
        videoInfoListeners.add(videoInfoListener);
    }

    /***
     *????????????????????????
     * @param videoInfoListener ??????
     */
    public void removeVideoInfoListener(@NonNull VideoInfoListener videoInfoListener) {
        videoInfoListeners.remove(videoInfoListener);
    }

    /****
     * ??????????????????????????????, ??????????????????
     * @param onClickListener ????????????
     */
    public void setOnPlayClickListener(@Nullable View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }


    /***
     * ??????????????????????????????
     * @param windowListener ??????
     * @deprecated {@link #addOnWindowListener(VideoWindowListener)}
     */
    public void setOnWindowListener(VideoWindowListener windowListener) {
        if (windowListener != null) {
            addOnWindowListener(windowListener);
        }
    }

    /***
     * ??????????????????????????????
     * @param windowListener ??????
     */
    public void addOnWindowListener(@NonNull VideoWindowListener windowListener) {
        videoWindowListeners.add(windowListener);
    }

    /***
     * ??????????????????????????????
     * @param windowListener ??????
     */
    public void removeOnWindowListener(@NonNull VideoWindowListener windowListener) {
        videoWindowListeners.remove(windowListener);
    }


    /********************?????????????????????????????????*****************************************************************/


    /***
     * ??????????????????
     * **/
    private void setDefaultLoadModel() {
        if (null == timer) {
            timer = Executors.newScheduledThreadPool(2);
            /*1s?????????????????????1s????????????**/
            timer.scheduleWithFixedDelay(task, 400, 300, TimeUnit.MILLISECONDS);
        }
    }

    public void reset() {
        releasePlayers();
    }

    /***
     * ???????????????????????????
     * @param uri uri
     * ***/
    private void setSwitchPlayer(@NonNull String uri) {
        playUrl = uri;
        handPause = false;
        updateResumePosition();
        if (mediaSourceBuilder.getMediaSource() instanceof ConcatenatingMediaSource) {
            ConcatenatingMediaSource source = (ConcatenatingMediaSource) mediaSourceBuilder.getMediaSource();
            source.getMediaSource(source.getSize() - 1).releaseSource(null);
            source.addMediaSource(mediaSourceBuilder.initMediaSource(Uri.parse(uri)));
            isSwitch = true;
        } else {
            mediaSourceBuilder.setMediaUri(Uri.parse(uri));
            onPlayNoAlertVideo();
        }
    }

    /***
     * ???????????????
     * @return boolean boolean
     */
    public boolean isPlaying() {
        if (player == null) return false;
        int playbackState = player.getPlaybackState();
        return playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
                && player.getPlayWhenReady();
    }

    /***
     * ??????????????????
     * @return int window count
     */
    public int getWindowCount() {
        if (player == null) {
            return 0;
        }
        return player.getCurrentTimeline().isEmpty() ? 1 : player.getCurrentTimeline().getWindowCount();
    }

    /***
     * ????????????????????????
     */
    public void next() {
        getPlayerViewListener().next();
    }

    /***
     * ??????????????????
     */
    public void hideControllerView() {
        hideControllerView(false);
    }

    /***
     * ??????????????????
     */
    public void showControllerView() {
        getPlayerViewListener().showController(false);
    }

    /***
     * ??????????????????
     * @param isShowFull ????????????????????????
     */
    public void hideControllerView(boolean isShowFull) {
        getPlayerViewListener().hideController(isShowFull);
    }

    /***
     * ??????????????????
     * @param isShowFull ????????????????????????
     */
    public void showControllerView(boolean isShowFull) {
        getPlayerViewListener().showController(isShowFull);
    }


    /****
     * ???????????????
     *
     * @param configuration ??????
     */
    public void onConfigurationChanged(Configuration configuration) {
        getPlayerViewListener().onConfigurationChanged(configuration.orientation);
    }

    public ImageView getPreviewImage() {
        return videoPlayerView.getPreviewImage();
    }

    /***
     * ????????????????????????
     * @return SimpleExoPlayer player
     */
    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public String getVideoString() {
        Format format = player.getVideoFormat();
        if (format == null) {
            return "";
        }
        return format.sampleMimeType + ", " + format.width + " x "
                + format.height + ", " + format.frameRate + "fps";
    }

    public String getAudioString() {
        Format format = player.getAudioFormat();
        if (format == null) {
            return "";
        }
        return format.sampleMimeType + ", " + format.sampleRate + " Hz";
    }

    /**
     * ?????????????????????  ??????????????????
     *
     * @return long duration
     */
    public long getDuration() {
        return player == null ? 0 : player.getDuration();
    }

    /**
     * ??????????????????????????????  ??????????????????
     *
     * @return long current position
     */
    public long getCurrentPosition() {
        return player == null ? 0 : player.getCurrentPosition();
    }

    /**
     * ????????????????????????d????????????  ??????????????????
     *
     * @return long buffered position
     */
    public long getBufferedPosition() {
        return player == null ? 0 : player.getBufferedPosition();
    }


    VideoPlayerView getVideoPlayerView() {
        return videoPlayerView;
    }

    /****
     * ????????????
     */
    private void updateResumePosition() {
        if (player != null) {
            resumeWindow = player.getCurrentWindowIndex();
            resumePosition = Math.max(0, player.getContentPosition());
        }
    }

    /**
     * ????????????
     ***/
    protected void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    /***
     * ??????????????????
     **/
    private final Runnable task = new Runnable() {
        @Override
        public void run() {
            if (getPlayerViewListener().isLoadingShow()) {
                getPlayerViewListener().showNetSpeed(getNetSpeed());
            }
        }
    };

    /****
     * ??????????????????
     *
     * @return String ????????????????????????
     **/
    private String getNetSpeed() {
        String netSpeed;
        long nowTotalRxBytes = VideoPlayUtils.getTotalRxBytes(activity);
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = (nowTimeStamp - lastTimeStamp);
        if (calculationTime == 0) {
            netSpeed = String.valueOf(1) + " kb/s";
            return netSpeed;
        }
        //????????????
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime);
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        if (speed > 1024) {
            DecimalFormat df = new DecimalFormat("######0.0");
            netSpeed = String.valueOf(df.format(VideoPlayUtils.getM(speed))) + " MB/s";
        } else {
            netSpeed = String.valueOf(speed) + " kb/s";
        }
        return netSpeed;
    }


    /****
     * ??????????????? true ???????????????????????????false ???????????????
     *
     * @return boolean boolean
     */
    public boolean onBackPressed() {
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getPlayerViewListener().exitFull();
            return false;
        } else {
            return true;
        }
    }

    /***
     * ??????????????????
     */
    void registerReceiverNet() {
        if (mNetworkBroadcastReceiver == null) {
            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            mNetworkBroadcastReceiver = new NetworkBroadcastReceiver();
            activity.registerReceiver(mNetworkBroadcastReceiver, intentFilter);
        }
    }

    /***
     * ??????????????????
     */
    void unNetworkBroadcastReceiver() {
        if (mNetworkBroadcastReceiver != null) {
            activity.unregisterReceiver(mNetworkBroadcastReceiver);
        }
        mNetworkBroadcastReceiver = null;
    }

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public SwitchListener getSwitchListener() {
        return switchListener;
    }

    public void setSwitchListener(SwitchListener switchListener) {
        this.switchListener = switchListener;
    }


    /***
     * ???????????????
     ***/
    private final class NetworkBroadcastReceiver extends BroadcastReceiver {
        long is = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (getVideoPlayerView() != null && !getVideoPlayerView().isNetworkNotify()) {
                return;
            }
            if (null != action && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                assert mConnectivityManager != null;
                NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();
                if (netInfo != null && netInfo.isAvailable()) {
                    if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                        /////////3g??????
                        if (System.currentTimeMillis() - is > 500 && networkMode == ConnectivityManager.TYPE_WIFI) {
                            is = System.currentTimeMillis();
                            if (!isPause) {
                                getVideoPlayerView().showBtnContinueHint(View.VISIBLE);
                                setStartOrPause(false);
                                networkMode = netInfo.getType();
                            }
                        }
                    } else if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        /////////?????? WiFi ??????
                        if (System.currentTimeMillis() - is > 500 && networkMode == ConnectivityManager.TYPE_MOBILE) {
                            is = System.currentTimeMillis();
                            networkMode = netInfo.getType();
//                            if (!isPause) {
//                                networkMode = netInfo.getType();
//                                getVideoPlayerView().showBtnContinueHint(View.GONE);
//                                if (isLoad()) {
//                                    setStartOrPause(true);
//                                } else {
//                                    playVideoUri();
//                                }
//                            }
                        }
                    }
                }
            }

        }
    }

    public void playVideoUri() {
        playComponentListener.playVideoUri();
        setStartOrPause(true);
    }


    /****
     * ????????????view????????????
     * ***/
    private final class PlayComponentListener implements ExoPlayerListener {
        @Override
        public void onCreatePlayers() {
            createPlayers();
        }

        @Override
        public void replayPlayers() {
            clearResumePosition();
            handPause = false;
            if (getPlayer() == null) {
                createPlayers();
            } else {
                getPlayer().seekTo(0, 0);
                getPlayer().setPlayWhenReady(true);
            }

        }


        @Override
        public void switchUri(int position) {
            if (switchListener != null) {
                List<String> urls = switchListener.switchUri(mediaSourceBuilder.getVideoUri(), position);
                if (urls != null && !urls.isEmpty()) {
                    mediaSourceBuilder.setVideoUri(urls);
                }
            }
            if (mediaSourceBuilder.getVideoUri() != null) {
                setSwitchPlayer(mediaSourceBuilder.getVideoUri().get(position));
            }
        }

        @Override
        public void playVideoUri() {
            VideoPlayerManager.getInstance().setClick(true);
            onPlayNoAlertVideo();
        }

        @Override
        public void playVideoUriForce() {
            VideoPlayerManager.getInstance().setClick(true);
            onPlayNoAlertVideo(false);
        }

        @Override
        public ExoUserPlayer getPlay() {
            return ExoUserPlayer.this;
        }

        @Override
        public void startPlayers() {
            startPlayer();
        }

        @Override
        public View.OnClickListener getClickListener() {
            return onClickListener;
        }

        @Override
        public void land() {
            boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
            if (handPause) {
                player.setPlayWhenReady(false);
            } else {
                player.setPlayWhenReady(true);
            }
            player.prepare(mediaSourceBuilder.getMediaSource(), !haveResumePosition, false);
        }
    }

    /***
     * view ???????????? ?????????
     */
    Player.EventListener componentListener = new Player.EventListener() {
        boolean isRemove;
        private int currentWindowIndex;

        @Override
        public void onTimelineChanged(Timeline timeline, int reason) {
            if (isSwitch) {
                isSwitch = false;
                isRemove = true;
                player.seekTo(player.getNextWindowIndex(), resumePosition);
            }
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            // Log.d(TAG, "onTracksChanged:" + currentWindowIndex + "_:" + player.getCurrentTimeline().getWindowCount());
            //   Log.d(TAG, "onTracksChanged:" + player.getNextWindowIndex() + "_:" + player.getCurrentTimeline().getWindowCount());
            if (getWindowCount() > 1) {
                if (isRemove) {
                    isRemove = false;
                    mediaSourceBuilder.removeMediaSource(resumeWindow);
                    return;
                }
                if (!videoWindowListeners.isEmpty()) {
                    for (VideoWindowListener videoWindowListener : videoWindowListeners) {
                        videoWindowListener.onCurrentIndex(currentWindowIndex, getWindowCount());
                    }
                    currentWindowIndex += 1;
                }
                if (mediaSourceBuilder.getIndexType() < 0) {
                    return;
                }
                GestureVideoPlayer gestureVideoPlayer = null;
                if (ExoUserPlayer.this instanceof GestureVideoPlayer) {
                    gestureVideoPlayer = (GestureVideoPlayer) ExoUserPlayer.this;
                }
                boolean setOpenSeek = !(mediaSourceBuilder.getIndexType() == currentWindowIndex && mediaSourceBuilder.getIndexType() > 0);
                if (gestureVideoPlayer != null) {
                    gestureVideoPlayer.setPlayerGestureOnTouch(setOpenSeek);
                }
                getPlayerViewListener().setOpenSeek(setOpenSeek);
            }
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
        }

        /**
         * ?????????????????????
         * STATE_IDLE ????????????????????????????????????????????????
         * STATE_PREPARING ?????????????????????
         * STATE_BUFFERING ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
         * STATE_ENDED ???????????????
         */
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playWhenReady) {
                //?????????
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                //?????????
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            for (VideoInfoListener videoInfoListener : videoInfoListeners) {
                videoInfoListener.isPlaying(player.getPlayWhenReady());
            }
            Log.d(TAG, "onPlayerStateChanged:" + playbackState + "+playWhenReady:" + playWhenReady);
            switch (playbackState) {
                case Player.STATE_BUFFERING:
                    if (playWhenReady) {
                        getPlayerViewListener().showLoadStateView(View.VISIBLE);
                    }
                    for (VideoInfoListener videoInfoListener : videoInfoListeners) {
                        videoInfoListener.onLoadingChanged();
                    }
                    break;
                case Player.STATE_ENDED:
                    Log.d(TAG, "onPlayerStateChanged:ended?????????");
                    isEnd = true;
                    getPlayerViewListener().showReplayView(View.VISIBLE);
                    currentWindowIndex = 0;
                    clearResumePosition();
                    for (VideoInfoListener videoInfoListener : videoInfoListeners) {
                        videoInfoListener.onPlayEnd();
                    }
                    break;
                case Player.STATE_IDLE:
                    Log.d(TAG, "onPlayerStateChanged::??????????????????????????????????????????");
                    getPlayerViewListener().showErrorStateView(View.VISIBLE);
                    break;
                case Player.STATE_READY:
                    mPlayerViewListener.showPreview(View.GONE, false);
                    getPlayerViewListener().showLoadStateView(View.GONE);
                    if (playWhenReady) {
                        Log.d(TAG, "onPlayerStateChanged:????????????");
                        isPause = false;
                        for (VideoInfoListener videoInfoListener : videoInfoListeners) {
                            videoInfoListener.onPlayStart(getCurrentPosition());
                        }
                    }
                    break;
                default:
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
        }

        @Override
        public void onPlayerError(PlaybackException e) {
            Log.e(TAG, "onPlayerError:" + e.getMessage() + ", " + e.getErrorCodeName());
            if ("ERROR_CODE_IO_BAD_HTTP_STATUS".equals(e.getErrorCodeName())
                    && getDuration() > 90000
                    && mediaSourceBuilder != null
                    && mediaSourceBuilder.getMediaSource() != null
                    && "com.google.android.exoplayer2.source.hls.HlsMediaSource".equals(mediaSourceBuilder.getMediaSource().getClass().getName())) {
                Log.e(TAG, "onPlayerError: " + mediaSourceBuilder.getMediaSource().getClass());
                if (getCurrentPosition() < 90000) {
                    setPosition(getCurrentPosition() + 10000);
                    startVideo();
                    return;
                }
            }
            updateResumePosition();
            if (e instanceof ExoPlaybackException) {
                if (VideoPlayUtils.isBehindLiveWindow((ExoPlaybackException) e)) {
                    clearResumePosition();
                    startVideo();
                } else {
                    getPlayerViewListener().showErrorStateView(View.VISIBLE);
                    for (VideoInfoListener videoInfoListener : videoInfoListeners) {
                        videoInfoListener.onPlayerError((ExoPlaybackException) e);
                    }
                }
            }
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        }

        @Override
        public void onSeekProcessed() {

        }
    };

    public interface SwitchListener {
        List<String> switchUri(List<String> videoUri, int position);
    }

}

