package de.tobi.slideshowwallpaper.utilities;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.preferences.ImagesPreferenceFragment;

public class ImageLoader {

    public static ImageInfo loadImage(String uri, Context context, int maxWidth, int maxHeight) throws IOException {
        Bitmap bitmap = null;
        String name = null;
        int size = 0;

        Cursor fileCursor = null;
        InputStream in = null;
        try  {
            fileCursor = context.getContentResolver().query(Uri.parse(uri), null, null, null, null);
            int nameIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            fileCursor.moveToFirst();
            name = fileCursor.getString(nameIndex);

            int sizeIndex = fileCursor.getColumnIndex(OpenableColumns.SIZE);
            fileCursor.moveToFirst();
            size = Integer.parseInt(fileCursor.getString(sizeIndex));


            in = context.getContentResolver().openInputStream(Uri.parse(uri));
            if (in != null) {
                byte[] bytes = readStream(in, size);
                bitmap = readBitmap(bytes, maxWidth, maxHeight);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileCursor != null) {
                fileCursor.close();
            }
        }

        return new ImageInfo(name, size, bitmap);
    }

    private static byte[] readStream(InputStream in, int size) throws IOException {
        byte[] bytes = new byte[size];
        in.read(bytes);
        return bytes;
    }

    private static Bitmap readBitmap(byte[] bytes, int maxWidth, int maxHeight) {
        int size = bytes.length;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes,0, size, options);
        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;
        if (imageWidth > imageHeight) {
            imageWidth = options.outHeight;
            imageHeight = options.outWidth;
        }
        options.inSampleSize = calculateSampleSize(imageWidth, imageHeight, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        options.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, size, options);
        if (bitmap.getWidth() > bitmap.getHeight()) {
            Matrix matrix = new Matrix();
            matrix.setRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        return bitmap;
    }

    private static int calculateSampleSize(int width, int height, int desiredWidth, int desiredHeight) {
        int result = 1;

        if (width > desiredWidth || height > desiredHeight) {
            int halfWidth = width / 2;
            int halfHeight = height / 2;

            while ((halfWidth / result) >= desiredWidth && (halfHeight / result >= desiredHeight)) {
                result *= 2;
            }
        }
        return result;
    }
}
