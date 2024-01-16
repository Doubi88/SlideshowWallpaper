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

import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.listeners.OnSelectListener;
import io.github.doubi88.slideshowwallpaper.utilities.AsyncTaskLoadImages;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ProgressListener;

public class ImageListAdapter extends RecyclerView.Adapter<ImageInfoViewHolder> {

    private List<Uri> uris;
    private List<OnSelectListener> listeners;
    private HashMap<Uri, AsyncTaskLoadImages> loading;
    private HashSet<ImageInfo> selectedImages;


    public ImageListAdapter(List<Uri> uris) {
        this.selectedImages = new HashSet<>();
        this.uris = new ArrayList<>(uris);
        listeners = new LinkedList<>();
        loading = new HashMap<>();
    }

    @NonNull
    @Override
    public ImageInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_list_entry, parent, false);
        ImageInfoViewHolder holder = new ImageInfoViewHolder(view);
        holder.setOnSelectListener(new OnSelectListener() {
            @Override
            public void onImageSelected(ImageInfo info) {
                select(info);
            }

            @Override
            public void onImagedDeselected(ImageInfo info) {
                deselect(info);
            }

            @Override
            public void onSelectionChanged(HashSet<ImageInfo> setInfo) {}
        });
        return holder;
    }

    public void addOnSelectListener(OnSelectListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (OnSelectListener listener : listeners) {
            listener.onSelectionChanged(this.selectedImages);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ImageInfoViewHolder holder, int position) {
        final Uri uri = uris.get(position);
        AsyncTaskLoadImages asyncTask = loading.get(uri);
        if (asyncTask == null) {
            asyncTask = new AsyncTaskLoadImages(holder.itemView.getContext(), holder.itemView.getWidth(), holder.itemView.getResources().getDimensionPixelSize(R.dimen.image_preview_height));
            loading.put(uri, asyncTask);

            asyncTask.addProgressListener(new ProgressListener<Uri, BigDecimal, List<ImageInfo>>() {
                @Override
                public void onProgressChanged(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, BigDecimal current, BigDecimal max) {

                }

                @Override
                public void onTaskFinished(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
                    loading.remove(uri);
                }

                @Override
                public void onTaskCancelled(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
                    loading.remove(uri);
                }
            });
            asyncTask.execute(uri);
        }
        asyncTask.addProgressListener(holder);
        holder.setUri(uri);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ImageInfoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        AsyncTaskLoadImages task = loading.get(holder.getUri());
        if (task != null) {
            task.cancel(false);
            loading.remove(holder.getUri());
        }
    }

    @Override
    public int getItemCount() {
        return uris.size();
    }

    public void delete(HashSet<ImageInfo> imageInfos) {
        for (ImageInfo imageInfo : imageInfos){
            int index = uris.indexOf(imageInfo.getUri());
            uris.remove(index);
            notifyItemRemoved(index);
        }
        selectedImages.clear();
        notifyListeners();
    }

    private void select(ImageInfo info){
        selectedImages.add(info);
        for (OnSelectListener listener: this.listeners){
            listener.onSelectionChanged(this.selectedImages);
        }
    }

    private void deselect(ImageInfo info){
        selectedImages.remove(info);
        for (OnSelectListener listener: this.listeners){
            listener.onSelectionChanged(this.selectedImages);
        }
    }

    public HashSet<ImageInfo> getSelectedImages(){
        return this.selectedImages;
    }

    public void addUris(List<Uri> uris) {
        int oldSize = this.uris.size();
        this.uris.addAll(uris);

        notifyItemRangeInserted(oldSize, uris.size());
    }
}
