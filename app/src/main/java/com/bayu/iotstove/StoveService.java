package com.bayu.iotstove;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.Provider;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class StoveService extends Service {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference outputRef;
    DatabaseReference iotRef;

    Stove stove;

    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent!=null){
            firebaseDatabase = FirebaseDatabase.getInstance();
            outputRef = firebaseDatabase.getReference("output");
            iotRef = firebaseDatabase.getReference("iot");

            double maxT = intent.getExtras().getDouble("maxT");

            createNotificationChannel();
            Intent notificationIntent = new Intent(this, StoveOn.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, 0);
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Foreground Service")
                    .setContentText("Start")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);

            getLiveTemp(maxT);

        }

        return Service.START_STICKY;
    }

    private void getLiveTemp(double maxT) {

        outputRef.child("realtime_temperature").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                getHeatingStatus();

                if (snapshot.getValue(Float.class)==maxT && stove.isHeating()){
                    new CountDownTimer(TimeUnit.MINUTES.toMillis(Math.round(maxT)),1000) {
                        @Override
                        public void onTick(long l) {

                        }

                        @Override
                        public void onFinish() {

                        }
                    }.start();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    private void getHeatingStatus() {
        iotRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                stove = snapshot.getValue(Stove.class);
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not Yet Implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
