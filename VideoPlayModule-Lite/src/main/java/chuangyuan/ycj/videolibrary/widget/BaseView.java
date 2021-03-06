package chuangyuan.ycj.videolibrary.widget;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.ExoPlayerControlView;
import com.google.android.exoplayer2.ui.ExoPlayerView;
import com.google.android.exoplayer2.ui.TimeBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chuangyuan.ycj.videolibrary.R;
import chuangyuan.ycj.videolibrary.danmuku.BiliDanmukuParser;
import chuangyuan.ycj.videolibrary.danmuku.DanamakuAdapter;
import chuangyuan.ycj.videolibrary.danmuku.DanmuWebView;
import chuangyuan.ycj.videolibrary.danmuku.JSONDanmukuParser;
import chuangyuan.ycj.videolibrary.listener.ExoPlayerListener;
import chuangyuan.ycj.videolibrary.utils.VideoPlayUtils;
import chuangyuan.ycj.videolibrary.video.ExoUserPlayer;
import chuangyuan.ycj.videolibrary.video.VideoPlayerManager;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.IDisplayer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import master.flame.danmaku.ui.widget.DanmakuView;

/**
 * author  yangc
 * date 2017/11/24
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated: ??????view ??????????????????
 */
abstract class BaseView extends FrameLayout {
    /*** The constant TAG.***/
    public static final String TAG = VideoPlayerView.class.getName();
    final Activity activity;
    /***??????view*/
    protected final ExoPlayerView playerView;
    private static final int ANIM_DURATION = 400;
    /*** ??????????????????*/
    protected TextView videoLoadingShowText;
    /***???????????????,?????????,????????????,???????????????,?????????????????????,????????????,????????????*/
    protected View exoLoadingLayout, exoPlayPreviewLayout, exoPreviewPlayBtn, exoBarrageLayout;
    /***??????,???????????????,???????????????????????????*/
    protected ImageView exoPlayWatermark, exoPreviewImage, exoPreviewBottomImage;

    /***??????????????????view***/
    protected final GestureControlView mGestureControlView;
    /***??????????????????view***/
    protected final ActionControlView mActionControlView;

    public LockControlView getmLockControlView() {
        return mLockControlView;
    }

    /*** ??????????????????***/
    protected final LockControlView mLockControlView;
    /***??????????????????***/
    protected final ExoPlayerControlView controllerView;
    /***??????*/
    protected BelowView belowView;
    /***???????????????***/
    protected AlertDialog alertDialog;
    private boolean networkNotify = true;
    protected ExoPlayerListener mExoPlayerListener;

    public AppCompatImageView getExoControlsBack() {
        return exoControlsBack;
    }

    /***????????????*/
    protected AppCompatImageView exoControlsBack;

    public void setLand(boolean land) {
        isLand = land;
    }

    /***???????????????,????????????,?????????????????? ??????false,??????????????????*/
    protected boolean isLand;
    protected boolean isListPlayer;

    public boolean isShowVideoSwitch() {
        return isShowVideoSwitch;
    }

    protected boolean isShowVideoSwitch;
    protected boolean isVerticalFullScreen;
    protected boolean isPipMode;

    protected boolean isLandLayout;

    private boolean networkNotifyUseDialog = false;

    /**
     * ?????????????????????????????????
     *
     * @param networkNotifyUseDialog
     */
    public void setNetworkNotifyUseDialog(boolean networkNotifyUseDialog) {
        this.networkNotifyUseDialog = networkNotifyUseDialog;
    }

    /**
     * ?????????????????????????????????
     *
     * @param text
     */
    public void showNotice(String text) {
        if (mGestureControlView != null) {
            mGestureControlView.showNotice(text);
        }
    }

    public String getNotice() {
        if (mGestureControlView != null) {
            return mGestureControlView.getNotice();
        } else {
            return null;
        }
    }

    public boolean isLand() {
        return isLand;
    }

    public boolean isLandLayout() {
        return isLandLayout;
    }

    public void setLandLayout(boolean landLayout) {
        isLandLayout = landLayout;
    }

    /**
     * ????????????????????????
     **/
    private boolean isShowBack = true;
    /***???????????????*/
    protected int getPaddingLeft;
    private ArrayList<String> nameSwitch;
    /***????????????,??????Ui?????????????????????????????????***/
    protected int switchIndex, setSystemUiVisibility = 0;
    /*** The Ic back image.***/
    @DrawableRes
    private int icBackImage = R.drawable.ic_exo_back;


    private BaseDanmakuParser parser;//???????????????
    private IDanmakuView danmakuView;//??????view
    private DanmakuContext danmakuContext;

    public boolean isDanmaKuShow() {
        return danmaKuShow;
    }

    private boolean danmaKuShow = false;
    private WebView danmuWebView;

    public boolean isUseDanmuWebView() {
        return useDanmuWebView;
    }

    private boolean useDanmuWebView = false;
    private boolean danmuDestroyed = false;
    private int danmuLineCount = 5;

    protected ViewGroup danmuViewContainer;


    public ViewGroup getDanmuViewContainer() {
        if (danmuViewContainer == null) {
            return playerView;
        }
        return danmuViewContainer;
    }

    public void setDanmuViewContainer(ViewGroup danmuViewContainer) {
        this.danmuViewContainer = danmuViewContainer;
    }

    /**
     * Instantiates a new Base view.
     *
     * @param context the context
     */
    public BaseView(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Instantiates a new Base view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public BaseView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Instantiates a new Base view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public BaseView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        activity = (Activity) getContext();
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        playerView = new ExoPlayerView(getContext(), attrs, defStyleAttr);
        controllerView = (ExoPlayerControlView) playerView.getControllerView();
        mGestureControlView = new GestureControlView(getContext(), attrs, defStyleAttr);
        mActionControlView = new ActionControlView(getContext(), attrs, defStyleAttr, playerView);
        mLockControlView = new LockControlView(getContext(), attrs, defStyleAttr, this);
        addView(playerView, params);
        int userWatermark = 0;
        int defaultArtworkId = 0;
        int loadId = R.layout.simple_exo_play_load;
        int preViewLayoutId = 0;
        int barrageLayoutId = 0;
        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.VideoPlayerView, 0, 0);
            try {
                icBackImage = a.getResourceId(R.styleable.VideoPlayerView_player_back_image, icBackImage);
                userWatermark = a.getResourceId(R.styleable.VideoPlayerView_user_watermark, 0);
                isListPlayer = a.getBoolean(R.styleable.VideoPlayerView_player_list, false);
                defaultArtworkId = a.getResourceId(R.styleable.VideoPlayerView_default_artwork, defaultArtworkId);
                loadId = a.getResourceId(R.styleable.VideoPlayerView_player_load_layout_id, loadId);
                preViewLayoutId = a.getResourceId(R.styleable.VideoPlayerView_player_preview_layout_id, preViewLayoutId);
                barrageLayoutId = a.getResourceId(R.styleable.VideoPlayerView_player_barrage_layout_id, barrageLayoutId);
                int playerViewId = a.getResourceId(R.styleable.VideoPlayerView_controller_layout_id, R.layout.simple_exo_playback_control_view);
                if (preViewLayoutId == 0 && (playerViewId == R.layout.simple_exo_playback_list_view || playerViewId == R.layout.simple_exo_playback_top_view)) {
                    preViewLayoutId = R.layout.exo_default_preview_layout;
                }
            } finally {
                a.recycle();
            }
        }
        if (barrageLayoutId != 0) {
            exoBarrageLayout = inflate(context, barrageLayoutId, null);
        }
        exoLoadingLayout = inflate(context, loadId, null);
        if (preViewLayoutId != 0) {
            exoPlayPreviewLayout = inflate(context, preViewLayoutId, null);
        }
        intiView();
        initWatermark(userWatermark, defaultArtworkId);
    }


    /**
     * Inti view.
     */
    private void intiView() {
        exoControlsBack = new AppCompatImageView(getContext());
        exoControlsBack.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        int ss = VideoPlayUtils.dip2px(getContext(), 7f);
        exoControlsBack.setId(R.id.exo_controls_back);
        exoControlsBack.setImageDrawable(ContextCompat.getDrawable(getContext(), icBackImage));
        exoControlsBack.setPadding(ss, ss, ss, ss);
        FrameLayout frameLayout = playerView.getContentFrameLayout();
        frameLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.exo_player_background_color));
        exoLoadingLayout.setVisibility(GONE);
        exoLoadingLayout.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.exo_player_background_color));
//        exoLoadingLayout.setClickable(true);
        frameLayout.addView(mGestureControlView, frameLayout.getChildCount());
        frameLayout.addView(mActionControlView, frameLayout.getChildCount());
        frameLayout.addView(mLockControlView, frameLayout.getChildCount());
        if (null != exoPlayPreviewLayout) {
            frameLayout.addView(exoPlayPreviewLayout, frameLayout.getChildCount());
        }
        frameLayout.addView(exoLoadingLayout, frameLayout.getChildCount());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(VideoPlayUtils.dip2px(getContext(), 35f), VideoPlayUtils.dip2px(getContext(), 35f));
        frameLayout.addView(exoControlsBack, frameLayout.getChildCount(), layoutParams);
        int index = frameLayout.indexOfChild(findViewById(R.id.exo_controller_barrage));
        if (exoBarrageLayout != null) {
            frameLayout.removeViewAt(index);
            exoBarrageLayout.setBackgroundColor(Color.TRANSPARENT);
            frameLayout.addView(exoBarrageLayout, index);
        }
        exoPlayWatermark = playerView.findViewById(R.id.exo_player_watermark);
        videoLoadingShowText = playerView.findViewById(R.id.exo_loading_show_text);

        exoPreviewBottomImage = playerView.findViewById(R.id.exo_preview_image_bottom);
        if (playerView.findViewById(R.id.exo_preview_image) != null) {
            exoPreviewImage = playerView.findViewById(R.id.exo_preview_image);
            exoPreviewImage.setBackgroundResource(android.R.color.transparent);
        } else {
            exoPreviewImage = exoPreviewBottomImage;
        }
        setSystemUiVisibility = ((Activity) getContext()).getWindow().getDecorView().getSystemUiVisibility();

        exoPreviewPlayBtn = playerView.findViewById(R.id.exo_preview_play);
    }

    /**
     * ????????????????????????
     *
     * @param verticalFullScreen isWGh  ?????? false  true ??????
     */
    public void setVerticalFullScreen(boolean verticalFullScreen) {
        isVerticalFullScreen = verticalFullScreen;
    }

    public boolean isVerticalFullScreen() {
        return isVerticalFullScreen;
    }

    /**
     * ????????????????????????
     *
     * @param verticalFullScreen isWGh  ?????? false  true ??????
     */
    public void setPipMode(boolean verticalFullScreen) {
        isPipMode = verticalFullScreen;
    }

    public boolean isPipMode() {
        return isPipMode;
    }


    /**
     * On destroy.
     */
    public void onDestroy() {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        if (belowView != null) {
            belowView = null;
        }
        if (exoControlsBack != null && exoControlsBack.animate() != null) {
            exoControlsBack.animate().cancel();
        }
        if (mLockControlView != null) {
            mLockControlView.onDestroy();
        }
        if (mExoPlayerListener != null) {
            mExoPlayerListener = null;
        }
        nameSwitch = null;
        if (danmakuView != null) {
            danmakuView.release();
        }
        danmuDestroyed = true;
        if (danmuWebView != null) {
            danmuWebView.onPause();
            danmuWebView.destroy();
        }
    }


    /***
     * ???????????????????????????
     * @param userWatermark userWatermark  ?????????
     * @param defaultArtworkId defaultArtworkId   ?????????
     */
    protected void initWatermark(int userWatermark, int defaultArtworkId) {
        if (userWatermark != 0) {
            exoPlayWatermark.setImageResource(userWatermark);
        }
        if (defaultArtworkId != 0) {
            setPreviewImage(BitmapFactory.decodeResource(getResources(), defaultArtworkId));
        }
    }

    /***
     * ?????????????????????
     */
    protected void hideDialog() {
        if (alertDialog == null || !alertDialog.isShowing()) {
            return;
        }
        alertDialog.dismiss();
    }

    /***
     * ?????????????????????
     */
    protected void showDialog() {
        if (!networkNotify) {
            showBtnContinueHint(View.GONE);
            if (mExoPlayerListener != null) {
                if (mExoPlayerListener.getPlay() != null && mExoPlayerListener.getPlay().isLoad()) {
                    mExoPlayerListener.getPlay().setStartOrPause(true);
                } else {
                    mExoPlayerListener.playVideoUri();
                    mExoPlayerListener.getPlay().setStartOrPause(true);
                }
            }
            return;
        }
        if (alertDialog != null && alertDialog.isShowing()) {
            return;
        }
        alertDialog = new AlertDialog.Builder(getContext()).create();
        alertDialog.setTitle(getContext().getString(R.string.exo_play_reminder));
        alertDialog.setMessage(getContext().getString(R.string.exo_play_wifi_hint_no));
        alertDialog.setCancelable(false);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getContext().
                getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (mExoPlayerListener != null) {
                    if (mExoPlayerListener.getPlay() != null && mExoPlayerListener.getPlay().isLoad()) {
                        mExoPlayerListener.getPlay().setStartOrPause(false);
                    }
                }
            }
        });
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getContext().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showBtnContinueHint(View.GONE);
                if (mExoPlayerListener != null) {
                    if (mExoPlayerListener.getPlay() != null && mExoPlayerListener.getPlay().isLoad()) {
                        mExoPlayerListener.getPlay().setStartOrPause(true);
                    } else {
                        mExoPlayerListener.playVideoUri();
                        mExoPlayerListener.getPlay().setStartOrPause(true);
                    }
                }

            }
        });
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.shape_dialog_cardbg);
            alertDialog.show();
            WindowManager.LayoutParams lp = alertDialog.getWindow().getAttributes();
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            manager.getDefaultDisplay().getMetrics(outMetrics);
            int width = Math.min(outMetrics.widthPixels, outMetrics.heightPixels);
            if (width > 0) {
                lp.width = 4 * width / 5;
                alertDialog.getWindow().setAttributes(lp);
            }
        }
    }

    public void toPortraitLayout() {
        if (!isLandLayout) {
            return;
        }
        ViewGroup parent = (ViewGroup) playerView.getParent();
        if (parent != null) {
            parent.removeView(playerView);
        }
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addView(playerView, params);
        isLandLayout = false;
    }

    public void toLandLayout() {
        if (isLandLayout) {
            return;
        }
        ViewGroup parent = (ViewGroup) playerView.getParent();
        if (parent != null) {
            parent.removeView(playerView);
        }
        ViewGroup contentView = ((Activity) getContext()).findViewById(android.R.id.content);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        contentView.addView(playerView, params);
        isLandLayout = true;
    }

    /***
     * ???????????????????????????
     *
     * @param newConfig ????????????
     */
    protected void scaleLayout(int newConfig) {
        if (isPipMode) {
            toLandLayout();
            return;
        }
        if (isVerticalFullScreen()) {
            scaleVerticalLayout();
            return;
        }
        if (newConfig == Configuration.ORIENTATION_PORTRAIT) {
            toPortraitLayout();
        } else {
            toLandLayout();
        }
    }

    /***
     * ????????????????????????
     *
     */
    public void scaleVerticalLayout() {
        ViewGroup contentView = activity.findViewById(android.R.id.content);
        final ViewGroup parent = (ViewGroup) playerView.getParent();
        if (isLand) {
            if (parent != null) {
                parent.removeView(playerView);
            }
            LayoutParams params;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                contentView.addView(playerView, params);
            } else {
                params = new LayoutParams(getWidth(), getHeight());
                contentView.addView(playerView, params);
                ChangeBounds changeBounds = new ChangeBounds();
                //???????????????????????????????????????????????????????????????
                changeBounds.setDuration(ANIM_DURATION);
                TransitionManager.beginDelayedTransition(contentView, changeBounds);
                ViewGroup.LayoutParams layoutParams = playerView.getLayoutParams();
                layoutParams.height = LayoutParams.MATCH_PARENT;
                layoutParams.width = LayoutParams.MATCH_PARENT;
                playerView.setLayoutParams(layoutParams);
            }

        } else {
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ChangeBounds changeBounds = new ChangeBounds();
                //???????????????????????????????????????????????????????????????
                changeBounds.setDuration(ANIM_DURATION);
                TransitionManager.beginDelayedTransition(contentView, changeBounds);
                ViewGroup.LayoutParams layoutParams2 = playerView.getLayoutParams();
                layoutParams2.width = getWidth();
                layoutParams2.height = getHeight();
                playerView.setLayoutParams(layoutParams2);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (parent != null) {
                            parent.removeView(playerView);
                        }
                        if (playerView.getParent() != null) {
                            ((ViewGroup) playerView.getParent()).removeView(playerView);
                        }
                        BaseView.this.addView(playerView);
                    }
                }, ANIM_DURATION);
            } else {
                if (parent != null) {
                    parent.removeView(playerView);
                }
                addView(playerView, params);
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mExoPlayerListener.land();
        }
    }

    /***
     * ?????????????????????
     *
     * @param visibility ??????
     */
    public void showLockState(int visibility) {
        mLockControlView.showLockState(visibility);
    }

    public int getLockState() {
        return mLockControlView.getLockState();
    }

    public boolean isLock() {
        return mLockControlView.isLock();
    }

    /***
     * ?????????????????????
     *
     * @param visibility ??????
     */
    protected void showLoadState(int visibility) {
        if (visibility == View.VISIBLE) {
            showErrorState(GONE);
            showReplay(GONE);
            showLockState(GONE);
        }
        if (exoLoadingLayout != null) {
            exoLoadingLayout.setVisibility(visibility);
        }
    }

    /***
     * ?????????????????????
     *
     * @param visibility ??????
     */
    protected void showErrorState(int visibility) {
        if (visibility == View.VISIBLE) {
            playerView.hideController();
            showReplay(GONE);
            showBackView(VISIBLE, true);
            showLockState(GONE);
            showLoadState(GONE);
            showPreViewLayout(GONE);
        }
        mActionControlView.showErrorState(visibility);
    }

    /***
     * ?????????????????????
     *
     * @param visibility ??????
     */
    public void showBtnContinueHint(int visibility) {
        showBtnContinueHint(visibility, getResources().getString(R.string.exo_play_wifi_hint_no));
    }

    /***
     * ?????????????????????
     *
     * @param visibility ??????
     */
    public void showBtnContinueHint(int visibility, String msg) {
        if (visibility == View.VISIBLE) {
            showReplay(GONE);
            showErrorState(GONE);
            showPreViewLayout(GONE);
            showLoadState(GONE);
            showBackView(VISIBLE, true);
            if (networkNotifyUseDialog && msg != null) {
                showDialog();
            }
        } else {
            hideDialog();
        }
        mActionControlView.showBtnContinueHint(visibility, msg);
    }

    /***
     * ?????????????????????
     *
     * @param visibility ??????
     */
    protected void showReplay(int visibility) {
        if (visibility == View.VISIBLE) {
            controllerView.hideNo();
            showErrorState(GONE);
            showBtnContinueHint(GONE);
            showPreViewLayout(GONE);
            showLockState(GONE);
            showBackView(VISIBLE, true);
            showLoadState(GONE);
        }
        mActionControlView.showReplay(visibility);
    }

    /***
     * ?????????????????????????????????
     *
     * @param visibility ??????
     */
    protected void showPreViewLayout(int visibility) {
        if (exoPlayPreviewLayout != null) {
            if (exoPlayPreviewLayout.getVisibility() == visibility) {
                return;
            }
            exoPlayPreviewLayout.setVisibility(visibility);
            if (playerView.findViewById(R.id.exo_preview_play) != null) {
                playerView.findViewById(R.id.exo_preview_play).setVisibility(visibility);
            }
        }
    }

    /***
     * ?????????????????????
     *
     * @param visibility ??????
     * @param is is
     */
    protected void showBackView(int visibility, boolean is) {
        if (exoControlsBack != null) {
            //??????????????????????????????????????????????????????
            if (!isShowBack && !isLand) {
                exoControlsBack.setVisibility(GONE);
                return;
            }
            if (isListPlayer() && !isLand) {
                exoControlsBack.setVisibility(GONE);
            } else {
                if (visibility == VISIBLE && is) {
                    exoControlsBack.setTranslationY(0);
                    exoControlsBack.setAlpha(1f);
                }
                exoControlsBack.setVisibility(visibility);
            }
        }
    }


    /***
     * ?????????????????????????????????????????????????????????????????????
     * @param visibility ??????
     * @param bitmap the bitmap
     */
    protected void showBottomView(int visibility, Bitmap bitmap) {
        exoPreviewBottomImage.setVisibility(visibility);
        if (bitmap != null) {
            exoPreviewBottomImage.setImageBitmap(bitmap);
        }
    }


    public boolean isShowBack() {
        return isShowBack;
    }

    /**
     * ????????????
     *
     * @param showBack true ????????????  false ??????
     */
    public void setShowBack(boolean showBack) {
        this.isShowBack = showBack;
    }

    /**
     * ????????????
     *
     * @param title ??????
     */
    public void setTitle(@NonNull String title) {
        controllerView.setTitle(title);
    }

    /***
     * ???????????????
     *
     * @param res ??????
     */
    public void setExoPlayWatermarkImg(int res) {
        if (exoPlayWatermark != null) {
            exoPlayWatermark.setImageResource(res);
        }
    }

    /**
     * ?????????????????????
     *
     * @param previewImage ?????????
     */
    public void setPreviewImage(Bitmap previewImage) {
        this.exoPreviewImage.setImageBitmap(previewImage);
    }

    /***
     * ??????????????????????????? .,?????????????????????????????????????????????
     *
     * @param mExoPlayerListener ??????
     */
    public void setExoPlayerListener(ExoPlayerListener mExoPlayerListener) {
        this.mExoPlayerListener = mExoPlayerListener;
    }

    /***
     * ??????????????????????????????
     *
     * @param showVideoSwitch true ??????  false ?????????
     */
    public void setShowVideoSwitch(boolean showVideoSwitch) {
        isShowVideoSwitch = showVideoSwitch;
    }

    /**
     * ????????????????????????
     *
     * @param icFullscreenStyle ??????????????????
     */
    public void setFullscreenStyle(@DrawableRes int icFullscreenStyle) {
        controllerView.setFullscreenStyle(icFullscreenStyle);
    }

    /**
     * ??????????????????????????????
     *
     * @param openLock ?????? true ??????   false ?????????
     */
    public void setOpenLock(boolean openLock) {
        mLockControlView.setOpenLock(openLock);
    }

    /**
     * ??????????????????????????????
     *
     * @param openLock ?????? false ?????????   true ??????
     */
    public void setOpenProgress2(boolean openLock) {
        mLockControlView.setProgress(openLock);
    }

    /**
     * Gets name switch.
     *
     * @return the name switch
     */
    protected ArrayList<String> getNameSwitch() {
        if (nameSwitch == null) {
            nameSwitch = new ArrayList<>();
        }
        return nameSwitch;
    }

    protected void setNameSwitch(ArrayList<String> nameSwitch) {
        this.nameSwitch = nameSwitch;
    }

    /**
     * Gets name switch.
     *
     * @return the name switch
     */
    protected int getSwitchIndex() {
        return switchIndex;
    }

    /**
     * ???????????????????????????
     *
     * @param name        name
     * @param switchIndex switchIndex
     */
    public void setSwitchName(@NonNull List<String> name, @Size(min = 0) int switchIndex) {
        this.nameSwitch = new ArrayList<>(name);
        this.switchIndex = switchIndex;
        getSwitchText().setText(name.get(switchIndex));
    }

    /****
     * ???????????????
     *
     * @return PlaybackControlView playback control view
     */
    @NonNull
    public ExoPlayerControlView getPlaybackControlView() {
        return controllerView;
    }

    /***
     * ????????????????????????
     *
     * @return boolean
     */
    public boolean isLoadingLayoutShow() {
        return exoLoadingLayout.getVisibility() == VISIBLE;
    }

    /***
     * ??????????????????view
     *
     * @return View load layout
     */
    @Nullable
    public View getLoadLayout() {
        return exoLoadingLayout;
    }

    /***
     * ??????????????????view
     *
     * @return View play hint layout
     */
    @Nullable
    public View getPlayHintLayout() {
        return mActionControlView.getPlayBtnHintLayout();
    }

    /***
     * ????????????view
     *
     * @return View replay layout
     */
    @Nullable
    public View getReplayLayout() {
        return mActionControlView.getPlayReplayLayout();
    }

    /***
     * ????????????view
     *
     * @return View error layout
     */
    @Nullable
    public View getErrorLayout() {
        return mActionControlView.getExoPlayErrorLayout();
    }

    /***
     * ??????????????????view
     *
     * @return View ??????
     */
    @NonNull
    public View getGestureAudioLayout() {
        return mGestureControlView.getExoAudioLayout();
    }

    /***
     * ??????????????????view
     *
     * @return View gesture brightness layout
     */
    @NonNull
    public View getGestureBrightnessLayout() {
        return mGestureControlView.getExoBrightnessLayout();
    }

    /***
     * ??????????????????????????????view
     *
     * @return View gesture progress layout
     */
    @NonNull
    public View getGestureProgressLayout() {
        return mGestureControlView.getDialogProLayout();
    }

    /***
     * ????????????????????????
     *
     * @return boolean boolean
     */
    public boolean isListPlayer() {
        return isListPlayer;
    }

    /***
     * ??????????????????
     * @return boolean exo fullscreen
     */
    public AppCompatCheckBox getExoFullscreen() {
        return controllerView.getExoFullscreen();
    }

    /**
     * Gets switch text.
     *
     * @return the switch text
     */
    @NonNull
    public TextView getSwitchText() {
        return controllerView.getSwitchText();
    }

    /**
     * ??????g???????????????
     *
     * @return ExoUserPlayer play
     */
    @Nullable
    public ExoUserPlayer getPlay() {
        if (mExoPlayerListener == null) {
            return null;
        } else {
            return mExoPlayerListener.getPlay();
        }
    }

    /***
     * ???????????????
     *
     * @return ImageView preview image
     */
    @NonNull
    public ImageView getPreviewImage() {
        return exoPreviewImage;
    }

    /***
     * ??????????????????view
     *
     * @return SimpleExoPlayerView player view
     */
    @NonNull
    public ExoPlayerView getPlayerView() {
        return playerView;
    }

    /**
     * ???????????????
     *
     * @return ExoDefaultTimeBar time bar
     */
    @NonNull
    public ExoDefaultTimeBar getTimeBar() {
        return (ExoDefaultTimeBar) controllerView.getTimeBar();
    }

    /**
     * Sets the aspect ratio that this view should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(float widthHeightRatio) {
        getPlayerView().getAspectRatioFrameLayout().setAspectRatio(widthHeightRatio);
    }


    public boolean isNetworkNotify() {
        return networkNotify;
    }

    public void setNetworkNotify(boolean networkNotify) {
        this.networkNotify = networkNotify;
    }


    public void useWebDanmuku(boolean use, String url, int lineCount) {
        useWebDanmuku(use, url, lineCount, null);
    }

    public void useDanmuku(boolean use, File file, int lineCount) {
        useDanmuku(use, file, lineCount, null);
    }

    /**
     * ???????????????
     *
     * @param use
     * @param file
     */
    public void useDanmuku(boolean use, File file, int lineCount, ViewGroup viewContainer) {
        danmaKuShow = use;
        useDanmuWebView = false;
        if (use) {
            networkNotifyUseDialog = true;
            setDanmuViewContainer(viewContainer);
            danmuLineCount = lineCount;
            if (danmakuView != null) {
                if (danmakuView.isShown()) {
                    danmakuView.hide();
                }
                danmakuView.release();
                danmakuView.removeAllDanmakus(true);
                danmakuView = null;
                getDanmuViewContainer().removeView((DanmakuView) danmakuView);
                if (parser != null) {
                    parser.release();
                    parser = null;
                }
            }
            initDanmuku(lineCount);
            if (file != null) {
                createParser(getIsStream(file), file);
                onPrepareDanmaku();
                resolveDanmakuShow();
            }
        } else {
            if (danmakuView != null) {
                if (danmakuView.isShown()) {
                    danmakuView.hide();
                }
                danmakuView.release();
                danmakuView.removeAllDanmakus(true);
            }
        }
    }

    public void useWebDanmuku(boolean use, String url, int lineCount, ViewGroup viewContainer) {
        danmaKuShow = use;
        useDanmuWebView = true;
        if (use) {
            networkNotifyUseDialog = true;
            setDanmuViewContainer(viewContainer);
            danmuLineCount = lineCount;
            if (danmuWebView == null) {
                initDanmuWebView();
            }
            if (danmuWebView.getParent() != null) {
                ((ViewGroup) danmuWebView.getParent()).removeView(danmuWebView);
            }
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDanmuViewContainer().addView(danmuWebView, params);
            danmuWebView.onResume();
            danmuWebView.loadUrl(url);
            postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getPlayerView().getPlayer() != null) {
                    danmuWebView.evaluateJavascript("window.isPlaying = " + getPlayerView().getPlayer().isPlaying() +
                            ";\nwindow.lineCount = " + lineCount, null);
                }
            }, 1000);
            postDelayed(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getPlayerView().getPlayer() != null) {
                    danmuWebView.evaluateJavascript("window.isPlaying = " + getPlayerView().getPlayer().isPlaying() +
                            ";\nwindow.lineCount = " + lineCount, null);
                }
            }, 3000);
        } else {
            if (danmuWebView != null) {
                danmuWebView.onPause();
                getDanmuViewContainer().removeView(danmuWebView);
            }
        }
    }

    private void initDanmuWebView() {
        danmuWebView = new DanmuWebView(getContext());
        WebSettings webSettings = danmuWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowContentAccess(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        danmuWebView.setBackgroundColor(0); // ???????????????
        if (danmuWebView.getBackground() != null) {
            danmuWebView.getBackground().setAlpha(0); // ????????????????????? ?????????0-255
        }
        if (getPlayerView().getPlayer() != null) {
            getPlayerView().getPlayer().addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (danmaKuShow && danmuWebView != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            danmuWebView.evaluateJavascript("window.isPlaying = " + isPlaying +
                                    ";\nwindow.lineCount = " + danmuLineCount, null);
                        }
                        if (isPlaying) {
                            danmuWebView.onResume();
                        } else {
                            danmuWebView.onPause();
                        }
                    }
                }
            });
        }
        addSeekListener();
    }

    private void addSeekListener() {
        getTimeBar().addListener(new TimeBar.OnScrubListener() {
            @Override
            public void onScrubStart(TimeBar timeBar, long position) {

            }

            @Override
            public void onScrubMove(TimeBar timeBar, long position) {

            }

            @Override
            public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                Log.d(TAG, "xxxxxx-onScrubStop: ");
                resolveDanmakuSeek(position);
            }
        });
    }


    private void onPrepareDanmaku() {
        if (danmakuView != null) {
            danmakuView.prepare(parser, danmakuContext);
        }
    }

    private InputStream getIsStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initDanmuku(int lineCount) {
        if (danmakuView != null) {
            return;
        }
        // ????????????????????????
        HashMap<Integer, Integer> maxLinesPair = new HashMap<Integer, Integer>();
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, lineCount); // ????????????????????????5???
        // ????????????????????????
        HashMap<Integer, Boolean> overlappingEnablePair = new HashMap<Integer, Boolean>();
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true);
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        danmakuView = new DanmakuView(getContext());
//        danmakuView.showFPS(true);
        getDanmuViewContainer().addView((DanmakuView) danmakuView, params);
        DanamakuAdapter danamakuAdapter = new DanamakuAdapter(danmakuView);
        danmakuContext = DanmakuContext.create();
        danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3)
                .setScrollSpeedFactor(1.2f)
                .setCacheStuffer(new SpannedCacheStuffer(), danamakuAdapter) // ??????????????????SpannedCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair);
        addSeekListener();
        danmuDestroyed = false;
        if (danmakuView != null) {
            danmakuView.setCallback(new master.flame.danmaku.controller.DrawHandler.Callback() {
                @Override
                public void updateTimer(DanmakuTimer timer) {
                    if (playerView != null && VideoPlayerManager.PLAY_SPEED > 1f || VideoPlayerManager.tempFastPlay) {
                        float speed = VideoPlayerManager.tempFastPlay ? VideoPlayerManager.PLAY_SPEED * 2 : VideoPlayerManager.PLAY_SPEED;
                        if (speed > 1f) {
                            timer.add((long) (timer.lastInterval() * (speed - 1)));
                        }
                    }
                }

                @Override
                public void drawingFinished() {

                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
                }

                @Override
                public void prepared() {
                    if (danmakuView != null) {
                        post(() -> {
                            if (playerView.getPlayer() != null && playerView.getPlayer().getCurrentPosition() > 0) {
                                Log.d(TAG, "xxxxxx-prepared: ");
                                if (danmakuView != null) {
                                    danmakuView.start(getPlayerView().getPlayer().getCurrentPosition());
                                }
                            } else {
                                danmakuView.start();
                            }
                            if (!playerView.getPlayer().isPlaying()) {
                                danmakuView.pause();
                            }
                            resolveDanmakuShow();
                        });
                    }
                }
            });
            danmakuView.enableDanmakuDrawingCache(true);
            if (getPlayerView().getPlayer() != null) {
                getPlayerView().getPlayer().addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        if (danmaKuShow && danmakuView != null) {
                            if (isPlaying) {
                                danmakuView.resume();
                            } else {
                                danmakuView.pause();
                            }
                        }
                    }
                });
            }
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     */
    public void resumeDanmu() {
        if (danmaKuShow && danmakuView != null && getPlayerView().getPlayer() != null) {
            danmakuView.resume();
        }
        if (danmaKuShow && danmuWebView != null && getPlayerView().getPlayer() != null) {
            danmuWebView.onResume();
        }
        if (playerView.getPlayer() != null && playerView.getPlayer().getCurrentPosition() > 0) {
            Log.d(TAG, "xxxxxx-prepared: ");
            resolveDanmakuSeek(playerView.getPlayer().getCurrentPosition());
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     */
    public void pauseDanmu() {
        if (danmaKuShow && danmakuView != null && getPlayerView().getPlayer() != null) {
            danmakuView.pause();
        }
        if (danmaKuShow && danmuWebView != null && getPlayerView().getPlayer() != null) {
            danmuWebView.onPause();
        }
    }

    /**
     * ????????????????????????
     */
    private void resolveDanmakuShow() {
        post(() -> {
            if (danmakuView == null) {
                return;
            }
            if (danmaKuShow) {
                if (!danmakuView.isShown()) {
                    danmakuView.show();
                    if (danmakuView.isPaused()) {
                        danmakuView.resume();
                    }
                }
            } else {
                if (danmakuView.isShown()) {
                    danmakuView.hide();
                }
            }
        });
    }

    /**
     * ????????????
     */
    private void resolveDanmakuSeek(long time) {
        if (!useDanmuWebView && danmakuView != null && danmakuView.isPrepared()) {
            //??????????????????????????????bug????????????????????????????????????????????????????????????
            postDelayed(() -> {
                if (!danmuDestroyed && danmakuView != null && getPlayerView().getPlayer() != null) {
                    danmakuView.seekTo(getPlayerView().getPlayer().getCurrentPosition());
                }
            }, 1000);
        }
        if (useDanmuWebView && danmuWebView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                danmuWebView.evaluateJavascript("window.seek = " + time, null);
            }
        }
    }

    /**
     * ???????????????????????????????????????
     *
     * @param stream
     * @return
     */
    private void createParser(InputStream stream, File file) {
        if (stream == null) {
            parser = isJson(file) ? new JSONDanmukuParser() {
                @Override
                public IDanmakus parse() {
                    return new Danmakus();
                }
            } : new BiliDanmukuParser() {
                @Override
                public Danmakus parse() {
                    return new Danmakus();
                }
            };
        }
        parser = isJson(file) ? new JSONDanmukuParser() : new BiliDanmukuParser();
        loadDanmuStream(stream, file);
    }

    private boolean isJson(File file) {
        return file.getAbsolutePath().endsWith(".json");
    }

    private void loadDanmuStream(InputStream stream, File file) {
        if (parser == null || stream == null) {
            return;
        }
        ILoader loader = DanmakuLoaderFactory.create(isJson(file) ? DanmakuLoaderFactory.TAG_ACFUN : DanmakuLoaderFactory.TAG_BILI);
        try {
            loader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        IDataSource<?> dataSource = loader.getDataSource();
        parser.load(dataSource);
    }

    public void seekFromPlayer(long pos) {
        Log.d(TAG, "xxxxxx-seekFromPlayer: ");
        resolveDanmakuSeek(pos);
    }

    public void updateDanmuLines(int lineCount) {
        if (danmakuView != null && danmakuContext != null) {
            HashMap<Integer, Integer> maxLinesPair = new HashMap<>();
            maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_LR, lineCount);
            danmakuContext.setMaximumLines(maxLinesPair);
        }
    }
}
