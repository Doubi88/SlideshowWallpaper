package de.tobi.slideshowwallpaper.preferences.imageList;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Debug;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import de.tobi.slideshowwallpaper.R;
import de.tobi.slideshowwallpaper.listeners.OnDeleteClickListener;
import de.tobi.slideshowwallpaper.utilities.AsyncTaskLoadImages;
import de.tobi.slideshowwallpaper.utilities.ImageInfo;
import de.tobi.slideshowwallpaper.utilities.ImageLoader;
import de.tobi.slideshowwallpaper.utilities.ProgressListener;

public class ImageInfoViewHolder extends RecyclerView.ViewHolder {

    private final int height;
    private final int width;
    private ImageInfo imageInfo;

    private ImageView imageView;
    private TextView textView;
    private ImageView deleteButton;
    private ProgressBar progressBar;

    private LinkedList<OnDeleteClickListener> listeners;

    public ImageInfoViewHolder(View itemView) {
        super(itemView);
        listeners = new LinkedList<>();
        imageView = itemView.findViewById(R.id.image_view);
        textView = itemView.findViewById(R.id.card_text);
        progressBar = itemView.findViewById(R.id.progress_bar);
        deleteButton = itemView.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyListeners();
            }
        });

        float scale = imageView.getResources().getDisplayMetrics().density;
        height = (int) (200f * scale + 0.5f);
        width = 1;
    }

    public void setImageInfo(Uri uri) {
        if (imageInfo == null || !uri.equals(imageInfo.getUri())) {
            if (imageInfo != null) {
                imageView.setImageBitmap(null);
                imageInfo.getImage().recycle();
                imageInfo = null;

                textView.setText("");
                progressBar.setVisibility(View.VISIBLE);
            }

            AsyncTaskLoadImages asyncTask = new AsyncTaskLoadImages(imageView.getContext(), width, height);
            asyncTask.addProgressListener(new ProgressListener<Uri, BigDecimal, List<ImageInfo>>() {
                @Override
                public void onProgressChanged(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, BigDecimal current, BigDecimal max) {

                }

                @Override
                public void onTaskFinished(AsyncTask<Uri, BigDecimal, List<ImageInfo>> task, List<ImageInfo> imageInfos) {
                    if (imageInfos.size() == 1) {
                        imageInfo = imageInfos.get(0);
                        if (imageInfo.getImage() != null) {
                            Matrix matrix = ImageLoader.calculateMatrixScaleToFit(imageInfo.getImage(), width, height);
                            imageView.setImageBitmap(Bitmap.createBitmap(imageInfo.getImage(), 0, 0, imageInfo.getImage().getWidth(), imageInfo.getImage().getHeight(), matrix, false));
                        }
                        textView.setText(imageInfo.getName());
                        progressBar.setVisibility(View.INVISIBLE);

                    }
                }
            });


            asyncTask.execute(uri);
        }
    }

    public void setOnDeleteButtonClickListener(OnDeleteClickListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (OnDeleteClickListener listener : listeners) {
            listener.onDeleteButtonClicked(imageInfo.getUri());
        }
    }

    public Uri getImageInfo() {
        return imageInfo.getUri();
    }
}
