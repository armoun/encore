package org.omnirom.music.app;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import org.omnirom.music.app.fragments.AutomixFragment;
import org.omnirom.music.app.fragments.MySongsFragment;
import org.omnirom.music.app.fragments.NavigationDrawerFragment;
import org.omnirom.music.app.fragments.PlaylistListFragment;
import org.omnirom.music.app.ui.PlayingBarView;
import org.omnirom.music.framework.PluginsLookup;
import org.omnirom.music.service.IPlaybackService;


public class MainActivity extends FragmentActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = "MainActivity";

    public static final int SECTION_LISTEN_NOW = 1;
    public static final int SECTION_MY_SONGS   = 2;
    public static final int SECTION_PLAYLISTS  = 3;
    public static final int SECTION_AUTOMIX    = 4;
    public static final int SECTION_NOW_PLAYING= 5;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private PlayingBarView mPlayingBarLayout;

    private boolean mRestoreBarOnBack;

    private SearchView mSearchView;

    public MainActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout), getTheme());


        // Setup the playing bar click listener
        mPlayingBarLayout = (PlayingBarView) findViewById(R.id.playingBarLayout);
    }

    public boolean isPlayBarVisible() {
        return mPlayingBarLayout.isVisible();
    }

    @Override
    public void onBackPressed() {
        if (!mPlayingBarLayout.isWrapped()) {
            mPlayingBarLayout.setWrapped(true);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PluginsLookup.getDefault().connectPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }

        // Release services connections if playback isn't happening
        IPlaybackService playbackService = PluginsLookup.getDefault().getPlaybackService();
        try {
            if (!playbackService.isPlaying()) {
                PluginsLookup.getDefault().tearDown();
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot determine if playbackservice is running");
        }

        super.onDestroy();
    }

    @Override
    public boolean onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        boolean result = true;
        try {
            Fragment newFrag = null;
            switch (position+1) {
                case SECTION_PLAYLISTS:
                    newFrag = PlaylistListFragment.newInstance(true);
                    break;
                case SECTION_MY_SONGS:
                    newFrag = MySongsFragment.newInstance();
                    break;
                case SECTION_AUTOMIX:
                    newFrag = AutomixFragment.newInstance();
                    break;
                case SECTION_NOW_PLAYING:
                    startActivity(new Intent(this, PlaybackQueueActivity.class));
                    break;
            }

            if (newFrag != null) {
                showFragment(newFrag, false);
                result = true;
            } else {
                result = false;
            }
        } catch (IllegalStateException e) {
            // The app is pausing
        }

        return result;
    }

    public void showFragment(Fragment f, boolean addToStack) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0 && !addToStack) {
            fragmentManager.popBackStack();
            if (mRestoreBarOnBack) {
                mRestoreBarOnBack = false;
            }
        }

        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (addToStack) {
            ft.addToBackStack(f.toString());
        }
        ft.replace(R.id.container, f);
        ft.commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case SECTION_LISTEN_NOW:
                mTitle = getString(R.string.title_section_listen_now);
                break;
            case SECTION_MY_SONGS:
                mTitle = getString(R.string.title_section_my_songs);
                break;
            case SECTION_PLAYLISTS:
                mTitle = getString(R.string.title_section_playlists);
                break;
            case SECTION_AUTOMIX:
                mTitle = getString(R.string.title_section_automix);
                break;
        }
    }

    public void setContentShadowTop(int pxTop) {
        findViewById(R.id.action_bar_shadow).setTranslationY(pxTop);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();

        if (actionBar != null) {
            ((TextView) actionBar.getCustomView().findViewById(android.R.id.title)).setText(mTitle);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

            mSearchView = (SearchView) menu.findItem(R.id.action_search)
                   .getActionView();
            mSearchView.setSearchableInfo(searchManager
                    .getSearchableInfo(getComponentName()));

            int searchTextViewId = mSearchView.getContext().getResources().getIdentifier("android:id/search_src_text", null, null);
            TextView searchTextView = (TextView) mSearchView.findViewById(searchTextViewId);
            searchTextView.setHintTextColor(getResources().getColor(R.color.white));

            // Google, why is searchView using a Gingerbread-era enforced icon?
            int searchImgId = getResources().getIdentifier("android:id/search_button", null, null);
            ImageView v = (ImageView) mSearchView.findViewById(searchImgId);
            v.setImageResource(R.drawable.ic_action_search);

            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
