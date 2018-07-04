package de.tobi.slideshowwallpaper.preferences;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;

public class ImagePreference extends Preference {

    private Bitmap imageBitmap;

    private ArrayList<OnDeleteClickListener> deleteClickListeners;

    public ImagePreference(Context context) {
        super(context);
        setWidgetLayoutResource(R.layout.image_list_entry);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        updateImage(holder);

        ImageView deleteButton = (ImageView)holder.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fireOnDeleteClickedEvent();
            }
        });
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        if (deleteClickListeners == null) {
            deleteClickListeners = new ArrayList<>(3);
        }
        deleteClickListeners.add(listener);
    }

    private void fireOnDeleteClickedEvent() {
        if (deleteClickListeners != null) {
            for (OnDeleteClickListener listener : deleteClickListeners) {
                listener.onDeleteButtonClicked(this);
            }
        }
    }

    private void updateImage(PreferenceViewHolder holder) {
        ImageView imageView = (ImageView)holder.findViewById(R.id.image_preference_view);

        if (imageBitmap != null) {
            imageView.setImageBitmap(imageBitmap);
        }
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.imageBitmap = bitmap;
    }
}
