package dm.audiostreamerdemo.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dm.audiostreamerdemo.util.MusicLibrary;
import dm.audiostreamerdemo.widgets.NotificationManager;

public class MediaBrowserService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener ,AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mMediaCompat;
    private MediaCallBack IMediaCallback;
    private NotificationManager mNotificationManager;
    private boolean mServiceInStartedState = false;
    private MediaMetadataCompat mCurrentMedia;



    @Override
    public void onCreate() {
        super.onCreate();

        mNotificationManager = new NotificationManager(this);

        initialMediaPlayer();

        initialMediaSession();

        registerNoisyBroadcast();

    }


    private void initialMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setVolume(1.0f,1.0f);


    }

    private void initialMediaSession() {
        IMediaCallback = new MediaCallBack();
        ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mMediaCompat = new MediaSessionCompat(getApplicationContext(), "Tag", mediaButtonReceiver, null);
        mMediaCompat.setCallback(IMediaCallback);
        mMediaCompat.setFlags( MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS );

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mMediaCompat.setMediaButtonReceiver(pendingIntent);
        setSessionToken(mMediaCompat.getSessionToken());

    }

    private void registerNoisyBroadcast() {
        IntentFilter noisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyBroadCastReciver,noisyIntentFilter);

    }


    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MusicLibrary.getRoot(),null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(MusicLibrary.getMediaItems());


    }

    @Override
    public void onAudioFocusChange(int focusChange) {

        switch( focusChange ) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                if( mMediaPlayer.isPlaying() ) {
                    mMediaPlayer.stop();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT: {
                mMediaPlayer.pause();
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                if( mMediaPlayer != null ) {
                    mMediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                if( mMediaPlayer != null ) {
                    if( !mMediaPlayer.isPlaying() ) {
                        mMediaPlayer.start();
                    }
                    mMediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;
            }
        }

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

    }

    private class MediaCallBack extends MediaSessionCompat.Callback {
        private final List<MediaSessionCompat.QueueItem> mPlaylist = new ArrayList<>();
        private int mQueueIndex = -1;
        private MediaMetadataCompat mPreparedMedia;

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            mPlaylist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mQueueIndex == -1) ? 0 : mQueueIndex;
            mMediaCompat.setQueue(mPlaylist);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            mPlaylist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            mQueueIndex = (mPlaylist.isEmpty()) ? -1 : mQueueIndex;
            mMediaCompat.setQueue(mPlaylist);
        }

        @Override
        public void onPrepare() {
            if (mQueueIndex < 0 && mPlaylist.isEmpty()) {
                // Nothing to play.
                return;
            }

            final String mediaId = mPlaylist.get(mQueueIndex).getDescription().getMediaId();
            mPreparedMedia = MusicLibrary.getMetadata(MediaBrowserService.this, mediaId);
            mMediaCompat.setMetadata(mPreparedMedia);

            if (!mMediaCompat.isActive()) {
                mMediaCompat.setActive(true);
            }        }

        @Override
        public void onPlay() {
            super.onPlay();
            if( !successfullyRetrievedAudioFocus() ) {
                return;
            }

            mMediaCompat.setActive(true);

            if (mPreparedMedia == null) {
                onPrepare();
            }

            playFromMedia(mPreparedMedia);

        }


        @Override
        public void onPause() {
            mMediaPlayer.pause();
        }

        @Override
        public void onStop() {
            mMediaPlayer.stop();
            mMediaCompat.setActive(false);
        }

        @Override
        public void onSeekTo(long pos) {
            mMediaPlayer.seekTo((int)pos);

        }


        @Override
        public void onSkipToNext() {
            mQueueIndex = (++mQueueIndex % mPlaylist.size());
            mPreparedMedia = null;
            onPlay();

        }

        @Override
        public void onSkipToPrevious() {
            mQueueIndex = mQueueIndex > 0 ? mQueueIndex - 1 : mPlaylist.size() - 1;
            mPreparedMedia = null;
            onPlay();
        }


    }


    private BroadcastReceiver noisyBroadCastReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {

            }

        }
    };

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }

    private void setMediaPlaybackState(int state) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        playbackstateBuilder.setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0);
        mMediaCompat.setPlaybackState(playbackstateBuilder.build());
    }


    class ServiceManager {

        private void moveServiceToStartedState(PlaybackStateCompat state) {
            Notification notification =
                    mNotificationManager.getNotification(
                            mCurrentMedia, state, getSessionToken());

            if (!mServiceInStartedState) {
                ContextCompat.startForegroundService(
                        MediaBrowserService.this,
                        new Intent(MediaBrowserService.this, MediaBrowserService.class));
                mServiceInStartedState = true;
            }

            startForeground(mNotificationManager.NOTIFICATION_ID, notification);
        }

        private void updateNotificationForPause(PlaybackStateCompat state) {
            stopForeground(false);
            Notification notification =
                    mNotificationManager.getNotification(
                            mCurrentMedia , state, getSessionToken());
            mNotificationManager.getNotificationManager()
                    .notify(mNotificationManager.NOTIFICATION_ID, notification);
        }

        private void moveServiceOutOfStartedState(PlaybackStateCompat state) {
            stopForeground(true);
            stopSelf();
            mServiceInStartedState = false;
        }
    }

    private void playFromMedia(MediaMetadataCompat media)  {
        mCurrentMedia = media;
        mMediaPlayer.prepareAsync();
        try {
            mMediaPlayer.setDataSource(media.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI));
        } catch (IOException e) {
            e.printStackTrace();
            mMediaPlayer.release();


        }

    }


}
