package de.tobi.slideshowwallpaper.preferences;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import de.tobi.slideshowwallpaper.R;

public class WallpaperPreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_preferences);
        getSupportFragmentManager().beginTransaction().replace(R.id.content, new WallpaperPreferencesFragment()).commit();
    }
}
