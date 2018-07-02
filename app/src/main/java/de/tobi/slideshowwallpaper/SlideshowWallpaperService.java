package de.tobi.slideshowwallpaper;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

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
        private  int height;

        private Runnable drawRunner;
        private Paint paint;
        private Paint clearPaint;
        private boolean visible;
        private ArrayList<String> uris;
        private int currentImageIndex;

        public SlideshowWallpaperEngine() {
            handler = new Handler(Looper.getMainLooper());
            drawRunner = new DrawRunner();
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(10f);

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


        private class DrawRunner implements Runnable {
            @Override
            public void run() {
                SurfaceHolder holder = getSurfaceHolder();
                Canvas canvas = null;
                SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        canvas.drawRect(0, 0, width, height, clearPaint);

                        uris = new ArrayList<>(preferences.getStringSet(getResources().getString(R.string.preference_pick_folder_key), new HashSet<String>()));
                        Bitmap bitmap = getNextImage();
                        Matrix matrix = calculateDrawMatrix(bitmap, width, height);
                        if (bitmap != null) {
                            canvas.drawBitmap(bitmap, matrix, null);
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
                    handler.postDelayed(drawRunner, Integer.parseInt(preferences.getString(getResources().getString(R.string.preference_seconds_key), "5")) * 1000);
                }
            }

            private Matrix calculateDrawMatrix(Bitmap bitmap, int screenWidth, int screenHeight) {
                int originalWidth = bitmap.getWidth();
                int originalHeight = bitmap.getHeight();

                Matrix result = new Matrix();

                float scale = (float)screenWidth / (float)originalWidth;
                float yTranslation = (screenHeight - originalHeight * scale) / 2f;

                result.postScale(scale, scale);

                result.postTranslate(0, yTranslation);
                return result;
            }
            private Bitmap getNextImage() throws IOException {
                String uri = getNextUri();
                if (uri != null) {
                    ImageInfo info = ImageLoader.loadImage(uri, SlideshowWallpaperService.this, width, height);
                    return info.getImage();
                } else {
                    return null;
                }
            }

            private String getNextUri() {
                String result = null;
                if (!uris.isEmpty()) {
                    currentImageIndex++;
                    if (currentImageIndex >= uris.size()) {
                        currentImageIndex = 0;
                    }
                    result = uris.get(currentImageIndex);
                }

                return result;
            }
        }
    }
}
