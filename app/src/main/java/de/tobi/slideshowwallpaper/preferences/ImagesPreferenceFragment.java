package de.tobi.slideshowwallpaper.preferences;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;
import de.tobi.slideshowwallpaper.utilities.AsyncTaskLoadImages;
import de.tobi.slideshowwallpaper.utilities.ImageInfo;
import de.tobi.slideshowwallpaper.utilities.ProgressListener;

public class ImagesPreferenceFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_FILE = 1;

    private SharedPreferencesManager manager;
    private Preference addPreference;

    private LinkedList<ImagePreference> imagePreferences;

    public ImagesPreferenceFragment() {
        imagePreferences = new LinkedList<>();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.images_preferences);
        manager = new SharedPreferencesManager(getPreferenceManager().getSharedPreferences());
        loadImages(manager.getImageUris(manager.getCurrentOrdering(getResources())));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        List<Uri> uris = new LinkedList<>();
        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK) {
            ClipData clipData = data.getClipData();
            if (clipData == null) {
                Uri uri = data.getData();
                if (uri != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    manager.addUri(uri);
                    uris.add(uri);
                }
            } else {
                for (int index = 0; index < clipData.getItemCount(); index++) {
                    Uri uri = clipData.getItemAt(index).getUri();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    manager.addUri(uri);
                    uris.add(uri);
                }
            }

            loadImages(uris);
        }
    }

    private void loadImages(List<Uri> uris) {
        final Preference progressBarPreference = new Preference(getContext());
        progressBarPreference.setWidgetLayoutResource(R.layout.progress_bar_preference);
        progressBarPreference.setKey("progress_bar");
        progressBarPreference.setOrder(0);
        getPreferenceScreen().addPreference(progressBarPreference);

        AsyncTaskLoadImages task = new AsyncTaskLoadImages(getContext(), 100, 100);
        task.addProgressListener(new ProgressListener<Uri, BigDecimal, List<ImageInfo>>() {
            @Override
            public void onProgressChanged(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, BigDecimal current, BigDecimal max) {

            }

            @Override
            public void onTaskFinished(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
                getPreferenceScreen().removePreference(progressBarPreference);
                if (getContext() != null) {
                    createUI(imageInfos);
                }
            }
        });
        task.execute(uris.toArray(new Uri[uris.size()]));
    }

    private void createUI(List<ImageInfo> imagesToAdd) {
        getPreferenceScreen().removeAll();

        getPreferenceScreen().addPreference(getAddPreference());

        for (ImageInfo info : imagesToAdd) {
            addPreferenceFromImageInfo(info);
        }
        for (ImagePreference imagePreference : imagePreferences) {
            getPreferenceScreen().addPreference(imagePreference);
        }
    }

    private void addPreferenceFromImageInfo(ImageInfo info) {
        ImagePreference preference = new ImagePreference(getContext());
        preference.setOnDeleteClickListener(new ImagePreferenceDeleteClickListener(info.getUri()));
        preference.setTitle(info.getName());
        if (info.getImage() != null) {
            preference.setImageBitmap(info.getImage());
        }
        imagePreferences.add(preference);
        getPreferenceScreen().addPreference(preference);
    }

    private Preference getAddPreference() {
        if (addPreference == null) {
            addPreference = new Preference(getContext());
            addPreference.setTitle(getResources().getString(R.string.preference_add_images));
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
        return addPreference;
    }

    private class ImagePreferenceDeleteClickListener implements OnDeleteClickListener {

        private Uri uri;

        public ImagePreferenceDeleteClickListener(Uri uri) {
            this.uri = uri;
        }

        @Override
        public void onDeleteButtonClicked(ImagePreference preference) {
            manager.removeUri(uri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                getContext().getContentResolver().releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            imagePreferences.remove(preference);
            getPreferenceScreen().removePreference(preference);
        }
    }

}
