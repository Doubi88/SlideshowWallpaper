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
package de.tobi.slideshowwallpaper.utilities;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.tobi.slideshowwallpaper.R;

public class AsyncTaskLoadImages extends AsyncTask<Uri, BigDecimal, List<ImageInfo>> {

    private LinkedList<ProgressListener<Uri, BigDecimal, List<ImageInfo>>> listeners;
    private Context context;
    private int desiredWidth;
    private int desiredHeight;

    public AsyncTaskLoadImages(Context context, int desiredWidth, int desiredHeight) {
        listeners = new LinkedList<>();
        this.context = context;
        this.desiredWidth = desiredWidth;
        this.desiredHeight = desiredHeight;
    }

    public void addProgressListener(ProgressListener<Uri, BigDecimal, List<ImageInfo>> listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeProgressListener(ProgressListener<Uri, BigDecimal, List<ImageInfo>> listener) {
        listeners.remove(listener);
    }

    private void notifyProgressListeners(BigDecimal current, BigDecimal max) {
        for (ProgressListener<Uri, BigDecimal, List<ImageInfo>> listener : listeners) {
            listener.onProgressChanged(this, current, max);
        }
    }

    private void notifyFinishedListeners(List<ImageInfo> result) {
        for (ProgressListener<Uri, BigDecimal, List<ImageInfo>> listener : listeners) {
            listener.onTaskFinished(this, result);
        }
    }

    private void notifyCancelledListeners(List<ImageInfo> result) {
        for (ProgressListener<Uri, BigDecimal, List<ImageInfo>> listener :  listeners) {
            listener.onTaskCancelled(this, result);
        }
    }

    @Override
    protected List<ImageInfo> doInBackground(Uri... uris) {
        List<ImageInfo> bitmaps = new ArrayList<>(uris.length);
        if (!isCancelled()) {
            BigDecimal listSize = BigDecimal.valueOf(uris.length);
            for (Uri uri : uris) {
                if (isCancelled()) {
                    break;
                }
                ImageInfo info = loadBitmap(uri);
                bitmaps.add(info);
                publishProgress(BigDecimal.valueOf(bitmaps.size()).divide(listSize, 2, RoundingMode.HALF_UP), BigDecimal.valueOf(bitmaps.size()));
            }
        }
        return bitmaps;
    }

    private ImageInfo loadBitmap(Uri uri) {
        ImageInfo info = null;

        try {
            info = ImageLoader.loadImage(uri, context, desiredWidth, desiredHeight, true);
        } catch (IOException e) {
            Log.e(AsyncTaskLoadImages.class.getSimpleName(), "Error opening file", e);
            info = new ImageInfo(uri, context.getResources().getString(R.string.error_reading_file) + ": " + e.getClass().getSimpleName() + " " + e.getMessage(), 0, null);
        }


        return info;
    }

    @Override
    protected void onCancelled(List<ImageInfo> imageInfos) {
        notifyCancelledListeners(imageInfos);
    }

    @Override
    protected void onProgressUpdate(BigDecimal... values) {
        notifyProgressListeners(values[0], values[1]);
    }

    @Override
    protected void onPostExecute(List<ImageInfo> imageInfos) {
        notifyFinishedListeners(imageInfos);
    }
}
