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
package io.github.doubi88.slideshowwallpaper.utilities;

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
