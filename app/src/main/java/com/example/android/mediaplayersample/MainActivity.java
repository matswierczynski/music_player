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

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaMetadataRetriever;
import android.media.audiofx.AudioEffect;
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
import java.util.Random;

/**
 * Allows playback of a single MP3 file via the UI. It contains a {@link MediaPlayerHolder}
 * which implements the {@link PlayerAdapter} interface that the activity uses to control
 * audio playback.
 */
public final class MainActivity extends AppCompatActivity
                                implements OnPlay{

    public static final String TAG = "MainActivity";
    private Field[] fields;
    private static final int ONE_SECOND = 1000; //milliseconds
    private static final int ONE_MINUTE = 60000;//milliseconds
    private static final int SECONDS_TO_SCROLL = 10; //no of seconds to forward / rewind song
    private final static float BUTTON_SIZE_LANDSCAPE = 30.0f; //dp
    private final static float BUTTON_SIZE_PORTRAIT = 48.0f; //dp
    private int currentSongSelected;
    private String [] songTitles;
    private String [] songAuthors;
    private String [] songDurations;

    private RecyclerView mRecyclerView;
    private SongsAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private static SeekBar mSeekbarAudio;
    private static PlayerAdapter mPlayerAdapter;
    private static boolean mUserIsSeeking = false;
    private static MediaMetadataRetriever mmr;

    public static void startEqualizer(Context context) {
        Intent starter = new Intent(AudioEffect
                .ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);

        if ((starter.resolveActivity(context.getPackageManager()) != null)) {
            context.startActivity(starter);
        } else {
            // No equalizer found :(
        }
    }

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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ImageButton playSong = findViewById(R.id.playSong);
        ImageButton fastRewind = findViewById(R.id.fast_rewind);
        ImageButton fastForward = findViewById(R.id.fast_forward);

        final float scale = getResources().getDisplayMetrics().density;
        float dps;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            dps = BUTTON_SIZE_LANDSCAPE;
        else
            dps = BUTTON_SIZE_PORTRAIT;
        int pixels = (int) (dps * scale + 0.5f);
        fastForward.getLayoutParams().height = pixels;
        fastForward.getLayoutParams().width = pixels;
        playSong.getLayoutParams().height = pixels;
        playSong.getLayoutParams().width = pixels;
        fastRewind.getLayoutParams().height = pixels;
        fastRewind.getLayoutParams().width = pixels;

    }



    private void initializeUI() {
        ImageButton mPlayButton = (ImageButton) findViewById(R.id.playSong);
        ImageButton mRewindButton = (ImageButton) findViewById(R.id.fast_rewind);
        ImageButton mForwardButton = (ImageButton) findViewById(R.id.fast_forward);
        mSeekbarAudio = (SeekBar) findViewById(R.id.seekbar_audio);

        mPlayButton.setOnClickListener(
                v -> handleMusicPlaying()
        );
        mRewindButton.setOnClickListener(
                v -> rewind()
        );
        mForwardButton.setOnClickListener(
                v -> forward()
        );
    }

    private void initializePlaybackController() {
        MediaPlayerHolder mMediaPlayerHolder = new MediaPlayerHolder(this);
        mMediaPlayerHolder.setPlaybackInfoListener(new PlaybackListener());
        mPlayerAdapter = mMediaPlayerHolder;
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
                            userSelectedPosition = progress;
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        mUserIsSeeking = false;
                        mPlayerAdapter.seekTo(userSelectedPosition);
                    }
                });
    }

    private void getSongsData(){
        fields=R.raw.class.getFields();
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

    private void handleMusicPlaying(){
        if (mPlayerAdapter.isPlaying()) {
            mPlayerAdapter.pause();
            ((ImageButton)findViewById(R.id.playSong)).
                    setImageResource(
                            R.drawable.ic_play_circle_outline_black_24dp);
            ((TextView)findViewById(R.id.currentTitle)).setVisibility(View.GONE);
            mSeekbarAudio.setVisibility(View.GONE);

        }
        else {
            mPlayerAdapter.play();
            ((ImageButton)findViewById(R.id.playSong)).
                    setImageResource(
                            R.drawable.ic_pause_circle_outline_black_24dp);
            ((TextView)findViewById(R.id.currentTitle)).setVisibility(View.VISIBLE);
            mSeekbarAudio.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onClick(int position) {
        currentSongSelected = position;
        ((TextView)findViewById(R.id.currentTitle)).setText(songTitles[position]);
        ((ImageButton)findViewById(R.id.fast_rewind)).setVisibility(View.VISIBLE);
        ((ImageButton)findViewById(R.id.playSong)).setVisibility(View.VISIBLE);
        ((ImageButton)findViewById(R.id.fast_forward)).setVisibility(View.VISIBLE);

        String filename = fields[position].getName();
        int id = getResources().getIdentifier(filename, "raw", getPackageName());
        mPlayerAdapter.reset();
        mPlayerAdapter.release();
        mPlayerAdapter.loadMedia(id);
        handleMusicPlaying();
    }

    private void shufflePlay(){
        Random random = new Random();
        int songNumber = random.nextInt(songTitles.length);
        while(currentSongSelected == songNumber )
            songNumber = random.nextInt(songTitles.length);
        onClick(songNumber);
    }

    private void standardPlay(){
        currentSongSelected++;
        currentSongSelected%=songTitles.length; //if next song number exceeds songs amount,
                                                //take first song
        onClick(currentSongSelected);
    }

    private void playNextSong(){

    }

    private void playPreviousSong(){

    }

    private void forward(){
        int currentSeekBarPos = mSeekbarAudio.getProgress();
        int seekPosition = currentSeekBarPos+SECONDS_TO_SCROLL*ONE_SECOND;
        if (seekPosition >= mSeekbarAudio.getMax())
            playNextSong();
        else {
            mSeekbarAudio.setProgress(seekPosition);
            mPlayerAdapter.seekTo(seekPosition);
        }
    }

    private void rewind(){
        int currentSeekBarPos = mSeekbarAudio.getProgress();
        int seekPosition = currentSeekBarPos-SECONDS_TO_SCROLL*ONE_SECOND;
        if (seekPosition <= 0)
            playPreviousSong();
        else {
            mSeekbarAudio.setProgress(seekPosition);
            mPlayerAdapter.seekTo(seekPosition);
        }
    }

    public static class PlaybackListener extends PlaybackInfoListener {

        @Override
        public void onDurationChanged(int duration) {
            mSeekbarAudio.setMax(duration);
        }

        @Override
        public void onPositionChanged(int position) {
            if (!mUserIsSeeking) {
                mSeekbarAudio.setProgress(position, true);
            }
        }

        @Override
        public void onStateChanged(@State int state) {
            String stateToString = PlaybackInfoListener.convertStateToString(state);
        }

        @Override
        public void onPlaybackCompleted() {
        }

    }
}