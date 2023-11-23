package io.github.doubi88.slideshowwallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SlideshowControlReceiver  extends BroadcastReceiver {

    public static String PREVIOUS_IMAGE = "io.github.doubi88.slideshowwallpaper.PREVIOUS_IMAGE";
    public static String NEXT_IMAGE = "io.github.doubi88.slideshowwallpaper.NEXT_IMAGE";
    public static String OPEN_IMAGE = "io.github.doubi88.slideshowwallpaper.OPEN_IMAGE";

    private static final String LOG_TAG = SlideshowControlReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "onReceive" + intent.getAction());
        if (NEXT_IMAGE.equals(intent.getAction())) {
            Log.d(LOG_TAG, "Next Image");
        }
    }
}
