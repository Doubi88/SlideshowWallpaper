package de.tobi.slideshowwallpaper;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class ImagePreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_preferences);
        getSupportFragmentManager().beginTransaction().replace(R.id.content, new ImagesPreferenceFragment()).commit();
    }
}
