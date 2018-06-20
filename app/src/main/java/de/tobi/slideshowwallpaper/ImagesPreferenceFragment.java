package de.tobi.slideshowwallpaper;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

public class ImagesPreferenceFragment extends PreferenceFragmentCompat {

    private static final int REQUEST_CODE_FILE = 1;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getContext());
        setPreferenceScreen(preferenceScreen);
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

            preferenceScreen.addPreference(preference);
        }

        Preference addPreference = new Preference(getContext());
        addPreference.setTitle(getResources().getString(R.string.preference_pick_folder));
        addPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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

        preferenceScreen.addPreference(addPreference);

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
