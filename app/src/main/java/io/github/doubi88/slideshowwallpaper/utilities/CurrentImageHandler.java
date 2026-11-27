package io.github.doubi88.slideshowwallpaper.utilities;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.preferences.SharedPreferencesManager;

public class CurrentImageHandler {

    private volatile ImageInfo currentImage;

    private SharedPreferencesManager manager;
    private int width;
    private int height;

    private final ScheduledThreadPoolExecutor worker = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> update;
    private long generation;

    private ArrayList<NextImageListener> nextImageListeners;

    public CurrentImageHandler(SharedPreferencesManager manager, int width, int height) {
        this.manager = manager;
        this.width = width;
        this.height = height;
        worker.setRemoveOnCancelPolicy(true);
        nextImageListeners = new ArrayList<>(1);
    }

    public void addNextImageListener(NextImageListener l) {
        this.nextImageListeners.add(l);
    }
    public void removeNextImageListener(NextImageListener l) {
        this.nextImageListeners.remove(l);
    }

    private void notifyNextImageListeners(ImageInfo i) {
        for (NextImageListener l : nextImageListeners) {
            l.nextImage(i);
        }
    }

    public ImageInfo getCurrentImage() {
        return currentImage;
    }

    public synchronized void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public synchronized void start(Context context) {
        stop();
        schedule(context, generation, 0);
    }

    private void schedule(Context context, long request, long seconds) {
        update = worker.schedule(() -> update(context, request), seconds, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        generation++;
        if (update != null) {
            update.cancel(false);
            update = null;
        }
    }

    public synchronized void destroy() {
        stop();
        worker.shutdown();
    }

    private void update(Context context, long request) {
        Uri uri;
        int targetWidth;
        int targetHeight;
        synchronized (this) {
            if (request != generation) {
                return;
            }
            targetWidth = width;
            targetHeight = height;
            uri = getNextUri(context);
        }
        ImageInfo next = null;
        try {
            if (uri != null && (currentImage == null || currentImage.getImage() == null
                    || !uri.equals(currentImage.getUri()))) {
                next = ImageLoader.loadImage(uri, context, targetWidth, targetHeight, false);
            }
        } catch (IOException | RuntimeException e) {
            Log.w(CurrentImageHandler.class.getSimpleName(), "Error loading image", e);
        }
        synchronized (this) {
            if (request != generation) {
                return;
            }
            if (next != null || (uri == null && currentImage != null)) {
                currentImage = next;
                notifyNextImageListeners(currentImage);
            }
            if (uri != null && !manager.changesOnUnlock()) {
                schedule(context, request, Math.max(1, calculateNextUpdateInSeconds(context)));
            }
        }
    }

    private Uri getNextUri(Context context) {
        SharedPreferencesManager.Ordering ordering = manager.getCurrentOrdering(context.getResources());
        if (ordering == null) {
            ordering = SharedPreferencesManager.Ordering.SELECTION;
        }
        List<Uri> images = manager.getImageUris(ordering);
        if (images.isEmpty()) {
            return null;
        }
        int index = manager.getCurrentIndex() % images.size();
        if (!manager.changesOnUnlock()) {
            long remaining = calculateNextUpdateInSeconds(context);
            if (remaining <= 0) {
                long steps = 1 - remaining / getDelaySeconds(context);
                index = (int) ((index + steps) % images.size());
                manager.setCurrentIndex(index);
                manager.setLastUpdate(System.currentTimeMillis());
            }
        }
        return images.get(index);
    }

    private long calculateNextUpdateInSeconds(Context context) {
        long lastUpdate = manager.getLastUpdate();
        long result = 0;
        if (lastUpdate > 0) {
            int delaySeconds = getDelaySeconds(context);
            long current = System.currentTimeMillis();
            result = delaySeconds - ((current - lastUpdate) / 1000); // Difference between delay and elapsed time since last update in seconds
        }
        return result;
    }

    private int getDelaySeconds(Context context) {
        Resources resources = context.getResources();
        int seconds = 5;
        try {
            seconds = manager.getSecondsBetweenImages();
            String[] entries = resources.getStringArray(R.array.seconds_values);
            seconds = CompatibilityHelpers.getNextAvailableSecondsEntry(seconds, entries); // Because of the update of the seconds entries (Issue #14), we have to find the nearest entry here.
        } catch (NumberFormatException e) {
            Log.e(CurrentImageHandler.class.getSimpleName(), "Invalid number", e);
        }
        return seconds;
    }

}
