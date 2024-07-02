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

import androidx.annotation.RequiresApi;

import io.github.doubi88.slideshowwallpaper.preferences.SharedPreferencesManager;
import io.github.doubi88.slideshowwallpaper.utilities.CurrentImageHandler;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ImageLoader;


public class SlideshowWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new SlideshowWallpaperEngine();
    }


    public class SlideshowWallpaperEngine extends Engine {

        private Handler handler;

        private CurrentImageHandler currentImageHandler;

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
            float deltaXResult = 0;
            if (currentImageHandler != null) {
                ImageInfo currentImage = currentImageHandler.getCurrentImage();
                if (currentImage != null) {
                    lastXOffset = xOffset;
                    lastXOffsetStep = xOffsetStep;
                    Bitmap image = currentImageHandler.getCurrentImage().getImage();
                    if (image != null) {
                        deltaXResult = calculateDeltaX(image, lastXOffset, lastXOffsetStep);
                    } else {
                        deltaXResult = 0;
                    }
                }
            }
            deltaX = deltaXResult;
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
            if (currentImageHandler == null) {
                currentImageHandler = new CurrentImageHandler(manager, width, height);
                currentImageHandler.addNextImageListener((i) -> {
                    handler.removeCallbacks(drawRunner);
                    handler.post(drawRunner);
                });

                // First load the current image directly, then start the timer
                currentImageHandler.updateAfter(getApplicationContext(), 0);
                currentImageHandler.startTimer(getApplicationContext());
            } else {
                currentImageHandler.setDimensions(width, height, getApplicationContext());
            }
            handler.post(drawRunner);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            currentImageHandler.stop();
            handler.removeCallbacks(drawRunner);
            visible = false;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            this.visible = visible;
            if (visible) {
                handler.post(drawRunner);
                if (!currentImageHandler.isStarted()) {

                    // First load the current image directly, then start the timer
                    currentImageHandler.updateAfter(getApplicationContext(), 0);
                    currentImageHandler.startTimer(getApplicationContext());
                }
            } else {
                handler.removeCallbacks(drawRunner);
                currentImageHandler.stop();
            }
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        @Override
        public WallpaperColors onComputeColors () {
            WallpaperColors result = null;
            if (currentImageHandler != null && currentImageHandler.getCurrentImage() != null) {
                Bitmap img = currentImageHandler.getCurrentImage().getImage();
                if (img != null) {
                    result = WallpaperColors.fromBitmap(img);
                }
            }
            if (result == null) {
                result = super.onComputeColors();
            }
            return result;
        }

        private SharedPreferences getSharedPreferences() {
            return SlideshowWallpaperService.this.getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
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
                        ImageInfo image = currentImageHandler.getCurrentImage();
                        Bitmap bitmap = null;
                        if (image != null) {
                            bitmap = image.getImage();
                        }
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
                                lastRenderedImage = image;
                            }
                            /*
                            if (BuildConfig.DEBUG) {
                                int listLength = manager.getImageUrisCount();
                                String drawText = (currentIndex + 1) + "/" + listLength;
                                canvas.drawText(drawText, 0, drawText.length(), textSize + 10, textSize + 10, textPaint);
                            }
                            */
                        }
                    }
                } finally {
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas);
                        } catch (IllegalArgumentException e) {
                            Log.e(SlideshowWallpaperService.class.getSimpleName(), "Error unlocking canvas", e);
                        }
                    }
                }
            }
        }

    }
}
