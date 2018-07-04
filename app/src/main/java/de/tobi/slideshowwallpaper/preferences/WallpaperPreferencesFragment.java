package de.tobi.slideshowwallpaper.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import java.util.HashSet;

import de.tobi.slideshowwallpaper.R;

public class WallpaperPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        //Debug.waitForDebugger();
        Log.d("SharedPreferencesName", getPreferenceManager().getSharedPreferencesName());
        Log.d("SharedPreferencesMode", String.valueOf(getPreferenceManager().getSharedPreferencesMode()));
        addPreferencesFromResource(R.xml.wallpaper_preferences);

        Preference preference = findPreference(getResources().getString(R.string.preference_pick_folder_key));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getContext(), ImagePreferencesActivity.class);
                startActivity(intent);
                return true;
            }
        });

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(getResources().getString(R.string.preference_pick_folder_key))) {
                    findPreference(key).setSummary(sharedPreferences.getStringSet(key, new HashSet<String>()).toString());
                } else if (key.equals(getResources().getString(R.string.preference_ordering_key))) {
                    findPreference(key).setSummary(sharedPreferences.getString(key, ""));
                }
            }
        });
    }


}
