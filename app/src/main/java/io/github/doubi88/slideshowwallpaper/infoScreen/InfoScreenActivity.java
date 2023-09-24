package io.github.doubi88.slideshowwallpaper.infoScreen;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import io.github.doubi88.slideshowwallpaper.BuildConfig;
import io.github.doubi88.slideshowwallpaper.R;

public class InfoScreenActivity extends AppCompatActivity {

    private static final int VERSION_INDEX = 0;
    private static final int AUTHOR_INDEX = 1;
    private static final int LICENSE_INDEX = 2;
    private static final int SOURCE_CODE_INDEX = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Resources res = getResources();

        CharSequence[] captions = new CharSequence[4];
        captions[VERSION_INDEX] = res.getString(R.string.info_version_caption);
        captions[AUTHOR_INDEX] = res.getString(R.string.info_author_caption);
        captions[LICENSE_INDEX] = res.getString(R.string.info_license_caption);
        captions[SOURCE_CODE_INDEX] = res.getString(R.string.info_sourcecode_caption);

        CharSequence[] texts = new CharSequence[4];
        texts[VERSION_INDEX] = BuildConfig.VERSION_NAME;
        texts[AUTHOR_INDEX] = res.getString(R.string.author_name) + " <" + res.getString(R.string.author_email) + ">";
        texts[LICENSE_INDEX] = res.getString(R.string.license);
        texts[SOURCE_CODE_INDEX] = res.getString(R.string.source_url);

        Drawable[] icons = new Drawable[4];
        icons[0] = res.getDrawable(R.drawable.ic_launcher_foreground);

        RecyclerView list = findViewById(R.id.recycler_view);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        InfoScreenListAdapter adapter = new InfoScreenListAdapter(this, texts, captions, icons);
        adapter.addListItemClickListener(index -> {
            if (index == AUTHOR_INDEX) {
                String url = "mailto:" + getResources().getString(R.string.author_email);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else if (index == LICENSE_INDEX) {
                String url = getResources().getString(R.string.license_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else if (index == SOURCE_CODE_INDEX) {
                String url = getResources().getString(R.string.source_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        list.setAdapter(adapter);

    }
}