package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by albus on 18-Dec-17.
 */

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    public static final String PERM_PRIVATE =
            "com.bignerdranch.android.photogallery.PRIVATE";

    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";

    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(
                context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }

        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent
                .getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);

    }

    public PollService() {
        super(TAG);

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;

        if (query == null) {
            items = new FlickrFetchr().fetchRecentPhotos();
        } else {
            items = new FlickrFetchr().searchPhotos(query);
        }

        if (items.size() == 0) {
            return;
        }

        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId)) {
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = getResources();

            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            /*NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(0, notification);

            sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE);*/

            showBackgroundNotification(0, notification);
        }
        QueryPreferences.setLastResultId(this, resultId);

    }

    private void showBackgroundNotification(int requestCode, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
//        sendOrderedBroadcast(i,PERM_PRIVATE,null,null, Activity.RESULT_OK,null,null);

        sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable &&
                cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    @RequiresApi(21)
    public static class PollJobService extends JobService {

        private PollTask mCurrentTask;
        private static final String TAG = "PollJobService";

        public PollJobService() {
        }

        @Override
        public boolean onStartJob(JobParameters params) {
            Log.d(TAG, "onStartJob");

            mCurrentTask = new PollTask();
            mCurrentTask.execute(params);
            return true;
        }

        @Override
        public boolean onStopJob(JobParameters jobParameters) {
            Log.d(TAG, "onStopJob");
            if (mCurrentTask != null) {
                mCurrentTask.cancel(true);
            }
            return true;
        }

        private class PollTask extends AsyncTask<JobParameters, Void, Void> {

            @Override
            protected Void doInBackground(JobParameters... params) {
                Log.d(TAG, "PollTask doInBackground");

                JobParameters jobParams = params[0];
                String query = QueryPreferences.getStoredQuery(getApplicationContext());
                String lastResultId = QueryPreferences.getLastResultId(getApplicationContext());
                List<GalleryItem> items;

                if (query == null) {
                    items = new FlickrFetchr().fetchRecentPhotos();
                } else {
                    items = new FlickrFetchr().searchPhotos(query);
                }

                String resultId = items.get(0).getId();
                if (resultId.equals(lastResultId)) {
                    Log.i(TAG, "Got an old result: " + resultId);
                } else {
                    Log.i(TAG, "Got a new result: " + resultId);

                    Resources resources = getResources();

                    Intent i = PhotoGalleryActivity.newIntent(getApplicationContext());
                    PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, i, 0);
                    Log.d(TAG, "Setting up a notification...");

                    Notification notification = new NotificationCompat.Builder(getApplicationContext())
                            .setTicker(resources.getString(R.string.new_pictures_title))
                            .setSmallIcon(android.R.drawable.ic_menu_report_image)
                            .setContentTitle(resources.getString(R.string.new_pictures_title))
                            .setContentText(resources.getString(R.string.new_pictures_text))
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .build();

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
                    notificationManager.notify(0, notification);
                }
                QueryPreferences.setLastResultId(getApplicationContext(), resultId);

                jobFinished(jobParams, false);
                return null;
            }
        }
    }
}
