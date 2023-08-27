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
import java.util.LinkedList;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.listeners.OnDeleteClickListener;
import io.github.doubi88.slideshowwallpaper.utilities.AsyncTaskLoadImages;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ProgressListener;

public class ImageListAdapter extends RecyclerView.Adapter<ImageInfoViewHolder> {

    private List<Uri> uris;
    private List<OnDeleteClickListener> listeners;
    private HashMap<Uri, AsyncTaskLoadImages> loading;


    public ImageListAdapter(List<Uri> uris) {
        this.uris = new ArrayList<>(uris);
        listeners = new LinkedList<>();
        loading = new HashMap<>();
    }

    @NonNull
    @Override
    public ImageInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_list_entry, parent, false);
        ImageInfoViewHolder holder = new ImageInfoViewHolder(view);
        holder.setOnDeleteButtonClickListener(new OnDeleteClickListener() {
            @Override
            public void onDeleteButtonClicked(ImageInfo info) {
                delete(info);
            }
        });
        return holder;
    }

    public void addOnDeleteClickListener(OnDeleteClickListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners(ImageInfo info) {
        for (OnDeleteClickListener listener : listeners) {
            listener.onDeleteButtonClicked(info);
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

    private void delete(ImageInfo info) {
        int index = uris.indexOf(info.getUri());
        uris.remove(index);
        notifyItemRemoved(index);

        notifyListeners(info);
    }

    public void addUris(List<Uri> uris) {
        int oldSize = this.uris.size();
        this.uris.addAll(uris);

        notifyItemRangeInserted(oldSize, uris.size());
    }
}
