package io.github.doubi88.slideshowwallpaper;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class SlideshowControlWidgetProvider extends AppWidgetProvider {
    private static final String LOG_TAG = SlideshowControlWidgetProvider.class.getSimpleName();

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {


        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.slideshow_control);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
        createPendingIntent(SlideshowControlReceiver.PREVIOUS_IMAGE, R.id.btn_left, views, context);
        createPendingIntent(SlideshowControlReceiver.NEXT_IMAGE, R.id.btn_right, views, context);
        createPendingIntent(SlideshowControlReceiver.OPEN_IMAGE, R.id.btn_open, views, context);
    }

    private void createPendingIntent(String action, int buttonId, RemoteViews views, Context context) {
        Intent intent = new Intent(action);
        intent.setComponent(new ComponentName(context.getPackageName(), SlideshowControlReceiver.class.getCanonicalName()));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(buttonId, pendingIntent );

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}