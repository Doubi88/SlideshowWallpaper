package de.tobi.slideshowwallpaper;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tobi.slideshowwallpaper.preferences.SharedPreferencesManager;
import de.tobi.slideshowwallpaper.utilities.ImageInfo;
import de.tobi.slideshowwallpaper.utilities.ImageLoader;


public class SlideshowWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new SlideshowWallpaperEngine();
    }


    public class SlideshowWallpaperEngine extends Engine {

        private Handler handler;

        private int width;
        private int height;

        private Runnable drawRunner;
        private Paint clearPaint;
        private Paint textPaint;
        private boolean visible;
        private int textSize;

        private int currentIndex;
        private int listLength;

        private SharedPreferencesManager manager;

        public SlideshowWallpaperEngine() {
            handler = new Handler(Looper.getMainLooper());
            drawRunner = new DrawRunner();

            clearPaint = new Paint();
            clearPaint.setAntiAlias(true);
            clearPaint.setColor(Color.BLACK);
            clearPaint.setStyle(Paint.Style.FILL);

            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.WHITE);
            textPaint.setStyle(Paint.Style.FILL);

            float scale = getResources().getDisplayMetrics().density;
            textSize = (int) (10f * scale + 0.5f);
            textPaint.setTextSize(textSize);
            handler.post(drawRunner);

            manager = new SharedPreferencesManager(getSharedPreferences());
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.width = width;
            this.height = height;
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

        private SharedPreferences getSharedPreferences() {
            return SlideshowWallpaperService.this.getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        }

        private class DrawRunner implements Runnable {
            @Override
            public void run() {
                //Debug.waitForDebugger();
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawRect(0, 0, width, height, clearPaint);

                        Bitmap bitmap = getNextImage();
                        if (bitmap != null) {
                            canvas.drawBitmap(bitmap, ImageLoader.calculateMatrixScaleToFit(bitmap, width, height), null);
                            String drawText = (currentIndex + 1) + "/" + listLength;
                            canvas.drawText(drawText, 0, drawText.length(), textSize + 10, textSize + 10, textPaint);
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

            private Bitmap getNextImage() throws IOException {
                Uri uri = getNextUri();
                if (uri != null) {
                    ImageInfo info = ImageLoader.loadImage(uri, SlideshowWallpaperService.this, width, height, false);
                    return info.getImage();
                } else {
                    return null;
                }
            }

            private Uri getNextUri() {
                Uri result = null;
                SharedPreferencesManager.Ordering ordering = manager.getCurrentOrdering(getResources());
                List<Uri> uris = manager.getImageUris(ordering);

                if (uris.size() > 0) {
                    int currentImageIndex = manager.getCurrentIndex();
                    int nextUpdate = calculateNextUpdateInSeconds();
                    if (nextUpdate <= 0) {
                        int delay = getDelaySeconds();
                        while (nextUpdate <= 0) {
                            currentImageIndex++;

                            if (currentImageIndex >= uris.size()) {
                                currentImageIndex = 0;
                            }

                            nextUpdate += delay;
                        }
                        manager.setCurrentIndex(currentImageIndex);
                        manager.setLastUpdate(System.currentTimeMillis());
                    }
                    result = uris.get(currentImageIndex);
                    currentIndex = currentImageIndex;
                    listLength = uris.size();
                }

                return result;
            }

            private int getDelaySeconds() {
                int seconds = 5;
                try {
                    seconds = manager.getSecondsBetweenImages();
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

        }
    }
}
