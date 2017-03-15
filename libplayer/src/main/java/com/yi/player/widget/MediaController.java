package com.yi.player.widget;

import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * Created by wang.wenkai on 2016/5/23.
 */

public class MediaController implements MediaPlayer.OnCompletionListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnPreparedListener
        , MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {
    public static final int UPDATE_RANGE = 100;//ms 更新播放进度幅度
    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_STOP = 6;
    public static final int STATE_PLAYBACK_COMPLETED = 5;

    private MediaPlayer mediaPlayer;
    private MediaControllerListener controllerListener;
    private android.os.Handler handler = new android.os.Handler();

    private int mCurrentState = STATE_IDLE;

    public MediaController() {
        this.mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnVideoSizeChangedListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setScreenOnWhilePlaying(true);
    }

    public void setControllerListener(@NonNull MediaControllerListener listener) {
        controllerListener = listener;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        controllerListener.onBufferingUpdate(percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("MediaController", "onCompletion isPlaying = " + mediaPlayer.isPlaying());
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        handler.removeCallbacks(checkProgress);
        if (controllerListener != null) {
            controllerListener.onComplete();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mCurrentState = STATE_IDLE;
        if (controllerListener != null) {
            controllerListener.onError(what, extra);
        }
        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mCurrentState = STATE_PREPARED;
        controllerListener.onPrepared(mp.getDuration());
        play();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d("", "onSeekComplete isPlaying = " + mp.isPlaying());
        controllerListener.onSeekComplete();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        controllerListener.onVideoSizeChanged(width, height);
    }

    public void setHolder(SurfaceHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.setDisplay(holder);
        }
    }

    /**
     * 设置视频链接
     *
     * @param path
     */
    public void setDataSource(String path) {
        if (mCurrentState != STATE_IDLE || mediaPlayer == null) {
            return;
        }
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            onError(null, -1, STATE_ERROR);
        }
    }

    public void play() {
        if (mCurrentState == STATE_IDLE || mCurrentState == STATE_STOP || mCurrentState == STATE_PLAYING || mediaPlayer == null)
            return;
        mCurrentState = STATE_PLAYING;
        mediaPlayer.start();
        controllerListener.onPlay();
        handler.postDelayed(checkProgress, 500);
    }

    public void pause() {
        if (mediaPlayer == null || mCurrentState == STATE_IDLE) return;
        mCurrentState = STATE_PAUSED;
        mediaPlayer.pause();
        controllerListener.onPause();
        handler.removeCallbacks(checkProgress);
    }

    public void seekTo(int currentPlayProgress, int allTime) {
        if (mCurrentState != STATE_IDLE && currentPlayProgress <= allTime) {
            mediaPlayer.seekTo(currentPlayProgress);
        }
    }

    public void seekTo(int currentPlayProgress) {
        if (mCurrentState != STATE_IDLE) {
            mediaPlayer.seekTo(currentPlayProgress);
        }
    }

    public void stop() {
        if (mediaPlayer == null || mCurrentState == STATE_IDLE) {
            return;
        }
        mCurrentState = STATE_STOP;
        mediaPlayer.stop();
    }

    public void destroy() {
        if (mediaPlayer == null) {
            return;
        }
        mCurrentState = STATE_IDLE;
        mediaPlayer.reset();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    /**
     * 用于获取当前播放进度，刷新进度条
     */
    private Runnable checkProgress = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                controllerListener.onPlayingProgress(mediaPlayer.getCurrentPosition());
                handler.postDelayed(checkProgress, UPDATE_RANGE);
            }
        }
    };

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void seekPlay(int currentPlayProgress) {
        seekTo(currentPlayProgress);
        play();
    }

    public interface MediaControllerListener {
        void onPrepared(int videoDuration);

        void onComplete();

        void onPlay();

        void onPause();

        void onError(int errorType, int extra);

        void onSeekComplete();

        void onPlayingProgress(int progress);

        void onVideoSizeChanged(int width, int height);

        void onBufferingUpdate(int percent);

    }
}
