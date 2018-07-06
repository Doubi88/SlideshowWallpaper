package de.tobi.slideshowwallpaper.preferences;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.SlideshowWallpaperService;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;
import de.tobi.slideshowwallpaper.utilities.ImageInfo;
import de.tobi.slideshowwallpaper.utilities.ImageLoader;

public class ImagesPreferenceFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_FILE = 1;

    private SharedPreferencesManager manager;
    private Preference addPreference;
    private Set<ImagePreference> imagePreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.images_preferences);
        manager = new SharedPreferencesManager(getPreferenceManager().getSharedPreferences());
        initList();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK) {

            ClipData clipData = data.getClipData();
            if (clipData == null) {
                Uri uri = data.getData();
                if (uri != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    manager.addUri(uri);
                    addPreferenceFromUri(uri);
                }
            } else {

                for (int index = 0; index < clipData.getItemCount(); index++) {
                    Uri uri = clipData.getItemAt(index).getUri();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    manager.addUri(uri);
                    addPreferenceFromUri(uri);
                }
            }
            updateDisplay();
        }
    }

    private void initList() {
        //Debug.waitForDebugger();

        Preference progressBarPreference = new Preference(getContext());
        progressBarPreference.setWidgetLayoutResource(R.layout.progress_bar_preference);
        progressBarPreference.setKey("progress_bar");
        getPreferenceScreen().addPreference(progressBarPreference);

        List<Uri> uris = manager.getImageUris(manager.getCurrentOrdering(getResources()));
        AsyncTaskLoadImages task = new AsyncTaskLoadImages(progressBarPreference);
        task.execute(uris.toArray(new Uri[uris.size()]));
    }

    private void addPreferenceFromUri(Uri uri) {
        ImagePreference preference = new ImagePreference(getContext());

        preference.setOnDeleteClickListener(new ImagePreferenceDeleteClickListener(uri));

        try {
            ImageInfo imageInfo = ImageLoader.loadImage(uri, getContext(), 100, 100);
            preference.setTitle(imageInfo.getName());
            preference.setImageBitmap(imageInfo.getImage());
        } catch (IOException e) {
            Log.e(ImagesPreferenceFragment.class.getSimpleName(), "Error opening file", e);
            preference.setTitle(R.string.error_reading_file);
            preference.setSummary(e.getClass().getName() + ": " + e.getLocalizedMessage());
        }
        imagePreferences.add(preference);
    }

    private void updateDisplay() {
        getPreferenceScreen().removeAll();
        if (addPreference == null) {
            addPreference = new Preference(getContext());
            addPreference.setTitle(getResources().getString(R.string.preference_pick_folder));
            addPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    } else {
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                    }
                    startActivityForResult(intent, REQUEST_CODE_FILE);
                    return true;
                }
            });
        }
        getPreferenceScreen().addPreference(addPreference);

        for (ImagePreference imagePreference : imagePreferences) {
            getPreferenceScreen().addPreference(imagePreference);
        }
    }

    private class ImagePreferenceDeleteClickListener implements OnDeleteClickListener {

        private Uri uri;

        public ImagePreferenceDeleteClickListener(Uri uri) {
            this.uri = uri;
        }

        @Override
        public void onDeleteButtonClicked(ImagePreference view) {
            manager.removeUri(uri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getContext().getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            imagePreferences.remove(view);
            updateDisplay();
        }
    }

    private class AsyncTaskLoadImages extends AsyncTask<Uri, BigDecimal, List<ImagePreference>> {

        private Preference progressBarPreference;

        public AsyncTaskLoadImages(Preference progressBarPreference) {
            this.progressBarPreference = progressBarPreference;
        }

        @Override
        protected List<ImagePreference> doInBackground(Uri... uris) {
            List<ImagePreference> bitmaps = new ArrayList<>(uris.length);
            BigDecimal listSize = BigDecimal.valueOf(uris.length);
            for (Uri uri : uris) {
                bitmaps.add(loadBitmap(uri));
                publishProgress(BigDecimal.valueOf(bitmaps.size()).divide(listSize, 2, RoundingMode.HALF_UP));
            }
            return bitmaps;
        }

        private ImagePreference loadBitmap(Uri uri) {
            ImagePreference preference = new ImagePreference(getContext());
            preference.setOnDeleteClickListener(new ImagePreferenceDeleteClickListener(uri));

            try {
                ImageInfo info = ImageLoader.loadImage(uri, getContext(), 100, 100);
                preference.setTitle(info.getName());
                preference.setImageBitmap(info.getImage());
            } catch (IOException e) {
                Log.e(ImagesPreferenceFragment.class.getSimpleName(), "Error opening file", e);
                preference.setTitle(R.string.error_reading_file);
                preference.setSummary(e.getClass().getName() + ": " + e.getLocalizedMessage());
            }


            return preference;
        }

        @Override
        protected void onProgressUpdate(BigDecimal... values) {

        }

        @Override
        protected void onPostExecute(List<ImagePreference> imagePreferences) {
            ImagesPreferenceFragment.this.imagePreferences = new HashSet<>(imagePreferences);
            updateDisplay();
            getPreferenceScreen().removePreference(progressBarPreference);
        }
    }
}
