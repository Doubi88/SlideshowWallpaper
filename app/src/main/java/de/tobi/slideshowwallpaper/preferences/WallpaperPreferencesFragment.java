/*
 * Slideshow Wallpaper: An Android live wallpaper displaying custom images.
 * Copyright (C) 2022  Doubi88 <tobis_mail@yahoo.de>
 *
 * Slideshow Wallpaper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Slideshow Wallpaper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package de.tobi.slideshowwallpaper.preferences;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.SlideshowWallpaperService;

public class WallpaperPreferencesFragment extends PreferenceFragmentCompat {

    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.wallpaper_preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
        listener = this::updateSummary;
        getPreferenceManager().findPreference(getResources().getString(R.string.preference_preview_key)).setOnPreferenceClickListener(preference -> {
            Context ctx = getContext();
            if (ctx != null) {
                Intent intent = new Intent(
                        WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(ctx, SlideshowWallpaperService.class));
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }

    private void updateSummaries() {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        updateSummary(sharedPreferences, getResources().getString(R.string.preference_add_images_key));
        updateSummary(sharedPreferences, getResources().getString(R.string.preference_seconds_key));
        updateSummary(sharedPreferences, getResources().getString(R.string.preference_ordering_key));
    }

    private <T> int getIndex(T[] values, T value) {
        int index = -1;
        for (int i = 0; (i < values.length) && (index < 0); i++) {
            if (values[i].equals(value)) {
                index = i;
            }
        }
        return index;
    }
    private void updateSummary(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getResources().getString(R.string.preference_add_images_key))) {
            findPreference(key).setSummary(new SharedPreferencesManager(getPreferenceManager().getSharedPreferences()).getImageUris(SharedPreferencesManager.Ordering.SELECTION).size() + " " + getResources().getString(R.string.images_selected));
        } else if (key.equals(getResources().getString(R.string.preference_seconds_key))) {
            String[] seconds = getResources().getStringArray(R.array.seconds);
            String[] secondsValues = getResources().getStringArray(R.array.seconds_values);
            String currentValue = sharedPreferences.getString(key, "15");
            int index = getIndex(secondsValues, currentValue);
            findPreference(key).setSummary(seconds[index]);
        } else if (key.equals(getResources().getString(R.string.preference_ordering_key))) {
            String[] orderings = getResources().getStringArray(R.array.orderings);
            String[] orderingValues = getResources().getStringArray(R.array.ordering_values);
            String currentValue = sharedPreferences.getString(key, "selection");
            int index = getIndex(orderingValues, currentValue);
            findPreference(key).setSummary(orderings[index]);
        }

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
        super.onPause();
    }
}
