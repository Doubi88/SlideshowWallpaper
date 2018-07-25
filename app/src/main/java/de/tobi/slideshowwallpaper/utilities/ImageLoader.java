package de.tobi.slideshowwallpaper.utilities;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Debug;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import de.tobi.slideshowwallpaper.R;

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
                fileCursor.moveToFirst();
                name = fileCursor.getString(nameIndex);

                int sizeIndex = fileCursor.getColumnIndex(OpenableColumns.SIZE);
                fileCursor.moveToFirst();
                size = Integer.parseInt(fileCursor.getString(sizeIndex));

            } else {
                Log.e(ImageLoader.class.getSimpleName(), "Could not load file " + uri.toString());
                name = context.getResources().getString(R.string.error_reading_file);
                size = 0;
            }
            return new ImageInfo(uri, name, size, null);
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
            in = context.getContentResolver().openInputStream(uri);
            if (in != null) {

                int degrees = getRotationDegrees(context, uri);
                bitmap = readBitmap(readStream(in, info.getSize()), desiredWidth, desiredHeight, considerMemory);
                Matrix matrix = new Matrix();
                matrix.setRotate(degrees);

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                info = new ImageInfo(uri, info.getName(), info.getSize(), bitmap);

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

    public static Matrix calculateMatrixScaleToFit(Bitmap bitmap, int screenWidth, int screenHeight, boolean both) {
        Matrix result = new Matrix();

        float scale = 0;
        if (both) {
            scale = Math.min((float) screenWidth / (float) bitmap.getWidth(), (float) screenHeight / (float) bitmap.getHeight());
        } else {
            scale = Math.max((float) screenWidth / (float) bitmap.getWidth(), (float) screenHeight / (float) bitmap.getHeight());
        }
        float yTranslate = (screenHeight - bitmap.getHeight() * scale) / 2f;

        result.setScale(scale, scale);
        result.postTranslate(0, yTranslate);
        return result;
    }
}
