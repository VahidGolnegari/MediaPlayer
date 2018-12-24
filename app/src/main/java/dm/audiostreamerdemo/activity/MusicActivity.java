/*
 * This is the source code of DMAudioStreaming for Android v. 1.0.0.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright @Dibakar_Mistry(dibakar.ece@gmail.com), 2017.
 */
package dm.audiostreamerdemo.activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;



import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


import dm.audiostreamerdemo.R;
import dm.audiostreamerdemo.adapter.AdapterMusic;
import dm.audiostreamerdemo.network.MusicBrowser;
import dm.audiostreamerdemo.network.MusicLoaderListener;
import dm.audiostreamerdemo.service.MediaBrowserService;
import dm.audiostreamerdemo.slidinguppanel.SlidingUpPanelLayout;
import dm.audiostreamerdemo.util.MediaMetaData;
import dm.audiostreamerdemo.widgets.LineProgress;
import dm.audiostreamerdemo.widgets.PlayPauseView;
import dm.audiostreamerdemo.widgets.Slider;

public class MusicActivity extends AppCompatActivity implements  View.OnClickListener, Slider.OnValueChangedListener {

    private static final String TAG = MusicActivity.class.getSimpleName();
    private Context context;
    private ListView musicList;
    private AdapterMusic adapterMusic;

    private PlayPauseView btn_play;
    private ImageView image_songAlbumArt;
    private ImageView img_bottom_albArt;
    private ImageView image_songAlbumArtBlur;
    private TextView time_progress_slide;
    private TextView time_total_slide;
    private TextView time_progress_bottom;
    private TextView time_total_bottom;
    private RelativeLayout pgPlayPauseLayout;
    private LineProgress lineProgress;
    private Slider audioPg;
    private ImageView btn_backward;
    private ImageView btn_forward;
    private TextView text_songName;
    private TextView text_songAlb;
    private TextView txt_bottom_SongName;
    private TextView txt_bottom_SongAlb;

    private SlidingUpPanelLayout mLayout;
    private RelativeLayout slideBottomView;
    private boolean isExpand = false;

    //For  Implementation
    private MediaMetaData currentSong;
    private List<MediaMetaData> listOfSongs = new ArrayList<MediaMetaData>();

    //
    private MediaBrowserCompat mMediaBrowserCompat;
    private MediaControllerCompat mMediaControllerCompat;
    private MediaSubscribtionCallback IMediaSessionCallback;
    private boolean isMediaConnected = false;
    private int mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        this.context = MusicActivity.this;

        IMediaSessionCallback = new MediaSubscribtionCallback();

        mCurrentState =  PlaybackStateCompat.STATE_NONE;

        mMediaBrowserCompat = new MediaBrowserCompat(this, new ComponentName(this, MediaBrowserService.class),
                mMediaBrowserCompatConnectionCallback, null);
        mMediaBrowserCompat.connect();
//        configAudioStreamer();
        uiInitialization();
        loadMusicData();
    }



    @Override
    public void onBackPressed() {
        if (isExpand) {
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
            overridePendingTransition(0, 0);
            finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_forward:
                mMediaControllerCompat.getTransportControls().skipToNext();
                break;
            case R.id.btn_backward:
                mMediaControllerCompat.getTransportControls().skipToPrevious();
                break;
            case R.id.btn_play:
                    playPauseEvent(view);

                break;
        }
    }

    @Override
    public void onValueChanged(int value) {

    }

    private void notifyAdapter(MediaMetaData media) {
        adapterMusic.notifyPlayState(media);
    }

    private void playPauseEvent(View v) {
        if (mCurrentState == PlaybackStateCompat.STATE_PLAYING) {
            mMediaControllerCompat.getTransportControls().pause();
            ((PlayPauseView) v).Pause();
        } else {
            mMediaControllerCompat.getTransportControls().play();
            ((PlayPauseView) v).Play();
        }

    }

    private void playSong(MediaMetaData media) {


//        if (streamingManager != null) {
//            streamingManager.onPlay(media);
//            showMediaInfo(media);
//        }
    }

    private void showMediaInfo(MediaMetaData media) {
        currentSong = media;
        audioPg.setValue(0);
        audioPg.setMin(0);
        audioPg.setMax(Integer.valueOf(media.getMediaDuration()) * 1000);
        setMaxTime();
        loadSongDetails(media);
    }

    private void uiInitialization() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.app_name));

        btn_play = (PlayPauseView) findViewById(R.id.btn_play);
        image_songAlbumArtBlur = (ImageView) findViewById(R.id.image_songAlbumArtBlur);
        image_songAlbumArt = (ImageView) findViewById(R.id.image_songAlbumArt);
        img_bottom_albArt = (ImageView) findViewById(R.id.img_bottom_albArt);
        btn_backward = (ImageView) findViewById(R.id.btn_backward);
        btn_forward = (ImageView) findViewById(R.id.btn_forward);
        audioPg = (Slider) findViewById(R.id.audio_progress_control);
        pgPlayPauseLayout = (RelativeLayout) findViewById(R.id.pgPlayPauseLayout);
        lineProgress = (LineProgress) findViewById(R.id.lineProgress);
        time_progress_slide = (TextView) findViewById(R.id.slidepanel_time_progress);
        time_total_slide = (TextView) findViewById(R.id.slidepanel_time_total);
        time_progress_bottom = (TextView) findViewById(R.id.slidepanel_time_progress_bottom);
        time_total_bottom = (TextView) findViewById(R.id.slidepanel_time_total_bottom);

        btn_backward.setOnClickListener(this);
        btn_forward.setOnClickListener(this);
        btn_play.setOnClickListener(this);
        pgPlayPauseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                return;
            }
        });

        btn_play.Pause();

        changeButtonColor(btn_backward);
        changeButtonColor(btn_forward);

        text_songName = (TextView) findViewById(R.id.text_songName);
        text_songAlb = (TextView) findViewById(R.id.text_songAlb);
        txt_bottom_SongName = (TextView) findViewById(R.id.txt_bottom_SongName);
        txt_bottom_SongAlb = (TextView) findViewById(R.id.txt_bottom_SongAlb);

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        slideBottomView = (RelativeLayout) findViewById(R.id.slideBottomView);
        slideBottomView.setVisibility(View.VISIBLE);
        slideBottomView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });

        audioPg.setMax(0);
        audioPg.setOnValueChangedListener(this);

        mLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if (slideOffset == 0.0f) {
                    isExpand = false;
                    slideBottomView.setVisibility(View.VISIBLE);
                    //slideBottomView.getBackground().setAlpha(0);
                } else if (slideOffset > 0.0f && slideOffset < 1.0f) {
                    //slideBottomView.getBackground().setAlpha((int) slideOffset * 255);
                } else {
                    //slideBottomView.getBackground().setAlpha(100);
                    isExpand = true;
                    slideBottomView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPanelExpanded(View panel) {
                isExpand = true;
            }

            @Override
            public void onPanelCollapsed(View panel) {
                isExpand = false;
            }

            @Override
            public void onPanelAnchored(View panel) {
            }

            @Override
            public void onPanelHidden(View panel) {
            }
        });

        musicList = (ListView) findViewById(R.id.musicList);
        adapterMusic = new AdapterMusic(context, new ArrayList<MediaMetaData>());
        adapterMusic.setListItemListener(new AdapterMusic.ListItemListener() {
            @Override
            public void onItemClickListener(MediaMetaData media) {
                playSong(media);
            }
        });
        musicList.setAdapter(adapterMusic);


    }

    private void loadMusicData() {
        MusicBrowser.loadMusic(context, new MusicLoaderListener() {
            @Override
            public void onLoadSuccess(List<MediaMetaData> listMusic) {
                listOfSongs = listMusic;
                adapterMusic.refresh(listMusic);

//                configAudioStreamer();
//                checkAlreadyPlaying();
            }

            @Override
            public void onLoadFailed() {
                //TODO SHOW FAILED REASON
            }

            @Override
            public void onLoadError() {
                //TODO SHOW ERROR
            }
        });
    }


    private void loadSongDetails(MediaMetaData metaData) {
        text_songName.setText(metaData.getMediaTitle());
        text_songAlb.setText(metaData.getMediaArtist());
        txt_bottom_SongName.setText(metaData.getMediaTitle());
        txt_bottom_SongAlb.setText(metaData.getMediaArtist());

    }


    private static void progressEvent(View v, boolean isShowing) {
        try {
            View parent = (View) ((ImageView) v).getParent();
            ProgressBar pg = (ProgressBar) parent.findViewById(R.id.pg);
            if (pg != null)
                pg.setVisibility(isShowing ? View.GONE : View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void setMaxTime() {
        try {
            String timeString = DateUtils.formatElapsedTime(Long.parseLong(currentSong.getMediaDuration()));
            time_total_bottom.setText(timeString);
            time_total_slide.setText(timeString);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void changeButtonColor(ImageView imageView) {
        try {
            int color = Color.BLACK;
            imageView.setColorFilter(color);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PendingIntent getNotificationPendingIntent() {
        Intent intent = new Intent(context, MusicActivity.class);
        intent.setAction("openplayer");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent mPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        return mPendingIntent;
    }

    ///

    private MediaBrowserCompat.ConnectionCallback mMediaBrowserCompatConnectionCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            isMediaConnected = true;
            try {
                mMediaControllerCompat = new MediaControllerCompat(MusicActivity.this, mMediaBrowserCompat.getSessionToken());
                mMediaControllerCompat.registerCallback(mMediaControllerCompatCallback);
                mMediaBrowserCompat.subscribe(mMediaBrowserCompat.getRoot(),IMediaSessionCallback);
                Log.d("musicBrowserConnected","true");


            } catch( RemoteException e ) {
                Log.d("mediaBrwConnection","ExEPTION " + e.getMessage());

            }
        }


    };

    private MediaControllerCompat.Callback mMediaControllerCompatCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);

            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    pgPlayPauseLayout.setVisibility(View.INVISIBLE);
                    btn_play.Play();
                    if (currentSong != null) {
                        currentSong.setPlayState(PlaybackStateCompat.STATE_PLAYING);
                        notifyAdapter(currentSong);
                    }
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    pgPlayPauseLayout.setVisibility(View.INVISIBLE);
                    btn_play.Pause();
                    if (currentSong != null) {
                        currentSong.setPlayState(PlaybackStateCompat.STATE_PAUSED);
                        notifyAdapter(currentSong);
                    }
                    break;
                case PlaybackStateCompat.STATE_NONE:
                    currentSong.setPlayState(PlaybackStateCompat.STATE_NONE);
                    notifyAdapter(currentSong);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    pgPlayPauseLayout.setVisibility(View.INVISIBLE);
                    btn_play.Pause();
                    audioPg.setValue(0);
                    if (currentSong != null) {
                        currentSong.setPlayState(PlaybackStateCompat.STATE_NONE);
                        notifyAdapter(currentSong);
                    }
                    break;
                case PlaybackStateCompat.STATE_BUFFERING:
                    pgPlayPauseLayout.setVisibility(View.VISIBLE);
                    if (currentSong != null) {
                        currentSong.setPlayState(PlaybackStateCompat.STATE_NONE);
                        notifyAdapter(currentSong);
                    }
                    break;
            }

        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            Log.d("onMetaData_title",metadata.getDescription() + " ");
        }


    };

    private MediaBrowserCompat.SubscriptionCallback IMediaSubscriptionCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
            Log.d("onChildren","Loaded");
            Log.d("onChildrenSize",children.size() + " ");

            for (final MediaBrowserCompat.MediaItem mediaItem : children) {
                mMediaControllerCompat.addQueueItem(mediaItem.getDescription());
            }

            // Call prepare now so pressing play just works.
            mMediaControllerCompat.getTransportControls().prepare();

        }

        @Override
        public void onError(@NonNull String parentId, @NonNull Bundle options) {
            super.onError(parentId, options);
            Log.d("onSubscriptionError","error");

        }
    };

    private class MediaSubscribtionCallback extends MediaBrowserCompat.SubscriptionCallback {

        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            Log.d("onChildren","Loaded");
            Log.d("onChildrenSize",children.size() + " ");


            for (final MediaBrowserCompat.MediaItem mediaItem : children) {

                 mMediaControllerCompat.addQueueItem(mediaItem.getDescription());
            }

            // Call prepare now so pressing play just works.
            mMediaControllerCompat.getTransportControls().prepare();

        }
    }
}