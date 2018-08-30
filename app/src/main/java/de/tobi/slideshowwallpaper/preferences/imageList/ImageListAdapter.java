package de.tobi.slideshowwallpaper.preferences.imageList;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;
import de.tobi.slideshowwallpaper.utilities.AsyncTaskLoadImages;

public class ImageListAdapter extends RecyclerView.Adapter<ImageInfoViewHolder> {

    private List<Uri> uris;
    private List<OnDeleteClickListener> listeners;
    private HashMap<Uri, AsyncTaskLoadImages> loading;


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
        Log.d(ImageListAdapter.class.getSimpleName(), "Loading image " + position);
        AsyncTaskLoadImages asyncTask = loading.get(uris.get(position));
        if (asyncTask == null) {
            Log.d(ImageListAdapter.class.getSimpleName(), "Starting new AsyncTask for " + position);
            asyncTask = new AsyncTaskLoadImages(holder.itemView.getContext(), holder.itemView.getWidth(), holder.itemView.getResources().getDimensionPixelSize(R.dimen.image_preview_height));
            loading.put(uris.get(position), asyncTask);

            asyncTask.execute(uris.get(position));
        } else {
            Log.d(ImageListAdapter.class.getSimpleName(), "AsyncTask for " + position + " is already there.");
        }
        asyncTask.addProgressListener(holder);
        holder.setUri(uris.get(position));
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
