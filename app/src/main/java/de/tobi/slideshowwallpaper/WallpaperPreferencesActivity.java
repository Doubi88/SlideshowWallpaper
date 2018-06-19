package de.tobi.slideshowwallpaper;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;

public class WallpaperPreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_preferences);
        getSupportFragmentManager().beginTransaction().replace(R.id.content, new WallpaperPreferencesFragment()).commit();
    }
}
