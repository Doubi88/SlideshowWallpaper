package de.tobi.slideshowwallpaper;

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
import android.provider.OpenableColumns;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import java.util.HashSet;
import java.util.Set;

public class ImagesPreferenceFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_FILE = 1;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.images_preferences);

        Preference addPreference = new Preference(getContext());
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
                intent = Intent.createChooser(intent, getResources().getString(R.string.preference_pick_folder));
                startActivityForResult(intent, REQUEST_CODE_FILE);
                return true;
            }
        });

        getPreferenceScreen().addPreference(addPreference);

        updateList();
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
            } else {

                for (int index = 0; index < clipData.getItemCount(); index++) {
                    Uri uri = clipData.getItemAt(index).getUri();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        getContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    uris.add(uri.toString());
                }
            }
            saveFilesPreference(uris);
            updateList();
        }
    }

    private void saveFilesPreference(Set<String> values) {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(getResources().getString(R.string.preference_pick_folder_key), values);
        editor.commit();
    }

    private void updateList() {
        Set<String> uris = getPreferenceManager().getSharedPreferences().getStringSet(getResources().getString(R.string.preference_pick_folder_key), new HashSet<String>());
        for (String uri : uris) {
            ImagePreference preference = new ImagePreference(getContext());

            Cursor fileCursor = getContext().getContentResolver().query(Uri.parse(uri), null, null, null, null);
            int nameIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            fileCursor.moveToFirst();

            String name = fileCursor.getString(nameIndex);
            preference.setTitle(name);

            Bitmap image = BitmapFactory.decodeFile(name);
            preference.setImageBitmap(image);

            getPreferenceScreen().addPreference(preference);
        }
    }
}
