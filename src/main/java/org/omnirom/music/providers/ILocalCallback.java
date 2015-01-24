package org.omnirom.music.providers;

import org.omnirom.music.model.Album;
import org.omnirom.music.model.Artist;
import org.omnirom.music.model.Playlist;
import org.omnirom.music.model.SearchResult;
import org.omnirom.music.model.Song;

import java.util.List;

/**
 * Local callback interface called on providers updates
 */
public interface ILocalCallback {
    /**
     * Called when a song metadata has been updated
     * @param s The list of songs updated
     */
    void onSongUpdate(List<Song> s);

    /**
     * Called when an album metadata has been updated
     * @param a The list of albums updated
     */
    void onAlbumUpdate(List<Album> a);

    /**
     * Called when playlist metadata has been updated
     * @param p The list of playlists updated
     */
    void onPlaylistUpdate(List<Playlist> p);

    /**
     * Called when artist metadata has been updated
     * @param a The list of artists updated
     */
    void onArtistUpdate(List<Artist> a);

    /**
     * Called when a provider has connected
     * @param provider The provider that connected
     */
    void onProviderConnected(IMusicProvider provider);

    /**
     * Called when a provider returns a search result
     * @param searchResult The result
     */
    void onSearchResult(List<SearchResult> searchResult);
}
