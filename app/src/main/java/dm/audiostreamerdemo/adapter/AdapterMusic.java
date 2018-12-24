/*
 * This is the source code of DMAudioStreaming for Android v. 1.0.0.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Copyright @Dibakar_Mistry(dibakar.ece@gmail.com), 2017.
 */
package dm.audiostreamerdemo.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;



import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import dm.audiostreamerdemo.R;
import dm.audiostreamerdemo.util.MediaMetaData;

public class AdapterMusic extends BaseAdapter {
    private List<MediaMetaData> musicList;
    private Context mContext;
    private LayoutInflater inflate;


    private ColorStateList colorPlay;
    private ColorStateList colorPause;

    public AdapterMusic(Context context, List<MediaMetaData> authors) {
        this.musicList = authors;
        this.mContext = context;
        this.inflate = LayoutInflater.from(context);
        this.colorPlay = ColorStateList.valueOf(context.getResources().getColor(R.color.md_black_1000));
        this.colorPause = ColorStateList.valueOf(context.getResources().getColor(R.color.md_blue_grey_500_75));

    }

    public void refresh(List<MediaMetaData> musicList) {
        if (this.musicList != null) {
            this.musicList.clear();
        }
        this.musicList.addAll(musicList);
        notifyDataSetChanged();
    }

    public void notifyPlayState(MediaMetaData metaData) {
        if (this.musicList != null && metaData != null) {
            int index = this.musicList.indexOf(metaData);
            //TODO SOMETIME INDEX RETURN -1 THOUGH THE OBJECT PRESENT IN THIS LIST
            if (index == -1) {
                for (int i = 0; i < this.musicList.size(); i++) {
                    if (this.musicList.get(i).getMediaId().equalsIgnoreCase(metaData.getMediaId())) {
                        index = i;
                        break;
                    }
                }
            }
            if (index > 0 && index < this.musicList.size()) {
                this.musicList.set(index, metaData);
            }
        }
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return musicList.size();
    }

    @Override
    public Object getItem(int i) {
        return musicList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {

        ViewHolder mViewHolder;
        if (convertView == null) {
            mViewHolder = new ViewHolder();
            convertView = inflate.inflate(R.layout.inflate_allsongsitem, null);
            mViewHolder.mediaArt = (ImageView) convertView.findViewById(R.id.img_mediaArt);
            mViewHolder.playState = (ImageView) convertView.findViewById(R.id.img_playState);
            mViewHolder.mediaTitle = (TextView) convertView.findViewById(R.id.text_mediaTitle);
            mViewHolder.MediaDesc = (TextView) convertView.findViewById(R.id.text_mediaDesc);
            convertView.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }
        MediaMetaData media = musicList.get(position);

        mViewHolder.mediaTitle.setText(media.getMediaTitle());
        mViewHolder.MediaDesc.setText(media.getMediaArtist());
        mViewHolder.playState.setImageDrawable(getDrawableByState(mContext, media.getPlayState()));
        String mediaArt = media.getMediaArt();

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listItemListener != null) {
                    listItemListener.onItemClickListener(musicList.get(position));
                }
            }
        });

        return convertView;
    }

    public static class ViewHolder {
        public ImageView mediaArt;
        public ImageView playState;
        public TextView mediaTitle;
        public TextView MediaDesc;
    }


    private Drawable getDrawableByState(Context context, int state) {
        switch (state) {
            case PlaybackStateCompat.STATE_NONE:
                Drawable pauseDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play);
                DrawableCompat.setTintList(pauseDrawable, colorPlay);
                return pauseDrawable;
            case PlaybackStateCompat.STATE_PLAYING:
                AnimationDrawable animation = (AnimationDrawable) ContextCompat.getDrawable(context, R.drawable.equalizer);
                DrawableCompat.setTintList(animation, colorPlay);
                animation.start();
                return animation;
            case PlaybackStateCompat.STATE_PAUSED:
                Drawable playDrawable = ContextCompat.getDrawable(context, R.drawable.equalizer);
                DrawableCompat.setTintList(playDrawable, colorPause);
                return playDrawable;
            default:
                Drawable noneDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play);
                DrawableCompat.setTintList(noneDrawable, colorPlay);
                return noneDrawable;
        }
    }



    private static void progressEvent(View v, boolean isShowing) {
        try {
            RelativeLayout rl = (RelativeLayout) ((ImageView) v).getParent();
            ProgressBar pg = (ProgressBar) rl.findViewById(R.id.pg);
            pg.setVisibility(isShowing ? View.GONE : View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setListItemListener(ListItemListener listItemListener) {
        this.listItemListener = listItemListener;
    }

    public ListItemListener listItemListener;

    public interface ListItemListener {
        void onItemClickListener(MediaMetaData media);
    }
}
