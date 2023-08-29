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
package io.github.doubi88.slideshowwallpaper.preferences;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.SlideshowWallpaperService;
import io.github.doubi88.slideshowwallpaper.utilities.CompatibilityHelpers;

public class WallpaperPreferencesFragment extends PreferenceFragmentCompat {
    public static int DEFAULT_SECONDS = 60;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.wallpaper_preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Here getResources should never throw an IllegalStateException,
        // because onResume is only called, if an Activity is present.
        updateSummaries(getResources());
        getPreferenceManager().findPreference(getResources().getString(R.string.preference_preview_key)).setOnPreferenceClickListener(preference -> {
            // A click on a preference can only occur in a valid context
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
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this::updateSummary);
    }

    private void updateSummaries(Resources resources) {
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        updateSummary(sharedPreferences, getResources().getString(R.string.preference_add_images_key));
        updateSummary(sharedPreferences, resources.getString(R.string.preference_seconds_key));
        updateSummary(sharedPreferences, resources.getString(R.string.preference_ordering_key));
        updateSummary(sharedPreferences, resources.getString(R.string.preference_too_wide_images_rule_key));
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
        Resources res = null;
        try {
            res = getResources();
        } catch (IllegalStateException e) {
            // There is no context currently -> We do not need to update the view
        }
        if (res != null) {
            if (key.equals(res.getString(R.string.preference_add_images_key))) {
                SharedPreferencesManager prefManager = new SharedPreferencesManager(sharedPreferences);
                int imagesCount = prefManager.getImageUris(SharedPreferencesManager.Ordering.SELECTION).size();

                int maxCount = 128;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    maxCount = 512;
                }
                findPreference(key).setSummary(res.getQuantityString(R.plurals.images_selected, imagesCount, imagesCount, maxCount));
            } else if (key.equals(res.getString(R.string.preference_seconds_key))) {
                String[] seconds = res.getStringArray(R.array.seconds);
                String[] secondsValues = res.getStringArray(R.array.seconds_values);
                String currentValue = sharedPreferences.getString(key, String.valueOf(DEFAULT_SECONDS));
                int intValue = DEFAULT_SECONDS;
                try {
                    intValue = Integer.parseInt(currentValue);
                } catch (NumberFormatException e) {
                    intValue = DEFAULT_SECONDS;
                }
                int currentIntValue = CompatibilityHelpers.getNextAvailableSecondsEntry(intValue, secondsValues);
                int index = getIndex(secondsValues, String.valueOf(currentIntValue));
                findPreference(key).setSummary(seconds[index]);
            } else if (key.equals(res.getString(R.string.preference_ordering_key))) {
                String[] orderings = res.getStringArray(R.array.orderings);
                String[] orderingValues = res.getStringArray(R.array.ordering_values);
                String currentValue = sharedPreferences.getString(key, SharedPreferencesManager.Ordering.SELECTION.getValue(res));
                int index = getIndex(orderingValues, currentValue);
                findPreference(key).setSummary(orderings[index]);
            } else if (key.equals(res.getString(R.string.preference_too_wide_images_rule_key))) {
                String[] displayRules = res.getStringArray(R.array.too_wide_images_rules);
                String[] displayRuleValues = res.getStringArray(R.array.too_wide_images_rule_values);
                String currentValue = sharedPreferences.getString(key, SharedPreferencesManager.TooWideImagesRule.SCALE_DOWN.getValue(res));
                int index = getIndex(displayRuleValues, currentValue);
                findPreference(key).setSummary(displayRules[index]);
            }
        }

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this::updateSummary);
        super.onPause();
    }
}
