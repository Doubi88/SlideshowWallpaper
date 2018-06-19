package de.tobi.slideshowwallpaper;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

public class WallpaperPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.wallpaper_preferences);
    }
}
