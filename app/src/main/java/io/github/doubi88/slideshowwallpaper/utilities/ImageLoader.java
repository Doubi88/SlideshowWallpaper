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

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.github.doubi88.slideshowwallpaper.R;

public class ImageLoader {

    /**
     * Loads the image file and name, without loading the image itself. {@link ImageInfo#getImage()} will return {@code null}
     * @param uri The {@link Uri} to load the image from
     * @param context The {@link Context} to use
     * @return An {@link ImageInfo} object, only containing uri, name and size. {@link ImageInfo#getImage()} will be {@code null}
     */
    public static ImageInfo loadFileNameAndSize(@NonNull Uri uri, @NonNull Context context) {
        String name = uri.getLastPathSegment();
        int size = 0;
        String[] columns = {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        try (Cursor cursor = context.getContentResolver().query(uri, columns, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex);
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = (int) Math.max(0, Math.min(Integer.MAX_VALUE, cursor.getLong(sizeIndex)));
                }
            }
        } catch (RuntimeException e) {
            name = context.getString(R.string.error_reading_file);
        }
        return new ImageInfo(uri, name, size, null);
    }

    @NonNull
    public static ImageInfo loadImage(@NonNull Uri uri, @NonNull Context context,
                                      int desiredWidth, int desiredHeight,
                                      boolean considerMemory) throws IOException {
        ImageInfo info = loadFileNameAndSize(uri, context);
        // Photo Picker providers may omit the encoded file size. Reopen streams for each pass.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream stream = openImage(uri, context)) {
            BitmapFactory.decodeStream(stream, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new IOException("Unsupported or corrupt image");
        }
        int rotation = 0;
        boolean flipped = false;
        try (InputStream stream = openImage(uri, context)) {
            ExifInterface exif = new ExifInterface(stream);
            rotation = exif.getRotationDegrees();
            flipped = exif.isFlipped();
        } catch (IOException ignored) {
            // Some decodable formats do not expose EXIF metadata.
        }
        boolean swap = rotation == 90 || rotation == 270;
        int sample = 1;
        int targetWidth = Math.max(1, swap ? desiredHeight : desiredWidth);
        int targetHeight = Math.max(1, swap ? desiredWidth : desiredHeight);
        while (options.outWidth / (2L * sample) >= targetWidth
                && options.outHeight / (2L * sample) >= targetHeight) {
            sample *= 2;
        }
        long maxPixels = Math.min(8_000_000L, Runtime.getRuntime().maxMemory() /
                (considerMemory ? 48 : 32));
        while (((options.outWidth + (long) sample - 1) / sample)
                * ((options.outHeight + (long) sample - 1) / sample) > maxPixels) {
            sample *= 2;
        }
        options.inSampleSize = sample;
        options.inJustDecodeBounds = false;
        Bitmap bitmap;
        try (InputStream stream = openImage(uri, context)) {
            bitmap = BitmapFactory.decodeStream(stream, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Unsupported or corrupt image");
        }
        if (rotation != 0 || flipped) {
            Matrix matrix = new Matrix();
            matrix.setScale(flipped ? -1f : 1f, 1f);
            matrix.postRotate(rotation);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, false);
            if (rotated != bitmap) {
                bitmap.recycle();
            }
            bitmap = rotated;
        }
        return new ImageInfo(uri, info.getName(), info.getSize(), bitmap);
    }

    private static InputStream openImage(Uri uri, Context context) throws IOException {
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            if (stream == null) {
                throw new FileNotFoundException();
            }
            return stream;
        } catch (SecurityException | IllegalArgumentException e) {
            throw new IOException("Image unavailable", e);
        }
    }

    public static float calculateScaleFactorToFit(Bitmap bitmap, int screenWidth, int screenHeight, boolean both) {
        float scale = 0;
        if (both) {
            scale = Math.min((float) screenWidth / (float) bitmap.getWidth(), (float) screenHeight / (float) bitmap.getHeight());
        } else {
            scale = Math.max((float) screenWidth / (float) bitmap.getWidth(), (float) screenHeight / (float) bitmap.getHeight());
        }
        return scale;
    }
    public static Matrix calculateMatrixScaleToFit(Bitmap bitmap, int screenWidth, int screenHeight, boolean both) {
        Matrix result = new Matrix();

        float scale = calculateScaleFactorToFit(bitmap, screenWidth, screenHeight, both);
        float yTranslate = 0;
        if (screenHeight > (bitmap.getHeight() * scale)) {
            yTranslate = (screenHeight - bitmap.getHeight() * scale) / 2f;
        }
        float xTranslate = 0;
        if (screenWidth > (bitmap.getWidth() * scale)) {
            xTranslate = (screenWidth - bitmap.getWidth() * scale) / 2f;
        }

        result.setScale(scale, scale);
        result.postTranslate(xTranslate, yTranslate);
        return result;
    }
}
