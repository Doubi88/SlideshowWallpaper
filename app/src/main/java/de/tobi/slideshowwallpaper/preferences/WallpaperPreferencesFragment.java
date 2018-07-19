package de.tobi.slideshowwallpaper.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.util.HashSet;

import de.tobi.slideshowwallpaper.R;

public class WallpaperPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.wallpaper_preferences);

        findPreference(getResources().getString(R.string.preference_add_images_key)).setSummary(getPreferenceManager().getSharedPreferences().getStringSet(getResources().getString(R.string.preference_add_images_key), new HashSet<String>()).size() + " " + getResources().getString(R.string.images_selected));
        findPreference(getResources().getString(R.string.preference_ordering_key)).setSummary(getPreferenceManager().getSharedPreferences().getString(getResources().getString(R.string.preference_ordering_key), ""));
        findPreference(getResources().getString(R.string.preference_seconds_key)).setSummary(getPreferenceManager().getSharedPreferences().getString(getResources().getString(R.string.preference_seconds_key), ""));
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(getResources().getString(R.string.preference_add_images_key))) {
                    findPreference(key).setSummary(sharedPreferences.getStringSet(key, new HashSet<String>()).size() + " " + getResources().getString(R.string.images_selected));
                } else {
                    findPreference(key).setSummary(sharedPreferences.getString(key, ""));
                }
            }
        });
    }


}
