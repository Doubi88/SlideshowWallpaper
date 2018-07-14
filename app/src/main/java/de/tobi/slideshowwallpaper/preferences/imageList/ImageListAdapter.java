package de.tobi.slideshowwallpaper.preferences.imageList;

import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;

public class ImageListAdapter extends RecyclerView.Adapter<ImageInfoViewHolder> {

    private List<Uri> imageInfos;
    private List<OnDeleteClickListener> listeners;

    public ImageListAdapter(List<Uri> imageInfos) {
        this.imageInfos = new ArrayList<>(imageInfos);
        listeners = new LinkedList<>();
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
        holder.setImageInfo(imageInfos.get(position));
    }

    @Override
    public int getItemCount() {
        return imageInfos.size();
    }

    public void delete(Uri uri) {
        int index = imageInfos.indexOf(uri);
        imageInfos.remove(index);
        notifyItemRemoved(index);

        notifyListeners(uri);
    }

    public void addImageInfos(List<Uri> infos) {
        int oldSize = imageInfos.size();
        imageInfos.addAll(infos);

        notifyItemRangeInserted(oldSize, infos.size());
    }
}
