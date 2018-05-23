package com.example.android.mediaplayersample;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


class SongsAdapter extends RecyclerView.Adapter <SongsAdapter.SongViewHolder>{

    private OnPlay onPlay;
    private LayoutInflater layoutInflater;
    private String [] songTitles;
    private String [] songAuthors;
    private String []songDurations;
    private static Context context;

    SongsAdapter(Context context, String [] songTitles,
                 String [] songAuthors, String [] songDurations){
        layoutInflater=LayoutInflater.from(context);
        this.songTitles = songTitles;
        this.songAuthors = songAuthors;
        this.songDurations = songDurations;
        this.context = context;
    }


    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.song_row, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SongViewHolder holder, final int position) {
        holder.songTitle.setText(songTitles[position]);
        holder.songAuthor.setText(songAuthors[position]);
        holder.songDuration.setText(songDurations[position]);
        holder.playSong.setOnClickListener(v -> onPlay.onClick(position));
    }

    @Override
    public int getItemCount() {
        return songTitles.length;
    }

    void setOnPlay(OnPlay onPlay){
        this.onPlay= onPlay;
    }

    static class SongViewHolder extends RecyclerView.ViewHolder{

        private TextView songTitle, songAuthor, songDuration;
        private ImageButton playSong;


        SongViewHolder(View itemView) {
            super(itemView);
            songTitle    = itemView.findViewById(R.id.songTitle);
            songAuthor   = itemView.findViewById(R.id.songAuthor);
            songDuration = itemView.findViewById(R.id.songDuration);
            playSong     = itemView.findViewById(R.id.play_selected);
        }
    }
}
