package io.github.doubi88.slideshowwallpaper.infoScreen;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import io.github.doubi88.slideshowwallpaper.R;

public class InfoScreenFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.info_screen);
    }
}
