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
package io.github.doubi88.slideshowwallpaper;

import android.app.WallpaperColors;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.preferences.SharedPreferencesManager;
import io.github.doubi88.slideshowwallpaper.utilities.CompatibilityHelpers;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ImageLoader;


public class SlideshowWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new SlideshowWallpaperEngine();
    }


    public class SlideshowWallpaperEngine extends Engine {

        private Handler handler;

        private int width;
        private int height;

        private int currentImageWidth;
        private int currentImageHeight;

        private Runnable drawRunner;
        private Paint clearPaint;
        private Paint imagePaint;
        private Paint textPaint;
        private boolean visible;
        private int textSize;

        private int currentIndex;
        private int listLength;

        private ImageInfo lastRenderedImage;

        private float deltaX;
        private float lastXOffset;
        private float lastXOffsetStep;
        private boolean isScrolling = false;

        private SharedPreferencesManager manager;

        public SlideshowWallpaperEngine() {
            SharedPreferences prefs = getSharedPreferences();
            manager = new SharedPreferencesManager(prefs);

            deltaX = 0;
            handler = new Handler(Looper.getMainLooper());
            drawRunner = new DrawRunner();

            clearPaint = new Paint();
            clearPaint.setColor(Color.BLACK);
            clearPaint.setStyle(Paint.Style.FILL);

            imagePaint = new Paint();
            if (manager.getAntiAlias()) {
                imagePaint.setAntiAlias(true);
            }

            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.WHITE);
            textPaint.setStyle(Paint.Style.FILL);

            float scale = getResources().getDisplayMetrics().density;
            textSize = (int) (10f * scale + 0.5f);
            textPaint.setTextSize(textSize);
            handler.post(drawRunner);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);
            try {
                lastXOffset = xOffset;
                lastXOffsetStep = xOffsetStep;
                Bitmap image = getNextImage();
                if (image != null) {
                    deltaX = calculateDeltaX(image, lastXOffset, lastXOffsetStep);
                } else {
                    deltaX = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
                deltaX = 0;
            }

            // When the xOffset is not a whole number, the wallpaper is scrolling
            isScrolling = (Math.floor(xOffset) != xOffset);
            handler.removeCallbacks(drawRunner);
            handler.post(drawRunner);
        }

        private float calculateDeltaX(Bitmap image, float xOffset, float xOffsetStep) {
            int width = image.getWidth();

            float result = 0;
            SharedPreferencesManager.TooWideImagesRule rule = manager.getTooWideImagesRule(getResources());
            float scale = ImageLoader.calculateScaleFactorToFit(image, this.width, this.height, rule == SharedPreferencesManager.TooWideImagesRule.SCALE_DOWN);
            width = Math.round(width * scale);
            if (width > this.width) {
                if (rule == SharedPreferencesManager.TooWideImagesRule.SCALE_UP) {
                    xOffset = 0.5f;
                } else if (rule == SharedPreferencesManager.TooWideImagesRule.SCROLL_BACKWARD) {
                    xOffset = 1 - xOffset;
                }
                result = -xOffset * (width - this.width);
            }
            return result;
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.width = width;
            this.height = height;
            handler.post(drawRunner);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            handler.removeCallbacks(drawRunner);
            visible = false;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            this.visible = visible;
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        @Override
        public WallpaperColors onComputeColors () {
            try {
                Bitmap img = this.getNextImage();
                if (img != null) {
                    return WallpaperColors.fromBitmap(img);
                } else {
                    return super.onComputeColors();
                }
            } catch (IOException e) {
                return super.onComputeColors();
            }
        }

        private SharedPreferences getSharedPreferences() {
            return SlideshowWallpaperService.this.getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        }


        private Bitmap getNextImage() throws IOException {
            Uri uri = getNextUri();
            if (uri != null) {
                if (lastRenderedImage == null || lastRenderedImage.getImage() == null || !uri.equals(lastRenderedImage.getUri())) {
                    lastRenderedImage = ImageLoader.loadImage(uri, SlideshowWallpaperService.this, width, height, false);
                    Bitmap image = lastRenderedImage.getImage();
                    if (image != null) {
                        deltaX = calculateDeltaX(image, lastXOffset, lastXOffsetStep);
                    }
                    return image;
                } else {
                    return lastRenderedImage.getImage();
                }
            } else {
                return null;
            }
        }

        private Uri getNextUri() {
            Uri result = null;
            SharedPreferencesManager.Ordering ordering = manager.getCurrentOrdering(getResources());
            int countUris = manager.getImageUrisCount();

            if (countUris > 0) {
                int currentImageIndex = manager.getCurrentIndex();
                if (currentImageIndex >= countUris) {
                    // If an image was deleted and therefore we are over the end of the list
                    currentImageIndex -= countUris;
                }
                int nextUpdate = calculateNextUpdateInSeconds();
                if (nextUpdate <= 0) {
                    int delay = getDelaySeconds();
                    while (nextUpdate <= 0) {
                        currentImageIndex++;

                        if (currentImageIndex >= countUris) {
                            currentImageIndex = 0;
                        }

                        nextUpdate += delay;
                    }
                    manager.setCurrentIndex(currentImageIndex);
                    manager.setLastUpdate(System.currentTimeMillis());
                }
                result = manager.getImageUri(currentImageIndex, ordering);
                currentIndex = currentImageIndex;
                listLength = countUris;
            }

            return result;
        }

        private int getDelaySeconds() {
            int seconds = 5;
            try {
                seconds = manager.getSecondsBetweenImages();
                String[] entries = getResources().getStringArray(R.array.seconds_values);
                seconds = CompatibilityHelpers.getNextAvailableSecondsEntry(seconds, entries); // Because of the update of the seconds entries (Issue #14), we have to find the nearest entry here.
            } catch (NumberFormatException e) {
                Log.e(SlideshowWallpaperEngine.class.getSimpleName(), "Invalid number", e);
                Toast toast = Toast.makeText(getApplicationContext(), e.getClass().getSimpleName() + " " + e.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            }
            return seconds;
        }

        private int calculateNextUpdateInSeconds() {
            long lastUpdate = manager.getLastUpdate();
            int result = 0;
            if (lastUpdate > 0) {
                int delaySeconds = getDelaySeconds();
                long current = System.currentTimeMillis();
                result = delaySeconds - (int)((current - lastUpdate) / 1000); // Difference between delay and elapsed time since last update in seconds
            }
            return result;
        }

        private class DrawRunner implements Runnable {
            @Override
            public void run() {
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawRect(0, 0, width, height, clearPaint);

                        Uri lastUri = lastRenderedImage != null ? lastRenderedImage.getUri() : null;
                        Bitmap bitmap = getNextImage();
                        if (bitmap != null) {
                            currentImageHeight = bitmap.getHeight();
                            currentImageWidth = bitmap.getWidth();

                            SharedPreferencesManager.TooWideImagesRule rule = manager.getTooWideImagesRule(getResources());
                            boolean antiAlias = manager.getAntiAlias();
                            boolean antiAliasScrolling = manager.getAntiAliasWhileScrolling();
                            imagePaint.setAntiAlias(antiAlias && (!isScrolling || antiAliasScrolling));
                            if (rule == SharedPreferencesManager.TooWideImagesRule.SCALE_DOWN) {
                                canvas.drawBitmap(bitmap, ImageLoader.calculateMatrixScaleToFit(bitmap, width, height, true), imagePaint);
                            } else if (rule == SharedPreferencesManager.TooWideImagesRule.SCALE_UP || rule == SharedPreferencesManager.TooWideImagesRule.SCROLL_FORWARD || rule == SharedPreferencesManager.TooWideImagesRule.SCROLL_BACKWARD) {
                                canvas.save();
                                canvas.translate(deltaX, 0);
                                canvas.drawBitmap(bitmap, ImageLoader.calculateMatrixScaleToFit(bitmap, width, height, false), imagePaint);
                                canvas.restore();
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && (lastUri == null || (!lastUri.equals(lastRenderedImage.getUri())))) {
                                // Only notify, if the image changes.
                                SlideshowWallpaperEngine.this.notifyColorsChanged();
                            }

                            if (BuildConfig.DEBUG) {
                                String drawText = (currentIndex + 1) + "/" + listLength;
                                canvas.drawText(drawText, 0, drawText.length(), textSize + 10, textSize + 10, textPaint);
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(SlideshowWallpaperService.class.getSimpleName(), getResources().getString(R.string.error_reading_file), e);
                } finally {
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas);
                        } catch (IllegalArgumentException e) {
                            Log.e(SlideshowWallpaperService.class.getSimpleName(), "Error unlocking canvas", e);
                        }
                    }
                }
                handler.removeCallbacks(drawRunner);
                if (visible) {

                    handler.postDelayed(drawRunner, calculateNextUpdateInSeconds() * 1000);
                }
            }
        }

    }
}
