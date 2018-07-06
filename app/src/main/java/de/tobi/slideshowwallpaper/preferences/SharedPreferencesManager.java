package de.tobi.slideshowwallpaper.preferences;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.SlideshowWallpaperService;

public class SharedPreferencesManager {

    private static final String PREFERENCE_KEY_ORDERING = "ordering";
    private static final String PREFERENCE_KEY_LAST_UPDATE = "last_update";
    private static final String PREFERENCE_KEY_LAST_INDEX = "last_index";
    private static final String PREFERENCE_KEY_URI_LIST = "pick_images";
    private static final String PREFERENCE_KEY_SECONDS_BETWEEN = "seconds";

    public enum Ordering {
        SELECTION(0) {
            @Override
            public List<Uri> sort(List<Uri> list) {
                return list;
            }
        },
        ALPHABET(1) {
            @Override
            public List<Uri> sort(List<Uri> list) {
                return list; //TODO
            }
        },
        RANDOM(2) {
            @Override
            public List<Uri> sort(List<Uri> list) {
                List<Uri> result = new ArrayList<Uri>(list);
                Collections.shuffle(result);
                return result;
            }
        };

        private int valueListIndex;

        private Ordering(int valueListIndex) {
            this.valueListIndex = valueListIndex;
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

        public abstract List<Uri> sort(List<Uri> list);
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
        Set<String> uris = getUriSet();
        ArrayList<Uri> result = new ArrayList<>(uris.size());
        for (String uri : uris) {
            result.add(Uri.parse(uri));
        }
        return result;
    }

    private Set<String> getUriSet() {
        return preferences.getStringSet(PREFERENCE_KEY_URI_LIST, Collections.<String>emptySet());
    }

    public void addUri(Uri uri) {
        Set<String> uris = getUriSet();
        HashSet<String> newSet = new HashSet<>(uris);
        newSet.add(uri.toString());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(PREFERENCE_KEY_URI_LIST, newSet);
        editor.apply();
    }

    public void removeUri(Uri uri) {
        Set<String> uris = getUriSet();
        HashSet<String> newSet = new HashSet<>(uris);
        newSet.remove(uri.toString());

        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(PREFERENCE_KEY_URI_LIST, newSet);
        editor.apply();
    }

    public int getCurrentIndex() {
        int result = preferences.getInt(PREFERENCE_KEY_LAST_INDEX, 0);
        Set<String> set = getUriSet();
        while (result >= set.size()) {
            result -= set.size();
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
        int seconds = 5;
        String secondsString = preferences.getString(PREFERENCE_KEY_SECONDS_BETWEEN, "5");
        seconds = Integer.parseInt(secondsString);
        return seconds;

    }

    public void setSecondsBetweenImages(int value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(PREFERENCE_KEY_SECONDS_BETWEEN, value);
        editor.apply();
    }
}
