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
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import android.util.Log;

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
        String name = null;
        int size = 0;
        Cursor fileCursor = null;
        ParcelFileDescriptor descriptor = null;
        try {

            fileCursor = context.getContentResolver().query(uri, null, null, null, null);
            if (fileCursor != null) {
                int nameIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                boolean cursorContainsData = fileCursor.moveToFirst();
                if (cursorContainsData) {
                    name = fileCursor.getString(nameIndex);
                    int sizeIndex = fileCursor.getColumnIndex(OpenableColumns.SIZE);
                    fileCursor.moveToFirst();
                    size = Integer.parseInt(fileCursor.getString(sizeIndex));
                }
                else {
                    Log.e("FileCursor error", "FileCursor: " + fileCursor.toString() + " nameIndex: " + String.valueOf(nameIndex));
                    name = "Error loading filename";
                }

            } else {
                Log.e(ImageLoader.class.getSimpleName(), "Could not load file " + uri.toString());
                name = context.getResources().getString(R.string.error_reading_file);
            }
            return new ImageInfo(uri, name, size, null);
        } catch (SecurityException e) {
            return new ImageInfo(null, "Cannot access image", 0, null);
        } catch (IllegalArgumentException e) {
            return new ImageInfo(null, "Cannot access image", 0, null);
        } finally {
            if (fileCursor != null) {
                fileCursor.close();
            }
        }
    }

    /**
     * Loads the {@link Bitmap} from the given file Uri. Turns it by 90 degrees, if it is wider than high (horizontal)
     * The resulting image will have either {@code desiredHeight} as height or {@code desiredWidth} as width
     *
     * @param uri The {@link Uri} to load the image from
     * @param context The current {@link Context}
     * @param desiredWidth The width of the element displaying the image
     * @param desiredHeight The height of the element displaying the image
     * @return A {@link ImageInfo} containing the info of the image. {@link ImageInfo#getImage()} may be {@code null}, if reading the file fails.
     * @throws IOException If thrown when trying to read the file
     */
    @NonNull
    public static ImageInfo loadImage(@NonNull Uri uri, @NonNull Context context, int desiredWidth, int desiredHeight, boolean considerMemory) throws IOException {
        Bitmap bitmap = null;
        ImageInfo info = null;
        InputStream in = null;
        try  {
            info = loadFileNameAndSize(uri, context);
            int retried = 0;
            if (info.getUri() != null) {
                do {
                    try {
                        in = context.getContentResolver().openInputStream(uri);
                    } catch (SecurityException e) {
                        // Permission denied. Show image as error
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }
                            retried++;
                        } catch (Exception e2) {
                            retried = 2; // No longer retry
                        }
                    }
                } while (in == null && retried < 2);
            }
            if (in != null) {

                int degrees = getRotationDegrees(context, uri);
                if (info.getSize() > 0) {
                    bitmap = readBitmap(readStream(in, info.getSize()), desiredWidth, desiredHeight, considerMemory);
                    Matrix matrix = new Matrix();
                    matrix.setRotate(degrees);

                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                    info = new ImageInfo(uri, info.getName(), info.getSize(), bitmap);
                }

            }
            else {
                info = new ImageInfo(uri, context.getResources().getString(R.string.error_reading_file), 0, null);
            }

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return info;
    }

    private static int getRotationDegrees(Context context, Uri uri) throws IOException {
        int result = 0;
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            result = new ExifInterface(inputStream).getRotationDegrees();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return result;
    }

    @NonNull
    private static byte[] readStream(@NonNull InputStream in, int size) throws IOException {
        byte[] bytes = new byte[size];
        in.read(bytes);
        return bytes;
    }

    private static Bitmap readBitmap(byte[] bytes, int maxWidth, int maxHeight, boolean considerMemory) {
        int size = bytes.length;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes,0, size, options);
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        options.inSampleSize = calculateSampleSize(imageWidth, imageHeight, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, size, options);
        if (considerMemory && Runtime.getRuntime().maxMemory() <= (bitmap.getByteCount() * 2)) {
            bitmap = null;
        }
        return bitmap;
    }

    private static int calculateSampleSize(int width, int height, int desiredWidth, int desiredHeight) {
        int result = 1;

        if (width > desiredWidth || height > desiredHeight) {

            while ((width / result) >= desiredWidth && (height / result >= desiredHeight)) {
                result *= 2;
            }
        }
        return result;
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
        float yTranslate = (screenHeight - bitmap.getHeight() * scale) / 2f;

        result.setScale(scale, scale);
        result.postTranslate(0, yTranslate);
        return result;
    }
}
