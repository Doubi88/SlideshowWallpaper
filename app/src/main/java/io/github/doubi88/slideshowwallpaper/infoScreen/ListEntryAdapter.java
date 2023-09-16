package io.github.doubi88.slideshowwallpaper.infoScreen;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.github.doubi88.slideshowwallpaper.R;

public class ListEntryAdapter extends RecyclerView.Adapter<ListEntryAdapter.ViewHolder> {

    private CharSequence[] captions;
    private CharSequence[] texts;
    private Drawable[] icons;
    private LayoutInflater inflater;

    // data is passed into the constructor
    ListEntryAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
        Resources res = Resources.getSystem();

        captions = new CharSequence[4];
        captions[0] = res.getString(R.string.info_version_caption);
        captions[1] = res.getString(R.string.info_author_caption);
        captions[2] = res.getString(R.string.info_license_caption);
        captions[3] = res.getString(R.string.info_sourcecode_caption);

        texts = new CharSequence[4];
        texts[0] = BuildConfig
        texts[1] = res.getString(R.string.author_name); + " <" + res.getString(R.string.author_email) + ">";
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
        String animal = mData.get(position);
        holder.setText(animal);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView textView;
        private TextView captionView;
        private ImageView imageView;
        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.info_screen_text_view);
            captionView = itemView.findViewById(R.id.info_screen_caption);
            imageView = itemView.findViewById(R.id.info_screen_image_view)
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
            if (clickListener != null) clickListener.onItemClick(view, getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.clickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
