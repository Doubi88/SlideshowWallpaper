package io.github.doubi88.slideshowwallpaper.infoScreen;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;

public class InfoScreenListAdapter extends RecyclerView.Adapter<InfoScreenListAdapter.ViewHolder> {

    public interface ListItemClickListener {
        public void itemClicked(int index);
    }

    private CharSequence[] captions;
    private CharSequence[] texts;
    private Drawable[] icons;
    private LayoutInflater inflater;

    private List<ListItemClickListener> clickListeners;

    // data is passed into the constructor
    InfoScreenListAdapter(Context context, CharSequence[] texts, CharSequence[] captions, Drawable[] icons) {
        this.inflater = LayoutInflater.from(context);
        this.texts = texts;
        this.captions = captions;
        this.icons = icons;

        this.clickListeners = new ArrayList<>(4);
    }

    public void addListItemClickListener(ListItemClickListener listener) {
        if (!clickListeners.contains(listener)) {
            clickListeners.add(listener);
        }
    }
    public void removeListItemCLickListener(ListItemClickListener listener) {
        clickListeners.remove(listener);
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.info_screen_entry, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setText(texts[position]);
        holder.setCaption(captions[position]);
        //holder.setImage(icons[position]);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return texts.length;
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView textView;
        private TextView captionView;
        private ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.info_screen_text_view);
            captionView = itemView.findViewById(R.id.info_screen_caption);
            imageView = itemView.findViewById(R.id.info_screen_image_view);
            itemView.setOnClickListener(this);
        }

        public void setText(CharSequence text) {
            this.textView.setText(text);
        }

        public void setCaption(CharSequence caption) {
            this.captionView.setText(caption);
        }

        public void setImage(Drawable image, String contentDescription) {
            this.imageView.setImageDrawable(image);
            this.imageView.setContentDescription(contentDescription);
        }

        @Override
        public void onClick(View view) {
            for (ListItemClickListener listener : clickListeners) {
                listener.itemClicked(getAdapterPosition());
            }
        }
    }

}
