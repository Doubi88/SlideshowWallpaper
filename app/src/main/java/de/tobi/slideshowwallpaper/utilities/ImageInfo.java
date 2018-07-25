package de.tobi.slideshowwallpaper.utilities;

import android.graphics.Bitmap;
import android.net.Uri;

public class ImageInfo {

    private Uri uri;
    private String name;
    private int size;
    private Bitmap image;

    public ImageInfo(Uri uri, String name, int size, Bitmap image) {
        this.uri = uri;
        this.name = name;
        this.size = size;
        this.image = image;
    }

    public Uri getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if ((obj != null) && (obj instanceof ImageInfo)) {
            result = ((obj == this) || (((ImageInfo) obj).getUri().equals(getUri())));
        }
        return result;
    }

    @Override
    public int hashCode() {
        return getUri().hashCode();
    }
}
