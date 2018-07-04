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
import java.util.Set;

import de.tobi.slideshowwallpaper.utilities.ImageInfo;
import de.tobi.slideshowwallpaper.utilities.ImageLoader;


public class SlideshowWallpaperService extends WallpaperService {
    public static final String PREFERENCE_KEY_ALPHABETICAL_LIST = "alphabetical_list";
    public static final String PREFERENCE_KEY_RANDOM_LIST = "random_list";
    private static final String PREFERENCE_KEY_LAST_UPDATE = "last_update";
    private static final String PREFERENCE_KEY_LAST_INDEX = "last_index";

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
        private boolean visible;

        public SlideshowWallpaperEngine() {
            handler = new Handler(Looper.getMainLooper());
            drawRunner = new DrawRunner();

            clearPaint = new Paint();
            clearPaint.setAntiAlias(true);
            clearPaint.setColor(Color.WHITE);
            clearPaint.setStyle(Paint.Style.FILL);
            handler.post(drawRunner);
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
                String uri = getNextUri();
                if (uri != null) {
                    ImageInfo info = ImageLoader.loadImage(Uri.parse(uri), SlideshowWallpaperService.this, width, height);
                    return info.getImage();
                } else {
                    return null;
                }
            }

            private String getNextUri() {
                String result = null;
                String ordering = getSharedPreferences().getString(getResources().getString(R.string.preference_ordering_key), "selection");
                String[] uris = null;
                if (ordering.equals("random") && getSharedPreferences().contains(PREFERENCE_KEY_RANDOM_LIST)) {
                    Set<String> urisSet = getSharedPreferences().getStringSet(PREFERENCE_KEY_RANDOM_LIST, new HashSet<String>());
                    uris = urisSet.toArray(new String[urisSet.size()]);
                }
                if (uris == null || uris.length == 0) {
                    Set<String> urisSet = getSharedPreferences().getStringSet(getResources().getString(R.string.preference_pick_folder_key), new HashSet<String>());
                    uris = urisSet.toArray(new String[urisSet.size()]);
                }
                if (uris.length > 0) {
                    int currentImageIndex = getSharedPreferences().getInt(PREFERENCE_KEY_LAST_INDEX, 0);
                    int nextUpdate = calculateNextUpdateInSeconds();
                    if (nextUpdate <= 0) {
                        int delay = getDelaySeconds();
                        while (nextUpdate <= 0) {
                            currentImageIndex++;

                            if (currentImageIndex >= uris.length) {
                                currentImageIndex = 0;
                            }

                            nextUpdate += delay;
                        }
                        SharedPreferences.Editor editor = getSharedPreferences().edit();
                        editor.putLong(PREFERENCE_KEY_LAST_UPDATE, System.currentTimeMillis());
                        editor.putInt(PREFERENCE_KEY_LAST_INDEX, currentImageIndex);
                        editor.apply();
                    }
                    result = uris[currentImageIndex];
                }

                return result;
            }

            private int getDelaySeconds() {
                int seconds = 5;
                try {
                    String secondsString = getSharedPreferences().getString(getResources().getString(R.string.preference_seconds_key), "5");
                    seconds = Integer.parseInt(secondsString);
                } catch (NumberFormatException e) {
                    Log.e(SlideshowWallpaperEngine.class.getSimpleName(), "Invalid number", e);
                    Toast toast = Toast.makeText(getApplicationContext(), e.getClass().getSimpleName() + " " + e.getMessage(), Toast.LENGTH_LONG);
                    toast.show();
                }
                return seconds;
            }

            private int calculateNextUpdateInSeconds() {
                long lastUpdate = getSharedPreferences().getLong(PREFERENCE_KEY_LAST_UPDATE, 0);
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
