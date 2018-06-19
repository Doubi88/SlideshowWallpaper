package de.tobi.slideshowwallpaper;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class WallpaperPreferencesFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_FILE = 1;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Debug.waitForDebugger();
        addPreferencesFromResource(R.xml.wallpaper_preferences);

        Preference preference = findPreference(getResources().getString(R.string.preference_pick_folder_key));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_CODE_FILE);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK) {
            Set<String> uris = new HashSet<>();
            ClipData clipData = data.getClipData();
            if (clipData == null) {
                String dataString = data.getDataString();
                uris.add(dataString);
            } else {

                for (int index = 0; index < clipData.getItemCount(); index++) {
                    uris.add(clipData.getItemAt(index).getUri().toString());
                }
            }
            saveFilesPreference(uris);
        }
    }

    private void saveFilesPreference(Set<String> values) {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(getResources().getString(R.string.preference_pick_folder_key), values);
        editor.commit();
    }
}
