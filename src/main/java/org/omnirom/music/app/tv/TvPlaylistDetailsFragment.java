package org.omnirom.music.app.tv;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.util.DisplayMetrics;
import android.view.View;

import org.omnirom.music.app.R;
import org.omnirom.music.art.AlbumArtHelper;
import org.omnirom.music.art.AlbumArtTask;
import org.omnirom.music.art.RecyclingBitmapDrawable;
import org.omnirom.music.framework.PlaybackProxy;
import org.omnirom.music.model.Artist;
import org.omnirom.music.model.BoundEntity;
import org.omnirom.music.model.Playlist;
import org.omnirom.music.model.Song;
import org.omnirom.music.providers.ProviderAggregator;
import org.omnirom.music.service.BasePlaybackCallback;
import org.omnirom.music.utils.Utils;

import java.util.Iterator;

public class TvPlaylistDetailsFragment extends DetailsFragment {
    private static final String TAG = "PlaylistDetailsFragment";

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int ACTION_PLAY = 1;
    private static final int ACTION_QUEUE = 2;

    private static final int MSG_UPDATE_ADAPTER = 1;

    private Playlist mPlaylist;
    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;
    private SongRowPresenter mSongRowPresenter;

    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private int mBackgroundColor;
    private DisplayMetrics mMetrics;

    private Action mActionPlay;
    private Action mActionQueue;
    private boolean mIsPlaying;

    private Handler mHandler;
    private BasePlaybackCallback mPlaybackCallback;
    private View.OnClickListener mSongClickListener;

    private AlbumArtTask mArtTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_UPDATE_ADAPTER:
                        updateAdapter();
                        break;
                }
            }
        };

        mPlaybackCallback = new BasePlaybackCallback() {
            @Override
            public void onSongStarted(boolean buffering, Song s) throws RemoteException {
                mSongRowPresenter.setCurrentSong(PlaybackProxy.getCurrentTrack());
                mHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER);
            }
        };

        mSongClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SongRow row = (SongRow) v.getTag();
                PlaybackProxy.clearQueue();
                PlaybackProxy.queuePlaylist(mPlaylist, false);
                PlaybackProxy.playAtIndex(row.getPosition());
                mIsPlaying = true;
                mActionPlay.setLabel1(getString(R.string.pause));
                updateAdapter();
            }
        };

        mActionPlay = new Action(ACTION_PLAY, getString(R.string.play));
        mActionQueue = new Action(ACTION_QUEUE, getString(R.string.tv_action_queue));

        prepareBackgroundManager();

        mPlaylist = getActivity().getIntent().getParcelableExtra(TvPlaylistDetailsActivity.EXTRA_PLAYLIST);
        mBackgroundColor = getActivity().getIntent().getIntExtra(TvPlaylistDetailsActivity.EXTRA_COLOR, getResources().getColor(R.color.primary));
        if (mPlaylist != null) {
            setupAdapter();
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            setupTrackListRow();
            setupTrackListRowPresenter();
            setupFragmentBackground();
        } else {
            Intent intent = new Intent(getActivity(), TvActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PlaybackProxy.addCallback(mPlaybackCallback);
        mSongRowPresenter.setCurrentSong(PlaybackProxy.getCurrentTrack());
        updateAdapter();
    }

    @Override
    public void onPause() {
        super.onPause();
        PlaybackProxy.removeCallback(mPlaybackCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mArtTask != null) {
            mArtTask.cancel(true);
        }
    }

    private void updateAdapter() {
        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.bg_welcome_top);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupAdapter() {
        mPresenterSelector = new ClassPresenterSelector();
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    private void setupDetailsOverviewRow() {
        final Resources res = getResources();
        final DetailsOverviewRow row = new DetailsOverviewRow(mPlaylist);
        row.setImageDrawable(res.getDrawable(R.drawable.album_placeholder));

        SparseArrayObjectAdapter actionsAdapter = new SparseArrayObjectAdapter();
        actionsAdapter.set(ACTION_PLAY, mActionPlay);
        actionsAdapter.set(ACTION_QUEUE, mActionQueue);
        row.setActionsAdapter(actionsAdapter);

        mAdapter.add(row);

        AlbumArtHelper.retrieveAlbumArt(getResources(), new AlbumArtHelper.AlbumArtListener() {
            @Override
            public void onArtLoaded(RecyclingBitmapDrawable output, BoundEntity request) {
                if (output != null) {
                    row.setImageDrawable(output);
                    updateAdapter();
                }
            }
        }, mPlaylist, DETAIL_THUMB_WIDTH, false);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background and style.
        DetailsOverviewRowPresenter detailsPresenter =
                new DetailsOverviewRowPresenter(new PlaylistDetailsPresenter());
        detailsPresenter.setBackgroundColor(mBackgroundColor);
        detailsPresenter.setStyleLarge(false);

        // Hook up transition element.
        detailsPresenter.setSharedElementEnterTransition(getActivity(),
                TvPlaylistDetailsActivity.SHARED_ELEMENT_NAME);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_PLAY) {
                    if (mIsPlaying) {
                        PlaybackProxy.pause();
                        mIsPlaying = false;
                    } else {
                        PlaybackProxy.playPlaylist(mPlaylist);
                        mIsPlaying = true;
                    }

                    mActionPlay.setLabel1(getString(mIsPlaying ? R.string.pause : R.string.play));
                    updateAdapter();
                } else if (action.getId() == ACTION_QUEUE) {
                    PlaybackProxy.queuePlaylist(mPlaylist, false);
                }
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private void setupTrackListRow() {
        Iterator<String> it = mPlaylist.songs();

        int index = 0;
        while (it.hasNext()) {
            String trackRef = it.next();
            Song song = ProviderAggregator.getDefault().retrieveSong(trackRef, mPlaylist.getProvider());
            if (song != null) {
                mAdapter.add(new SongRow(song, index++));
            }
        }
    }

    private void setupTrackListRowPresenter() {
        mSongRowPresenter = new SongRowPresenter(mSongClickListener);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mPresenterSelector.addClassPresenter(SongRow.class, mSongRowPresenter);
    }

    private void setupFragmentBackground() {
        String artistRef = Utils.getMainArtist(mPlaylist);

        if (artistRef != null) {
            Artist artist = ProviderAggregator.getDefault().retrieveArtist(artistRef, mPlaylist.getProvider());

            mArtTask = AlbumArtHelper.retrieveAlbumArt(getResources(), new AlbumArtHelper.AlbumArtListener() {
                @Override
                public void onArtLoaded(RecyclingBitmapDrawable output, BoundEntity request) {
                    if (output != null && mBackgroundManager.isAttached()) {
                        mBackgroundManager.setDrawable(output);
                    }
                }
            }, artist, mMetrics.widthPixels, false);
        }
    }
}
