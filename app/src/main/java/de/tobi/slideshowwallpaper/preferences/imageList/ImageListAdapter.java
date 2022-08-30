package de.tobi.slideshowwallpaper.preferences.imageList;

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

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;
import de.tobi.slideshowwallpaper.utilities.AsyncTaskLoadImages;
import de.tobi.slideshowwallpaper.utilities.ImageInfo;
import de.tobi.slideshowwallpaper.utilities.ProgressListener;

public class ImageListAdapter extends RecyclerView.Adapter<ImageInfoViewHolder> {

    private List<Uri> uris;
    private List<OnDeleteClickListener> listeners;
    private HashMap<Uri, AsyncTaskLoadImages> loading;
    private HashMap<Uri, ImageInfoViewHolder> activeHolders;


    public ImageListAdapter(List<Uri> uris) {
        this.uris = new ArrayList<>(uris);
        listeners = new LinkedList<>();
        loading = new HashMap<>();
    }

    @Override
    public ImageInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_list_entry, parent, false);
        ImageInfoViewHolder holder = new ImageInfoViewHolder(view);
        holder.setOnDeleteButtonClickListener(new OnDeleteClickListener() {
            @Override
            public void onDeleteButtonClicked(Uri uri) {
                delete(uri);
            }
        });
        return holder;
    }

    public void addOnDeleteClickListener(OnDeleteClickListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners(Uri uri) {
        for (OnDeleteClickListener listener : listeners) {
            listener.onDeleteButtonClicked(uri);
        }
    }

    @Override
    public void onBindViewHolder(ImageInfoViewHolder holder, int position) {
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

    private void delete(Uri uri) {
        int index = uris.indexOf(uri);
        uris.remove(index);
        notifyItemRemoved(index);

        notifyListeners(uri);
    }

    public void addUris(List<Uri> uris) {
        int oldSize = this.uris.size();
        this.uris.addAll(uris);

        notifyItemRangeInserted(oldSize, uris.size());
    }
}
