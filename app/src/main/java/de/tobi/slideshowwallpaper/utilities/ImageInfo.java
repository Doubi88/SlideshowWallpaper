package de.tobi.slideshowwallpaper.utilities;

import android.graphics.Bitmap;

public class ImageInfo {

    private String name;
    private int size;
    private Bitmap image;

    public ImageInfo(String name, int size, Bitmap image) {
        this.name = name;
        this.size = size;
        this.image = image;
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
}
