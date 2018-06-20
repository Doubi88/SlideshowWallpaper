package de.tobi.slideshowwallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.widget.ImageView;

public class ImagePreference extends Preference {

    private Drawable image;
    private Bitmap imageBitmap;

    public ImagePreference(Context context) {
        super(context);
        setLayoutResource(R.layout.image_list_entry);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ImageView imageView = (ImageView)holder.findViewById(R.id.image_view);

        if (image != null) {
            imageView.setImageDrawable(image);
        } else if (imageBitmap != null) {
            imageView.setImageBitmap(imageBitmap);
        }
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public void setImageBitmap(Bitmap bitmap) {
        this.imageBitmap = bitmap;
    }
}
