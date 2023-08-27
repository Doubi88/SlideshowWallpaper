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
package io.github.doubi88.slideshowwallpaper.preferences.imageList;

import android.animation.Animator;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;
import io.github.doubi88.slideshowwallpaper.listeners.OnDeleteClickListener;
import io.github.doubi88.slideshowwallpaper.utilities.AsyncTaskLoadImages;
import io.github.doubi88.slideshowwallpaper.utilities.ImageInfo;
import io.github.doubi88.slideshowwallpaper.utilities.ImageLoader;
import io.github.doubi88.slideshowwallpaper.utilities.ProgressListener;

public class ImageInfoViewHolder extends RecyclerView.ViewHolder implements ProgressListener<Uri, BigDecimal, List<ImageInfo>> {

    private final int height;
    private final int width;
    private ImageInfo imageInfo;

    private boolean bottomBarDisplaying;

    private ImageView imageView;
    private TextView textView;
    private LinearLayout bottomBar;
    private ProgressBar progressBar;
    private AsyncTaskLoadImages asyncTask;

    private LinkedList<OnDeleteClickListener> listeners;

    public ImageInfoViewHolder(View itemView) {
        super(itemView);
        listeners = new LinkedList<>();
        imageView = itemView.findViewById(R.id.image_view);
        textView = itemView.findViewById(R.id.card_text);

        bottomBar = itemView.findViewById(R.id.bottom_bar);
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleBottomBar();
            }
        });
        progressBar = itemView.findViewById(R.id.progress_bar);
        Button deleteButton = itemView.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyListeners();
            }
        });

        height = imageView.getResources().getDimensionPixelSize(R.dimen.image_preview_height);
        width = itemView.getWidth();
    }

    public void setUri(Uri uri) {
        if (imageInfo == null || !uri.equals(imageInfo.getUri())) {
            showBottomBar();

            imageInfo = ImageLoader.loadFileNameAndSize(uri, imageView.getContext());
            textView.setText(imageInfo.getName());

      }
    }

    private void toggleBottomBar() {
        if (bottomBarDisplaying) {
            hideBottomBar();
        } else {
            showBottomBar();
        }
    }
    private void showBottomBar() {
        Log.i(ImageInfoViewHolder.class.getSimpleName(), "Show text view");
        bottomBar.setVisibility(View.VISIBLE);
        ViewPropertyAnimator anim = bottomBar.animate();
        anim.translationY(0);
        anim.setDuration(500);
        anim.start();
        bottomBarDisplaying = true;
    }

    private void hideBottomBar() {
        Log.i(ImageInfoViewHolder.class.getSimpleName(), "Hide text view");
        ViewPropertyAnimator anim = bottomBar.animate();
        anim.translationY(bottomBar.getHeight());
        anim.setDuration(500);
        anim.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                Log.i(ImageInfoViewHolder.class.getSimpleName(), "Animation end: gone");
                bottomBar.setVisibility(View.GONE);
                bottomBar.clearAnimation();
                bottomBar.animate().setListener(null);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                Log.i(ImageInfoViewHolder.class.getSimpleName(), "Animation cancel: gone");
                bottomBar.setTranslationY(bottomBar.getHeight());
                bottomBar.setVisibility(View.GONE);
                bottomBar.clearAnimation();
                bottomBar.animate().setListener(null);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

        anim.start();
        bottomBarDisplaying = false;
    }
    public void setOnDeleteButtonClickListener(OnDeleteClickListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (OnDeleteClickListener listener : listeners) {
            listener.onDeleteButtonClicked(imageInfo);
        }
    }

    public Uri getUri() {
        return imageInfo.getUri();
    }

    @Override
    public void onProgressChanged(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, BigDecimal current, BigDecimal max) {
        progressBar.setMax(max.intValue());
        progressBar.setProgress(current.intValue());
    }

    @Override
    public void onTaskFinished(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
        if (imageInfos.size() == 1) {
            imageInfo = imageInfos.get(0);
            if (imageInfo.getImage() != null) {
                Matrix matrix = ImageLoader.calculateMatrixScaleToFit(imageInfo.getImage(), width, height, false);
                imageView.setImageBitmap(Bitmap.createBitmap(imageInfo.getImage(), 0, 0, imageInfo.getImage().getWidth(), imageInfo.getImage().getHeight(), matrix, false));
            }
            textView.setText(imageInfo.getName());
            progressBar.setVisibility(View.GONE);

        }
    }

    @Override
    public void onTaskCancelled(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
        if (imageInfos != null) {
            for (ImageInfo info : imageInfos) {
                info.getImage().recycle();
            }
        }
    }
}
