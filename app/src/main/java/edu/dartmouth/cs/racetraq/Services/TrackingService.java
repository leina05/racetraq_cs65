package edu.dartmouth.cs.racetraq.Services;

        import android.Manifest;
        import android.app.Notification;
        import android.app.NotificationChannel;
        import android.app.NotificationManager;
        import android.app.PendingIntent;
        import android.app.Service;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.location.Criteria;
        import android.location.Location;
        import android.location.LocationListener;
        import android.location.LocationManager;
        import android.os.Build;
        import android.os.Bundle;
        import android.os.IBinder;
        import android.support.annotation.NonNull;
        import android.support.annotation.Nullable;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.app.NotificationCompat;
        import android.util.Log;

        import com.google.android.gms.tasks.OnFailureListener;
        import com.google.android.gms.tasks.OnSuccessListener;
        import com.google.android.gms.tasks.Task;

        import edu.dartmouth.cs.racetraq.DriveActivity;
        import edu.dartmouth.cs.racetraq.MainActivity;
        import edu.dartmouth.cs.racetraq.R;

public class TrackingService extends Service {
    private static final String CHANNEL_ID = "notification_channel";
    private static final int DETECTION_INTERVAL_IN_MILLISECONDS = 10000;    // check for new activity every 10 seconds
    private static final String TAG = "myruns4.TrackingService";

    private static boolean isRunning = false;
    private Notification notification;
    private NotificationManager notificationManager;
    private PendingIntent mPendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        // send notification
        if (notificationManager != null)
        {
            notificationManager.notify(0, notification);
        }

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Set up notification
        //setupNotification();

        // start location update
        setupLocation();


        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationManager.cancelAll();
        isRunning = false;
    }

    /**
     * Set up location tracking notification
     */
//    private void setupNotification()
//    {
//        Context context = getApplicationContext();
//        String notificationTitle = "MyRuns";
//        String notificationText = "MyRuns is using your location";
//        Intent notifyIntent = new Intent(this, MainActivity.class);
//        notifyIntent.setAction(Intent.ACTION_MAIN);
//        notifyIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//
//        // create pending intent to return to Map Activity
//        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent,
//                0);
//
//        // set up a notification with the pending intent
//        NotificationChannel notificationChannel = null;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notificationChannel = new NotificationChannel(CHANNEL_ID,
//                    "channel name", NotificationManager.IMPORTANCE_DEFAULT);
//        }
//
//        NotificationCompat.Builder notificationBuilder =
//                new NotificationCompat.Builder(this, CHANNEL_ID)
//                        .setContentTitle(notificationTitle)
//                        .setContentText(notificationText)
//                        .setSmallIcon(R.drawable.myruns_icon)
//                        .setContentIntent(pendingIntent); // note the pending intent to launch browser
//
//        notification = notificationBuilder.build();
//
//        // get the notification manager
//        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        if (notificationManager != null) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                notificationManager.createNotificationChannel(notificationChannel);
//            }
//        }
//    }

    /**
     * Set up location services
     */
    void setupLocation()
    {
        // get location manager
        LocationManager locationManager;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // set criteria
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        String provider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location loc = locationManager.getLastKnownLocation(provider);
        broadcastLocation(loc);

        // update once every second, min distance 0 therefore not considered
        locationManager.requestLocationUpdates(provider, 1000, 0, locationListener);
    }


    /**
     * Broadcast location to broadcast receiver in MapDisplayActivity
     */
    private void broadcastLocation(Location location)
    {
        Intent locationIntent = new Intent();
        locationIntent.setAction(DriveActivity.LOCATION_RECEIVE_ACTION);
        locationIntent.putExtra(DriveActivity.LOCATION_KEY, location);

        sendBroadcast(locationIntent);
    }

    /**
     * LocationListener to receive location updates
     */
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // send location to MapDisplayActivity
            broadcastLocation(location);

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    public static boolean isRunning()
    {
        return isRunning;
    }


}

