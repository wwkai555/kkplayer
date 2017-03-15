package com.yi.player.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.yi.player.R;
import com.yi.player.databinding.WidgetYiPlayerBinding;
import com.yi.player.util.ScreenUtil;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;


/**
 * Created by kevin on 17-3-13.
 */

public class YiPlayerView extends LinearLayout implements SurfaceHolder.Callback {
    public static final String TAG = YiPlayerView.class.getName();
    public static final int VIDEO_ERROR = -1002;

    private int currentPlayProgress;
    private boolean isAutoPlay = true;
    private Pair<Integer, Integer> wh;
    protected MediaController controller;
    private WidgetYiPlayerBinding yiPlayerBinding;
    private Disposable timeSubscription;

    public YiPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YiPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.widget_yi_player, null);
        yiPlayerBinding = DataBindingUtil.bind(view);
        yiPlayerBinding.setPlayer(this);
        yiPlayerBinding.setShowPause(false);
        yiPlayerBinding.setPlayStatus(false);
        yiPlayerBinding.setAssistVisibility(true);
        yiPlayerBinding.surfaceView.getLayoutParams().height = (int) (ScreenUtil.getScreenWidth(context) * 0.5625);
        yiPlayerBinding.surfaceView.getHolder().addCallback(this);
        initAttr(attrs, view);
        controller = new MediaController();
        controller.setControllerListener(new ControlListener());
        timeSubscription = yiPlayerBinding.yiSeekBar.timeObservable().subscribe(new Consumer<Pair<Long, Boolean>>() {
            @Override public void accept(@io.reactivex.annotations.NonNull Pair<Long, Boolean> longBooleanPair) throws Exception {
                currentPlayProgress = longBooleanPair.first.intValue();
                eventSubject.onNext(PlayerEvent.SEEK.setValue(String.valueOf(longBooleanPair.first)));
            }
        });
    }

    private void initAttr(AttributeSet attrs, View view) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.YiPlayerView);
        int style = typedArray.getInteger(R.styleable.YiPlayerView_style, getResources().getInteger(R.integer.style_lay));
        setLayoutStyle(style);
        typedArray.recycle();
        blockControlClick();
        addView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void setLayoutStyle(int style) {
        RelativeLayout.LayoutParams seekParams = (RelativeLayout.LayoutParams) yiPlayerBinding.yiSeekBar.getLayoutParams();
        if (style == getResources().getInteger(R.integer.style_lay)) {
            seekParams.addRule(RelativeLayout.ALIGN_BOTTOM, 0);
            seekParams.addRule(RelativeLayout.BELOW, yiPlayerBinding.surfaceView.getId());
        } else {
            seekParams.addRule(RelativeLayout.BELOW, 0);
            seekParams.addRule(RelativeLayout.ALIGN_BOTTOM, yiPlayerBinding.surfaceView.getId());
        }
        yiPlayerBinding.yiSeekBar.setLayoutParams(seekParams);
    }

    public void setSeekBarHeight(int controlBarHeight) {
        yiPlayerBinding.setSeekBarHeight(controlBarHeight);
    }

    public void setDataSource(@NonNull final String source) {
        if (TextUtils.isEmpty(source) || controller == null) return;
        controller.setDataSource(source);
    }

    /**
     * 在 mediaplayer没有prepare完成之前，禁止用户点击操作
     */
    protected void blockControlClick() {
        Log.d(TAG, "blockControlClick");
        yiPlayerBinding.imgPlay.setEnabled(false);
        yiPlayerBinding.yiSeekBar.setCompoundEnabled(false);
    }

    /**
     * mediaplayer prepare完成之后，恢复控件操作
     */
    protected void resetControlClick() {
        Log.d(TAG, "resetControlClick");
        yiPlayerBinding.imgPlay.setEnabled(true);
        yiPlayerBinding.yiSeekBar.setCompoundEnabled(true);
    }

    protected void setPlayerStatus(boolean is) {
        yiPlayerBinding.setPlayStatus(is);
        yiPlayerBinding.setAssistVisibility(!is);
        if (is) eventSubject.onNext(PlayerEvent.PLAYING);
        else {
            eventSubject.onNext(PlayerEvent.PAUSE);
            yiPlayerBinding.setShowPause(false);
        }
    }

    protected void setErrorStatus(int errorType, int extra) {
        Log.d(TAG, "setErrorStatus");
        blockControlClick();
        yiPlayerBinding.setPlayStatus(true);
        eventSubject.onNext(PlayerEvent.ERROR.setValue(String.valueOf(extra)));
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated currentPlayProgress = " + currentPlayProgress);
        yiPlayerBinding.surfaceView.setKeepScreenOn(true);
        holder.addCallback(this);
        if (controller != null) controller.setHolder(holder);
        if (isAutoPlay) play();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged width = " + width + " height = " + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (controller != null) controller.setHolder(null);
        Log.d(TAG, "surfaceDestroyed");
    }

    /**
     * 动态改变surface的宽高，防止被拉伸
     *
     * @param videoWidth
     * @param videoHeight
     */
    private void initSurfaceLayout(int videoWidth, int videoHeight) {
        int screenWidth = ScreenUtil.getScreenWidth(getContext());
        int screenHeight = ScreenUtil.getScreenHeight(getContext());
        float videoRatio = videoHeight / (float) videoWidth;
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) yiPlayerBinding.surfaceView.getLayoutParams();
        float wRatio;
        float hRatio;
        float ratio;
        if (ScreenUtil.isLandSpace(getContext())) {
            screenWidth += ScreenUtil.getNavigationBarHeight(getContext());
            Log.d(TAG, "initSurfaceLayout -- ORIENTATION_LANDSCAPE");
            if (videoWidth > screenWidth || videoHeight > screenHeight) {
                wRatio = videoWidth / (float) screenWidth;
                hRatio = videoHeight / (float) screenHeight;
                ratio = Math.max(wRatio, hRatio);
                videoWidth = (int) Math.ceil(videoWidth / ratio);
                videoHeight = (int) Math.ceil(videoHeight / ratio);
                params.height = videoHeight;
                params.width = videoWidth;
            } else {
                params.height = screenHeight;
                params.width = (int) (screenHeight * (videoWidth / (float) videoHeight));
            }
        } else {
            Log.d(TAG, "initSurfaceLayout -- ORIENTATION_PORT");
            if (videoWidth > screenWidth || videoHeight > screenWidth * 0.75f) {
                wRatio = videoWidth / (float) screenWidth;
                hRatio = videoHeight / (screenWidth * 0.75f);
                ratio = Math.max(wRatio, hRatio);
                videoWidth = (int) Math.ceil(videoWidth / ratio);
                videoHeight = (int) Math.ceil(videoHeight / ratio);
                params.height = videoHeight;
                params.width = videoWidth;
            } else {
                params.width = screenWidth;
                params.height = (int) (screenWidth * videoRatio);
            }
        }
    }

    public void updateProgress(int progress) {
        this.currentPlayProgress = progress;
    }

    public int getCurrentPlayProgress() {
        return currentPlayProgress;
    }

    public void onPlayClick() {
        if (isPlaying()) pause();
        else play();
    }

    public void prePlayOrPause() {
        yiPlayerBinding.setAssistVisibility(!yiPlayerBinding.getAssistVisibility());
        if (isPlaying()) yiPlayerBinding.setShowPause(!yiPlayerBinding.getShowPause());
        else yiPlayerBinding.setShowPause(false);
    }

    public void onScaleClick() {
        eventSubject.onNext(PlayerEvent.SCALE);
    }

    public void play() {
        Log.d(TAG, "------ play curr progress : " + currentPlayProgress);
        if (controller != null) controller.seekPlay(currentPlayProgress);
    }

    public void pause() {
        Log.d(TAG, "------ pause");
        if (controller != null) controller.pause();
    }

    public void stop() {
        if (controller != null) controller.stop();
    }

    public void destroy() {
        if (timeSubscription != null) timeSubscription.dispose();
        if (controller != null) controller.destroy();
    }

    /**
     * 判断当前是否处于播放状态
     */
    public boolean isPlaying() {
        return controller != null && controller.isPlaying();
    }

    /**
     * 横竖屏切换 调整布局
     */
    public void notifyScreenChange() {
        if (wh == null || wh.first <= 0 || wh.second <= 0) {
            setErrorStatus(-1, VIDEO_ERROR);
            return;
        }
        initSurfaceLayout(wh.first, wh.second);
        yiPlayerBinding.surfaceView.requestLayout();
    }

    public void setIsAutoPlay(boolean isAutoPlay) {
        this.isAutoPlay = isAutoPlay;
    }

    private class ControlListener implements MediaController.MediaControllerListener {
        @Override
        public void onPrepared(int videoDuration) {
            Log.d(TAG, "onPrepared Video Duration = " + videoDuration);
            yiPlayerBinding.yiSeekBar.setAllTime(videoDuration);
            resetControlClick();
            if (isAutoPlay) play();
        }

        @Override
        public void onComplete() {
            setPlayerStatus(false);
//            yiPlayerBinding.yiSeekBar.complete();
            yiPlayerBinding.yiSeekBar.reset();
        }

        @Override
        public void onPlay() {
            setPlayerStatus(true);
        }

        @Override
        public void onPause() {
            setPlayerStatus(false);
        }

        @Override
        public void onError(int errorType, int extra) {
            setErrorStatus(errorType, extra == MediaController.STATE_ERROR ? VIDEO_ERROR : extra);
        }

        @Override
        public void onSeekComplete() {

        }

        @Override
        public void onBufferingUpdate(int percent) {
            Log.d(TAG, "onBufferingUpdate : " + percent);
        }

        @Override
        public void onPlayingProgress(int progress) {
            Log.d(TAG, "onPlayingProgress Progress = " + progress);
            currentPlayProgress = progress;
            yiPlayerBinding.yiSeekBar.setPlayTime(progress);
        }

        @Override
        public void onVideoSizeChanged(int width, int height) {
            Log.d(TAG, "onVideoSizeChanged width = " + width + " height = " + height);
            wh = new Pair<>(width, height);
            notifyScreenChange();
        }
    }

    public enum PlayerEvent {
        PLAYING, PAUSE, ERROR, SCALE, SEEK;
        String value;

        PlayerEvent setValue(String value) {
            this.value = value;
            return this;
        }

        public String getValue() {
            return value;
        }
    }

    private BehaviorSubject<PlayerEvent> eventSubject = BehaviorSubject.createDefault(PlayerEvent.PAUSE);

    public Observable<PlayerEvent> eventObservable() {
        return eventSubject;
    }

    @BindingAdapter("seekbar_height")
    public static void setSeekBarHeight(@NonNull YiPlayerSeekBar seekBar, Integer height) {
        if (height == null) return;
        seekBar.getLayoutParams().height = height;
    }

    @BindingAdapter("aniVisibility")
    public static void setAniVisibility(@NonNull View view, Boolean visibility) {
        view.animate().alpha(visibility ? 1 : 0).setDuration(200).start();
    }

}
