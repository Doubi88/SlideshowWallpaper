package de.tobi.slideshowwallpaper.utilities;

import android.os.AsyncTask;

public interface ProgressListener<Params, Progress, Result> {

    public void onProgressChanged(AsyncTask<Params, Progress, Result> task, Progress current, Progress max);
    public void onTaskFinished(AsyncTask<Params, Progress, Result> task, Result result);
    public void onTaskCancelled(AsyncTask<Params, Progress, Result> task, Result result);
}
