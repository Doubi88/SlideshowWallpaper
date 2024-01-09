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

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;

public class SharedPreferencesManager {

    private static final String PREFERENCE_KEY_ORDERING = "ordering";
    private static final String PREFERENCE_KEY_LAST_UPDATE = "last_update";
    private static final String PREFERENCE_KEY_LAST_INDEX = "last_index";
    private static final String PREFERENCE_KEY_URI_LIST = "pick_images";
    private static final String PREFERENCE_KEY_URI_LIST_RANDOM = "uri_list_random";
    private static final String PREFERENCE_KEY_SECONDS_BETWEEN = "seconds";
    private static final String PREFERENCE_KEY_TOO_WIDE_IMAGES_RULE = "too_wide_images_rule";
    private static final String PREFERENCE_KEY_ANTI_ALIAS = "anti_alias";
    private static final String PREFERENCE_KEY_ANTI_ALIAS_WHILE_SCROLLING = "anti_alias_scrolling";

    public enum Ordering {
        SELECTION(0, PREFERENCE_KEY_URI_LIST) {
            @Override
            public List<Uri> sort(List<Uri> list) {
                return list;
            }
        },
        RANDOM(1, PREFERENCE_KEY_URI_LIST_RANDOM) {
            @Override
            public List<Uri> sort(List<Uri> list) {
                List<Uri> result = new ArrayList<>(list);
                Collections.shuffle(result);
                return result;
            }
        };

        private int valueListIndex;
        private String preferenceKey;

        private Ordering(int valueListIndex, String preferenceKey) {
            this.valueListIndex = valueListIndex;
            this.preferenceKey = preferenceKey;
        }

        public static Ordering forValue(String value, Resources r) {
            Ordering[] values = values();
            Ordering result = null;
            for (int i = 0; i < values.length && result == null; i++) {
                if (values[i].getValue(r).equals(value)) {
                    result = values[i];
                }
            }
            return result;
        }

        public String getDescription(Resources r) {
            return r.getStringArray(R.array.orderings)[valueListIndex];
        }

        public String getValue(Resources r) {
            return r.getStringArray(R.array.ordering_values)[valueListIndex];
        }

        public String getPreferenceKey() {
            return preferenceKey;
        }

        public abstract List<Uri> sort(List<Uri> list);
    }

    public enum TooWideImagesRule {
        SCROLL_FORWARD(0),
        SCROLL_BACKWARD(1),
        SCALE_DOWN(2),
        SCALE_UP(3);

        private int valueListIndex;

        private TooWideImagesRule(int valueListIndex) {
            this.valueListIndex = valueListIndex;
        }

        public String getDescription(Resources r) {
            return r.getStringArray(R.array.too_wide_images_rules)[valueListIndex];
        }

        public String getValue(Resources r) {
            return r.getStringArray(R.array.too_wide_images_rule_values)[valueListIndex];
        }

        public static TooWideImagesRule forValue(String value, Resources r) {
            TooWideImagesRule[] values = values();
            TooWideImagesRule result = null;
            for (int i = 0; i < values.length && result == null; i++) {
                if (values[i].getValue(r).equals(value)) {
                    result = values[i];
                }
            }
            return result;
        }
    }
    private SharedPreferences preferences;

    public SharedPreferencesManager(@NonNull SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public Ordering getCurrentOrdering(Resources r) {
        String value = preferences.getString(PREFERENCE_KEY_ORDERING, "selection");
        return Ordering.forValue(value, r);
    }

    public List<Uri> getImageUris(@NonNull Ordering ordering) {
        String[] uris = getUriList(ordering);
        ArrayList<Uri> result = new ArrayList<>(uris.length);
        for (String uri : uris) {
            result.add(Uri.parse(uri));
        }
        return result;
    }

    public boolean hasImageUri(@NonNull Uri uri) {
        List<Uri> uris = getImageUris(Ordering.SELECTION);
        return uris.contains(uri);
    }

    private String[] getUriList(Ordering ordering) {
        String list = preferences.getString(ordering.getPreferenceKey(), null);
        if (list == null || list.equals("")) {
            return new String[0];
        } else {
            return list.split(";");
        }
    }

    public boolean addUri(Uri uri) {
        List<Uri> list = getImageUris(Ordering.SELECTION);
        boolean result = false;
        if (!list.contains(uri)) {
            result = list.add(uri);
            for (Ordering ordering : Ordering.values()) {
                saveUriList(ordering.sort(list), ordering.getPreferenceKey());
            }
        }
        return result;
    }

    private void saveUriList(List<Uri> uris, String preferenceKey) {
        StringBuilder listBuilder = new StringBuilder();
        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            listBuilder.append(uri.toString());

            if (i + 1 < uris.size()) {
                listBuilder.append(";");
            }
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(preferenceKey, listBuilder.toString());
        editor.apply();
    }

    public void removeUri(Uri uri) {
        List<Uri> uris = getImageUris(Ordering.SELECTION);
        uris.remove(uri);
        for (Ordering ordering : Ordering.values()) {
            saveUriList(ordering.sort(uris), ordering.getPreferenceKey());
        }
    }

    public int getCurrentIndex() {
        int result = preferences.getInt(PREFERENCE_KEY_LAST_INDEX, 0);
        String[] uris = getUriList(Ordering.SELECTION);
        while (result >= uris.length) {
            result -= uris.length;
        }
        return result;
    }

    public void setCurrentIndex(int index) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFERENCE_KEY_LAST_INDEX, index);
        editor.apply();
    }

    public long getLastUpdate() {
        return preferences.getLong(PREFERENCE_KEY_LAST_UPDATE, 0);
    }

    public void setLastUpdate(long value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(PREFERENCE_KEY_LAST_UPDATE, value);
        editor.apply();
    }

    public int getSecondsBetweenImages() throws NumberFormatException {
        String secondsString = preferences.getString(PREFERENCE_KEY_SECONDS_BETWEEN, "15");
        int result = Integer.parseInt(secondsString);

        return result;
    }

    public void setSecondsBetweenImages(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCE_KEY_SECONDS_BETWEEN, String.valueOf(value));
        editor.apply();
    }

    public TooWideImagesRule getTooWideImagesRule(Resources r) {
        String value = preferences.getString(PREFERENCE_KEY_TOO_WIDE_IMAGES_RULE, TooWideImagesRule.SCALE_DOWN.getValue(r));
        return TooWideImagesRule.forValue(value, r);
    }

    public boolean getAntiAlias() {
        return preferences.getBoolean(PREFERENCE_KEY_ANTI_ALIAS, true);
    }

    public void setAntiAlias(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREFERENCE_KEY_ANTI_ALIAS, value);
        editor.apply();
    }

    public boolean getAntiAliasWhileScrolling() {
        return preferences.getBoolean(PREFERENCE_KEY_ANTI_ALIAS_WHILE_SCROLLING, true);
    }

    public void setAntiAliasWhileScrolling(boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(PREFERENCE_KEY_ANTI_ALIAS_WHILE_SCROLLING, value);
        editor.apply();
    }
}
