package com.yi.player;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.yi.player.databinding.ActivityYiPlayerBinding;
import com.yi.player.util.ScreenUtil;
import com.yi.player.widget.YiPlayerView;

import java.io.File;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


public class YiPlayerActivity extends FragmentActivity {
    public static final String TAG = YiPlayerActivity.class.getName();
    public static final String VIDEO_PATH = "video_player_path";

    private ActivityYiPlayerBinding yiPlayerBinding;
    private Disposable eventSubscription;
    protected String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        yiPlayerBinding = DataBindingUtil.setContentView(this, R.layout.activity_yi_player);
        path = getIntent().getStringExtra(VIDEO_PATH);
        yiPlayerBinding.setShowError(false);
        yiPlayerBinding.setShowOtherPlayer(false);
        yiPlayerBinding.videoPlayer.setDataSource(path);
        eventSubscription = yiPlayerBinding.videoPlayer.eventObservable().subscribe(new Consumer<YiPlayerView.PlayerEvent>() {
            @Override public void accept(@NonNull YiPlayerView.PlayerEvent playerEvent) throws Exception {
                if (playerEvent == YiPlayerView.PlayerEvent.SCALE) {
                    doScreenChange();
                } else if (playerEvent == YiPlayerView.PlayerEvent.ERROR) {
                    String msg;
                    yiPlayerBinding.setShowError(true);
                    Log.d(TAG, "Error value : " + playerEvent.getValue());
                    switch (Integer.parseInt(playerEvent.getValue())) {
                        case YiPlayerView.VIDEO_ERROR:
                        case MediaPlayer.MEDIA_ERROR_MALFORMED:
                            msg = getString(R.string.video_malformed);
                            break;
                        case -2147483648:
                        case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                            msg = getString(R.string.video_unsupported);
                            yiPlayerBinding.setShowOtherPlayer(true);
                            break;
                        case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                            msg = getString(R.string.video_time_out);
                            break;
                        default:
                            msg = getString(R.string.video_unknown_error);
                            break;
                    }
                    yiPlayerBinding.setErrorMsg(msg);
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged orientation = " + newConfig.orientation);
        yiPlayerBinding.videoPlayer.notifyScreenChange();
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) yiPlayerBinding.videoPlayer.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ScreenUtil.hideSystemUI(this);
            layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
            yiPlayerBinding.videoPlayer.setSeekBarHeight((int) getResources().getDimension(R.dimen.seekbar_landspace));
            yiPlayerBinding.videoPlayer.setLayoutStyle(getResources().getInteger(R.integer.style_overlay));
        } else {
            ScreenUtil.showSystemUI(this);
            layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
            yiPlayerBinding.videoPlayer.setSeekBarHeight((int) getResources().getDimension(R.dimen.seekbar_protrait));
            yiPlayerBinding.videoPlayer.setLayoutStyle(getResources().getInteger(R.integer.style_lay));
        }
    }

    public void onPlayerClick(View v) {
        yiPlayerBinding.videoPlayer.prePlayOrPause();
    }

    public void onBackClick(View view) {
        onBackPressed();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (ScreenUtil.isLandSpace(this)) ScreenUtil.hideSystemUI(this);
            else ScreenUtil.showSystemUI(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
        outState.putInt("progress", yiPlayerBinding.videoPlayer.getCurrentPlayProgress());
        outState.putBoolean("isPlaying", yiPlayerBinding.videoPlayer.isPlaying());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
        if (savedInstanceState != null && yiPlayerBinding.videoPlayer != null) {
            yiPlayerBinding.videoPlayer.updateProgress(savedInstanceState.getInt("progress", -1));
            yiPlayerBinding.videoPlayer.setIsAutoPlay(savedInstanceState.getBoolean("isPlaying"));
        }
    }

    @Override
    public void onBackPressed() {
        if (ScreenUtil.isLandSpace(this)) doScreenChange();
        else finish();
    }

    /**
     * 进行横竖屏切换时 数据处理
     */
    public void doScreenChange() {
        if (ScreenUtil.isLandSpace(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public void switchPlayer(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        File file = new File(path);
        intent.setDataAndType(Uri.fromFile(file), "video/*");
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        yiPlayerBinding.videoPlayer.setIsAutoPlay(yiPlayerBinding.videoPlayer.isPlaying());
        yiPlayerBinding.videoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        yiPlayerBinding.videoPlayer.destroy();
        if (eventSubscription != null)
            eventSubscription.dispose();
    }
}
