/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.mediaplayersample;

import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity
                                implements OnPlay{

    public static final String TAG = "MainActivity";
    private static final int ONE_SECOND = 1000; //milliseconds
    private static final int ONE_MINUTE = 60000;//milliseconds
    public static final int MEDIA_RES_ID = R.raw.jazz_in_paris;
    private String [] songTitles;
    private String [] songAuthors;
    private String [] songDurations;

    private RecyclerView mRecyclerView;
    private SongsAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private SeekBar mSeekbarAudio;
    private PlayerAdapter mPlayerAdapter;
    private boolean mUserIsSeeking = false;
    MediaMetadataRetriever mmr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        initializeSeekbar();
        initializePlaybackController();
        getSongsData();
        initializeRecView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPlayerAdapter.loadMedia(MEDIA_RES_ID);
        Log.d(TAG, "onStart: create MediaPlayer");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isChangingConfigurations() && mPlayerAdapter.isPlaying()) {
            Log.d(TAG, "onStop: don't release MediaPlayer as screen is rotating & playing");
        } else {
            mPlayerAdapter.release();
            Log.d(TAG, "onStop: release MediaPlayer");
        }
    }

    private void initializeUI() {
        ImageButton mPlayButton = (ImageButton) findViewById(R.id.playSong);
      //  Button mPauseButton = (Button) findViewById(R.id.button_pause);
      //  Button mResetButton = (Button) findViewById(R.id.button_reset);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);

       /* mPauseButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.pause();
                    }
                });*/
        mPlayButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.play();
                    }
                });
        /*mResetButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPlayerAdapter.reset();
                    }
                });*/
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        Log.d(TAG, "initializePlaybackController: created MediaPlayerHolder");
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
        Log.d(TAG, "initializePlaybackController: MediaPlayerHolder progress callback set");
    }

    private void initializeSeekbar() {
        mSeekbarAudio.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int userSelectedPosition = 0;

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = true;
                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            userSelectedPosition = progress;
                        }
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    private void getSongsData(){
        Field[] fields=R.raw.class.getFields();
        songTitles = new String[fields.length];
        songAuthors = new String[fields.length];
        songDurations = new String[fields.length];
        mmr = new MediaMetadataRetriever();

        for (int i = 0; i < fields.length; i++) {

            final String uriPath="android.resource://"+getPackageName()+"/raw/"+
                    fields[i].getName();
            final Uri uri=Uri.parse(uriPath);
            mmr.setDataSource(getApplication(),uri);


            songTitles[i] =
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            songAuthors[i] =
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            songDurations[i] =
                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        }

        mmr.release(); // all done, release the object
        transformMilliseconds();
    }

    private void initializeRecView(){
        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new SongsAdapter(this, songTitles,
                songAuthors, songDurations);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setOnPlay(this);

    }

    private void transformMilliseconds() {
        for (int i = 0; i < songDurations.length; i++) {
            int milliseconds = Integer.parseInt(songDurations[i]);
            int minutes = milliseconds / ONE_MINUTE;
            milliseconds -= minutes * ONE_MINUTE;
            int seconds = milliseconds / ONE_SECOND;
            StringBuilder formattedDuration = new StringBuilder(String.valueOf(minutes) + ":");
            if (seconds < 10)
                formattedDuration.append("0" + seconds);
            else
                formattedDuration.append(seconds);
            songDurations[i] = formattedDuration.toString();

        }
    }

    @Override
    public void onClick(int position) {
        ((TextView)findViewById(R.id.currentTitle)).setText(songTitles[position]);
        findViewById(R.id.currentTitle).setVisibility(View.VISIBLE);
        findViewById(R.id.seekbar_audio).setVisibility(View.VISIBLE);
    }

    public class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
            Log.d(TAG, String.format("setPlaybackDuration: setMax(%d)", duration));
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
                Log.d(TAG, String.format("setPlaybackPosition: setProgress(%d)", position));
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
            onLogUpdated(String.format("onStateChanged(%s)", stateToString));
        }

        @Override
        public void onPlaybackCompleted() {
        }

    }
}