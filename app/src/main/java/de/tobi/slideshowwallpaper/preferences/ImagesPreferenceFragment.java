package de.tobi.slideshowwallpaper.preferences;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.provider.OpenableColumns;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;

public class ImagesPreferenceFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_FILE = 1;

    private Preference addPreference;
    private Set<ImagePreference> imagePreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.images_preferences);
        initList();
        updateDisplay();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK) {

            Set<String> uris = new HashSet<>();
            ClipData clipData = data.getClipData();
            if (clipData == null) {
                Uri uri = data.getData();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                uris.add(uri.toString());
                addPreferenceFromUri(uri.toString());
            } else {

                for (int index = 0; index < clipData.getItemCount(); index++) {
                    Uri uri = clipData.getItemAt(index).getUri();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    uris.add(uri.toString());
                    addPreferenceFromUri(uri.toString());
                }
            }
            saveFilesPreference(uris);
            updateDisplay();
        }
    }

    private void initList() {
        //Debug.waitForDebugger();
        imagePreferences = new HashSet<>();
        Set<String> uris = getPreferenceManager().getSharedPreferences().getStringSet(getResources().getString(R.string.preference_pick_folder_key), new HashSet<String>());
        for (String uri : uris) {
            addPreferenceFromUri(uri);
        }
    }

    private void addPreferenceFromUri(String uri) {
        ImagePreference preference = new ImagePreference(getContext());

        preference.setOnDeleteClickListener(new ImagePreferenceDeleteClickListener(uri));

        Cursor fileCursor = getContext().getContentResolver().query(Uri.parse(uri), null, null, null, null);
        int nameIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        fileCursor.moveToFirst();
        String name = fileCursor.getString(nameIndex);
        preference.setTitle(name);

        int sizeIndex = fileCursor.getColumnIndex(OpenableColumns.SIZE);
        fileCursor.moveToFirst();
        int size = Integer.parseInt(fileCursor.getString(sizeIndex));

        InputStream in = null;
        try {
            in = getContext().getContentResolver().openInputStream(Uri.parse(uri));
            if (in != null) {
                byte[] bytes = readStream(in, size);
                preference.setImageBitmap(readBitmap(bytes));
            }
        } catch (IOException e) {
            Log.e(ImagesPreferenceFragment.class.getSimpleName(), "Error opening file", e);
            preference.setSummary(getResources().getString(R.string.error_reading_file) + ": " + e.getClass().getName() + ": " + e.getLocalizedMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        imagePreferences.add(preference);
    }

    private void saveFilesPreference(Set<String> values) {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        Set<String> currentValue = prefs.getStringSet(getResources().getString(R.string.preference_pick_folder_key), new HashSet<String>());
        Set<String> newValue = new HashSet<>(currentValue);
        newValue.addAll(values);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(getResources().getString(R.string.preference_pick_folder_key), newValue);
        editor.apply();
    }

    private Bitmap readBitmap(byte[] bytes) {
        int size = bytes.length;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes,0, size, options);

        options.inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, 100, 100);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(bytes, 0, size, options);
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

    private byte[] readStream(InputStream in, int size) throws IOException {
        byte[] bytes = new byte[size];
        in.read(bytes);
        return bytes;
    }

    private int calculateSampleSize(int width, int height, int desiredWidth, int desiredHeight) {
        int result = 1;

        if (width > desiredWidth || height > desiredHeight) {
            int halfWidth = width / 2;
            int halfHeight = height / 2;

            while ((halfWidth / result) >= desiredWidth && (halfHeight / result >= desiredHeight)) {
                result *= 2;
            }
        }
        return result;
    }

    private class ImagePreferenceDeleteClickListener implements OnDeleteClickListener {

        private String uri;

        public ImagePreferenceDeleteClickListener(String uri) {
            this.uri = uri;
        }

        @Override
        public void onDeleteButtonClicked(ImagePreference view) {
            Set<String> uris = getPreferenceManager().getSharedPreferences().getStringSet(getResources().getString(R.string.preference_pick_folder_key), new HashSet<String>());

            HashSet<String> newSet = new HashSet<>();
            for (String uri : uris) {
                if (!uri.equals(this.uri)) {
                    newSet.add(uri);
                }
            }

            SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
            editor.putStringSet(getResources().getString(R.string.preference_pick_folder_key), newSet);
            editor.apply();

            imagePreferences.remove(view);
            updateDisplay();
        }
    }
}
