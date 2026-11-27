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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;


import io.github.doubi88.slideshowwallpaper.preferences.SharedPreferencesManager;
import io.github.doubi88.slideshowwallpaper.utilities.CurrentImageHandler;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ImageLoader;


public class SlideshowWallpaperService extends WallpaperService {

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_USER_PRESENT.equals(intent.getAction())) {
                return;
            }
            SharedPreferencesManager manager = new SharedPreferencesManager(
                    getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE));
            int count = manager.getImageUrisCount();
            if (manager.changesOnUnlock() && count > 1) {
                // Update the shared selection once, even if both wallpaper engines are attached.
                manager.setCurrentIndex((manager.getCurrentIndex() + 1) % count);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ContextCompat.registerReceiver(this, unlockReceiver,
                new IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(unlockReceiver);
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new SlideshowWallpaperEngine();
    }


    public class SlideshowWallpaperEngine extends Engine {

        private Handler handler;

        private CurrentImageHandler currentImageHandler;
        private boolean visible;
        private boolean surfaceReady;
        private SharedPreferencesManager manager;
        private final SharedPreferences.OnSharedPreferenceChangeListener settingsListener =
                (preferences, key) -> {
                    if (key == null || "change_on_unlock".equals(key)
                            || "pick_images".equals(key) || "seconds".equals(key)
                            || "ordering".equals(key)
                            || ("last_index".equals(key) && manager.changesOnUnlock())) {
                        if ("change_on_unlock".equals(key)) {
                            manager.setLastUpdate(System.currentTimeMillis());
                        }
                        refresh();
                    }
                };

        private int width;
        private int height;

        private Runnable drawRunner;
        private Paint clearPaint;
        private Paint imagePaint;
        private Paint textPaint;
        private int textSize;

        private ImageInfo lastRenderedImage;

        private boolean isScrolling = false;
        private float xOffset = 0f;
        private float xOffsetStep = 0f;


        public SlideshowWallpaperEngine() {
            SharedPreferences prefs = getSharedPreferences();
            manager = new SharedPreferencesManager(prefs);

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
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            getSharedPreferences().registerOnSharedPreferenceChangeListener(settingsListener);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset);

            this.xOffset = xOffset;
            this.xOffsetStep = xOffsetStep;
            // When the xOffset is not a whole number, the wallpaper is scrolling
            isScrolling = (Math.floor(xOffset) != xOffset);

            SharedPreferencesManager.TooWideImagesRule rule = manager.getTooWideImagesRule(getResources());
            // We only need to retrigger a redraw on scroll if we have one of the scrolling options enabled
            if (rule == SharedPreferencesManager.TooWideImagesRule.SCROLL_FORWARD || rule == SharedPreferencesManager.TooWideImagesRule.SCROLL_BACKWARD) {
                handler.removeCallbacks(drawRunner);
                handler.post(drawRunner);
            }
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
            surfaceReady = width > 0 && height > 0;
            if (currentImageHandler == null) {
                currentImageHandler = new CurrentImageHandler(manager, width, height);
                currentImageHandler.addNextImageListener(image -> {
                    handler.removeCallbacks(drawRunner);
                    handler.post(drawRunner);
                });
            } else {
                currentImageHandler.setDimensions(width, height);
            }
            refresh();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
            refresh();
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            this.visible = visible;
            refresh();
        }

        private void refresh() {
            handler.removeCallbacks(drawRunner);
            if (currentImageHandler == null) {
                return;
            }
            if (visible && surfaceReady) {
                currentImageHandler.start(getApplicationContext());
                handler.post(drawRunner);
            } else {
                currentImageHandler.stop();
            }
        }

        @Override
        public void onDestroy() {
            visible = false;
            getSharedPreferences().unregisterOnSharedPreferenceChangeListener(settingsListener);
            handler.removeCallbacks(drawRunner);
            if (currentImageHandler != null) {
                currentImageHandler.destroy();
            }
            super.onDestroy();
        }

        @RequiresApi(Build.VERSION_CODES.O_MR1)
        @Override
        public WallpaperColors onComputeColors () {
            ImageInfo image = currentImageHandler == null ? null : currentImageHandler.getCurrentImage();
            Bitmap bitmap = image == null ? null : image.getImage();
            return bitmap == null ? super.onComputeColors() : WallpaperColors.fromBitmap(bitmap);
        }

        private SharedPreferences getSharedPreferences() {
            return SlideshowWallpaperService.this.getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        }

        private class DrawRunner implements Runnable {
            @Override
            public void run() {
                if (!visible || !surfaceReady || currentImageHandler == null) {
                    return;
                }
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawRect(0, 0, width, height, clearPaint);

                        ImageInfo image = currentImageHandler.getCurrentImage();
                        Bitmap bitmap = null;
                        if (image != null) {
                            bitmap = image.getImage();
                        }
                        if (bitmap != null) {
                            SharedPreferencesManager.TooWideImagesRule rule = manager.getTooWideImagesRule(getResources());
                            float deltaX = calculateDeltaX(bitmap, xOffset, xOffsetStep);
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

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && (image != null && (lastRenderedImage == null || !image.getUri().equals(lastRenderedImage.getUri())))) {
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
                } catch (Exception e) {
                    Log.e(getClass().getName(), "Error when drawing", e);
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
