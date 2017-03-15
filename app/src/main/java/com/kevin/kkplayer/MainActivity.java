package com.kevin.kkplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.yi.player.YiPlayerActivity;

public class MainActivity extends AppCompatActivity {
    String dcim = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM";
    private String path = dcim + "/h.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void play(View view) {
        Intent intent = new Intent(this, YiPlayerActivity.class);
        intent.putExtra(YiPlayerActivity.VIDEO_PATH, path);
        startActivity(intent);
    }
}