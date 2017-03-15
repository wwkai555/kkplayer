package com.yi.player.widget;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.yi.player.R;
import com.yi.player.databinding.WidgetYiPlayerSeekbarBinding;
import com.yi.player.util.TimeUtil;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;


/**
 * Created by kevin on 17-3-13.
 */

public class YiPlayerSeekBar extends FrameLayout implements SeekBar.OnSeekBarChangeListener {
    private static String TAG = "YiPlayerSeekBar";
    private WidgetYiPlayerSeekbarBinding seekbarBinding;
    private BehaviorSubject<Pair<Long, Boolean>> timeSubject = BehaviorSubject.create();

    public YiPlayerSeekBar(@NonNull Context context) {
        this(context, null);
    }

    public YiPlayerSeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public YiPlayerSeekBar(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        seekbarBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.widget_yi_player_seekbar, null, false);
        seekbarBinding.setPlayTime(0L);
        seekbarBinding.setAllTime(0L);
        seekbarBinding.seekBar.setOnSeekBarChangeListener(this);
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(seekbarBinding.getRoot(), params);
    }

    public void setPlayTime(long time) {
        seekbarBinding.setPlayTime(time);
    }

    public void setAllTime(long time) {
        seekbarBinding.setAllTime(time);
    }

    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.d(TAG, "fromUser : " + fromUser);
        timeSubject.onNext(new Pair<>((long) progress, fromUser));
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override public void onStopTrackingTouch(SeekBar seekBar) {

    }

    public Observable<Pair<Long, Boolean>> timeObservable() {
        return timeSubject;
    }

    public void setCompoundEnabled(boolean compoundEnabled) {
        seekbarBinding.seekBar.setEnabled(compoundEnabled);
    }

    public void setSeekBackground(Drawable seekBackground) {
    }

    public void complete() {
        setPlayTime(seekbarBinding.seekBar.getMax());
    }

    public void reset() {
        setPlayTime(0);
    }

    @BindingAdapter("timeFormat")
    public static void timeFormat(TextView textView, long time) {
        textView.setText(TimeUtil.format(time));
    }

    @BindingAdapter({"seekProgress", "max"})
    public static void seekProgress(SeekBar seekBar, long playTime, long allTime) {
        seekBar.setMax((int) allTime);
        seekBar.setProgress(TimeUtil.progress(playTime, allTime));
    }

}
