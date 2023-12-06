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
package io.github.doubi88.slideshowwallpaper.preferences.imageList;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.preferences.SharedPreferencesManager;

public class ImageListActivity extends AppCompatActivity {

    private SharedPreferencesManager manager;

    private ImageListAdapter imageListAdapter;

    private final ActivityResultLauncher<PickVisualMediaRequest> launcher = registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(), this::imagePickerCallback);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        RecyclerView recyclerView = findViewById(R.id.image_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        manager = new SharedPreferencesManager(getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE));

        List<Uri> uris = manager.getImageUris(SharedPreferencesManager.Ordering.SELECTION);

        imageListAdapter = new ImageListAdapter(uris);
        imageListAdapter.addOnDeleteClickListener(info -> {
            manager.removeUri(info.getUri());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && info.getSize() > 0 && !manager.hasImageUri(info.getUri())) {
                getContentResolver().releasePersistableUriPermission(info.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        });
        recyclerView.setAdapter(imageListAdapter);

        findViewById(R.id.add_button).setOnClickListener(view -> {
            PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build();
            launcher.launch(request);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void imagePickerCallback(List<Uri> uris) {
        List<Uri> urisToAdd = new ArrayList<>(uris.size());
        for (Uri uri : uris) {
            boolean takePermissionSuccess = takePermission(uri);
            if (takePermissionSuccess && manager.addUri(uri)) {
                urisToAdd.add(uri);
            }
        }
        imageListAdapter.addUris(urisToAdd);
    }

    private boolean takePermission(Uri uri) {
        boolean takePermissionSuccess = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver res = getContentResolver();
            int perms = res.getPersistedUriPermissions().size();
            res.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // If taking the permission was unsuccessful (e.g. because the limit was reached), Don't add the uri
            if (res.getPersistedUriPermissions().size() <= perms) {
                takePermissionSuccess = false;
            }
        }
        return takePermissionSuccess;
    }
}
