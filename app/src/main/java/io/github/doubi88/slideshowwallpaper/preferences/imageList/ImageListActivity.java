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

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.listeners.OnSelectListener;
import io.github.doubi88.slideshowwallpaper.preferences.SharedPreferencesManager;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;

public class ImageListActivity extends AppCompatActivity {

    private SharedPreferencesManager manager;

    private ImageListAdapter imageListAdapter;

    private FloatingActionButton removeButton;

    private final ActivityResultLauncher<PickVisualMediaRequest> launcher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(),
                    this::imagePickerCallback);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);

        this.removeButton = findViewById(R.id.delete_button);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        RecyclerView recyclerView = findViewById(R.id.image_list);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);
        manager = new SharedPreferencesManager(getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE));

        List<Uri> uris = manager.getImageUris(SharedPreferencesManager.Ordering.SELECTION);

        imageListAdapter = new ImageListAdapter(uris);
        imageListAdapter.addOnSelectListener(new OnSelectListener() {
            @Override
            public void onImageSelected(ImageInfo info) {

            }

            @Override
            public void onImagedDeselected(ImageInfo info) {

            }

            @Override
            public void onSelectionChanged(HashSet<ImageInfo> setInfo) {
                Log.d(ImageListActivity.class.getSimpleName(), setInfo.size() + " image(s) selected");
                if (setInfo.size() > 0) {
                    removeButton.setVisibility(View.VISIBLE);
                } else {
                    removeButton.setVisibility(View.GONE);
                }
            }
        });
        recyclerView.setAdapter(imageListAdapter);

        View addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(view -> openPicker());
        if (savedInstanceState == null && uris.isEmpty()) {
            addButton.post(this::openPicker);
        }

        this.removeButton.setOnClickListener(view -> {
            HashSet<ImageInfo> selectedImages = imageListAdapter.getSelectedImages();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.remove_confirmation_title));
            builder.setMessage(getResources().getQuantityString(R.plurals.remove_confirmation_message, selectedImages.size(), selectedImages.size()));
            builder.setPositiveButton(getString(R.string.positive_action_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    for (ImageInfo imageInfo : selectedImages) {
                        manager.removeUri(imageInfo.getUri());
                        try {
                            getContentResolver().releasePersistableUriPermission(
                                    imageInfo.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException | IllegalArgumentException ignored) {
                            // The provider may already have removed the grant.
                        }
                    }
                    imageListAdapter.delete(selectedImages);
                }
            });

            builder.setNegativeButton(getString(R.string.cancel_action_text), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            // Create and display the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPicker() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        try {
            launcher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.photo_picker_unavailable, Toast.LENGTH_LONG).show();
        }
    }

    private void imagePickerCallback(List<Uri> uris) {
        List<Uri> accessible = new ArrayList<>(uris.size());
        for (Uri uri : uris) {
            if (takePermission(uri)) {
                accessible.add(uri);
            }
        }
        imageListAdapter.addUris(manager.addUris(accessible));
        if (accessible.size() < uris.size()) {
            Toast.makeText(this, R.string.photo_access_failed, Toast.LENGTH_LONG).show();
        }
    }

    private boolean takePermission(Uri uri) {
        if (uri == null) {
            return false;
        }
        try {
            ContentResolver resolver = getContentResolver();
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            for (android.content.UriPermission grant : resolver.getPersistedUriPermissions()) {
                if (grant.isReadPermission() && uri.equals(grant.getUri())) {
                    return true;
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
            // An unavailable URI cannot be added for later use.
        }
        return false;
    }
}
