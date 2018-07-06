package de.tobi.slideshowwallpaper.utilities;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class ImageLoader {

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
    public static ImageInfo loadImage(@NonNull Uri uri, @NonNull Context context, int desiredWidth, int desiredHeight) throws IOException {
        Bitmap bitmap = null;
        String name = null;
        int size = 0;

        Cursor fileCursor = null;
        InputStream in = null;
        try  {
            fileCursor = context.getContentResolver().query(uri, null, null, null, null);
            if (fileCursor != null) {
                int nameIndex = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileCursor.moveToFirst();
                name = fileCursor.getString(nameIndex);

                int sizeIndex = fileCursor.getColumnIndex(OpenableColumns.SIZE);
                fileCursor.moveToFirst();
                size = Integer.parseInt(fileCursor.getString(sizeIndex));


                in = context.getContentResolver().openInputStream(uri);
                if (in != null) {
                    byte[] bytes = readStream(in, size);
                    bitmap = readBitmap(bytes, desiredWidth, desiredHeight);
                }
            } else {
                Log.e(ImageLoader.class.getSimpleName(), "Could not load file " + uri.toString());
                name = "Error loading image";
                size = 0;
                bitmap = null;
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

    @NonNull
    private static byte[] readStream(@NonNull InputStream in, int size) throws IOException {
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
        if ((imageWidth > imageHeight && maxHeight > maxWidth) || (imageHeight > imageWidth) && (maxWidth > maxHeight)) {
            // swapping width and height is intended here
            //noinspection SuspiciousNameCombination
            imageWidth = options.outHeight;
            //noinspection SuspiciousNameCombination
            imageHeight = options.outWidth;
        }
        options.inSampleSize = calculateSampleSize(imageWidth, imageHeight, maxWidth, maxHeight);
        options.inJustDecodeBounds = false;
        options.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, size, options);
        if ((bitmap.getWidth() > bitmap.getHeight() && maxHeight > maxWidth) || (bitmap.getHeight() > bitmap.getWidth() && maxWidth > maxHeight)) {
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

    public static Matrix calculateMatrixScaleToFit(Bitmap bitmap, int screenWidth, int screenHeight) {
        Matrix result = new Matrix();

        float scale = Math.max((float)screenWidth / (float)bitmap.getWidth(), (float)screenHeight / (float)bitmap.getHeight());
        float yTranslate = (screenHeight - bitmap.getHeight() * scale) / 2f;

        result.setScale(scale, scale);
        result.postTranslate(0, yTranslate);
        return result;
    }
}
