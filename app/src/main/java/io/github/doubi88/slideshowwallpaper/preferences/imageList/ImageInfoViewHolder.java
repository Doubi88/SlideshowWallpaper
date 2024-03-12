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

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.listeners.OnSelectListener;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ImageLoader;
import io.github.doubi88.slideshowwallpaper.utilities.ProgressListener;

public class ImageInfoViewHolder extends RecyclerView.ViewHolder implements ProgressListener<Uri, BigDecimal, List<ImageInfo>> {

    private final int height;
    private final int width;
    private ImageInfo imageInfo;

    private boolean imageIsSelected;

    private final FrameLayout frameLayout;
    private final ImageView imageView;
    private final ImageView checkIcon;
    private final ProgressBar progressBar;

    private LinkedList<OnSelectListener> listeners;


    public ImageInfoViewHolder(View itemView) {
        super(itemView);
        listeners = new LinkedList<>();
        imageView = itemView.findViewById(R.id.image_view);
        checkIcon = itemView.findViewById(R.id.check_icon);
        frameLayout = itemView.findViewById(R.id.frame_layout);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSelection();
            }
        });
        progressBar = itemView.findViewById(R.id.progress_bar);

        height = imageView.getResources().getDimensionPixelSize(R.dimen.image_preview_height);
        width = itemView.getWidth();
    }

    public void setUri(Uri uri) {
        DeselectImage();
        if (imageInfo == null || !uri.equals(imageInfo.getUri())) {
            imageInfo = ImageLoader.loadFileNameAndSize(uri, imageView.getContext());
        }
    }

    private void toggleSelection() {
        if (imageIsSelected) {
            DeselectImage();
            notifyOnImageDeselected();
        } else {
            SelectImage();
            notifyOnImageSelected();
        }
    }
    private void DeselectImage() {
        Log.i(ImageInfoViewHolder.class.getSimpleName(), "Deselect the image");

        this.frameLayout.setPadding(0,0,0,0);
        this.frameLayout.setBackgroundColor(ContextCompat.getColor(imageView.getContext(), R.color.secondaryTextColor));
        this.checkIcon.setVisibility(View.GONE);

        imageIsSelected = false;
    }

    private void SelectImage() {
        Log.i(ImageInfoViewHolder.class.getSimpleName(), "Select the image");

        this.frameLayout.setPadding(10,10,10,10);
        this.frameLayout.setBackgroundColor(ContextCompat.getColor(imageView.getContext(), R.color.primaryLightColor));
        this.checkIcon.setVisibility(View.VISIBLE);

        imageIsSelected = true;
    }

    public Uri getUri() {
        return imageInfo.getUri();
    }

    @Override
    public void onProgressChanged(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, BigDecimal current, BigDecimal max) {
        progressBar.setMax(max.intValue());
        progressBar.setProgress(current.intValue());
    }

    @Override
    public void onTaskFinished(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
        if (imageInfos.size() == 1) {
            imageInfo = imageInfos.get(0);
            if (imageInfo.getImage() != null) {
                Matrix matrix = ImageLoader.calculateMatrixScaleToFit(imageInfo.getImage(), width, height, false);
                imageView.setImageBitmap(Bitmap.createBitmap(imageInfo.getImage(), 0, 0, imageInfo.getImage().getWidth(), imageInfo.getImage().getHeight(), matrix, false));
            }
            progressBar.setVisibility(View.GONE);

        }
    }

    @Override
    public void onTaskCancelled(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
        if (imageInfos != null) {
            for (ImageInfo info : imageInfos) {
                info.getImage().recycle();
            }
        }
    }

    public void setOnSelectListener(OnSelectListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void notifyOnImageSelected(){
        for (OnSelectListener listener : listeners) {
            listener.onImageSelected(imageInfo);
        }
    }

    public void notifyOnImageDeselected(){
        for (OnSelectListener listener : listeners) {
            listener.onImagedDeselected(imageInfo);
        }
    }
}