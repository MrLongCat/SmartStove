package com.bayu.iotstove;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
import java.util.logging.Logger;

public class StoveService extends Service {

    public static  boolean IS_ACTIVITY_RUNNING = false;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference outputRef;
    DatabaseReference iotRef;
    ValueEventListener getLiveTemp, getHeatingStatus;

    Stove stove = new Stove();

    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        IS_ACTIVITY_RUNNING = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (IS_ACTIVITY_RUNNING){
            if (intent!=null){
                Log.d("SERVICE_HASH",String.valueOf(this.hashCode()));
                firebaseDatabase = FirebaseDatabase.getInstance();
                outputRef = firebaseDatabase.getReference("output");
                iotRef = firebaseDatabase.getReference("iot");

                double maxT = intent.getExtras().getDouble("maxT");
                double maxD = intent.getExtras().getDouble("maxD");

                Log.d("MaxT",String.valueOf(maxT));
                Log.d("MaxD",String.valueOf(maxD));

                createNotificationChannel();

                Intent notificationIntent = new Intent(this, StoveOn.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        0, notificationIntent, PendingIntent.FLAG_MUTABLE);
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("SmartStove")
                        .setContentText("Stove : ON")
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentIntent(pendingIntent)
                        .build();
                startForeground(1, notification);

                getLiveTemp(maxT,maxD);
            }
        }
        return Service.START_NOT_STICKY;
    }

    //-------------------------NEW----------------------//
    private void getLiveTemp(double maxT,double maxD) {
        getLiveTemp = outputRef.child("realtime_temperature").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(String.valueOf(this.hashCode()),String.valueOf(maxD));
                Log.d(String.valueOf(this.hashCode()),String.valueOf(maxT));
                getHeatingStatus();
                float realTemp = snapshot.getValue(Float.class);

                if (realTemp >= maxT && stove.isHeating()){
                    updateHeatingStatus();
                    new CountDownTimer(TimeUnit.MINUTES.toMillis(Math.round(maxD)),1000) {
                        @Override
                        public void onTick(long l) {
                            Log.d(String.valueOf(this.hashCode()),String.valueOf(l));
                        }
                        @Override
                        public void onFinish() {
                            notifFinish();
                            stopRelay();
                        }
                    }.start();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    private void notifFinish() {
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O){
            NotificationChannel channel= new NotificationChannel("My Notification","My Notification",NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager =getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            String message="Hello Programming Digest";
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"My Notification");
            builder.setContentTitle("NotificationTitle");
            builder.setContentText(message);
            builder.setSmallIcon(R.drawable.ic_launcher_foreground);
            builder.setAutoCancel(true);
            NotificationManagerCompat managerCompat=NotificationManagerCompat.from(this);
            managerCompat.notify(1,builder.build());
        }
    }

    private void updateHeatingStatus() {
        iotRef.child("heating").setValue(false);
    }

    //---------------------------NEW---------------------//
    private void stopRelay(){
        Log.d("Stove Service","STOPPED");
        stove.setHeating(false);
        stove.setStartTime("");
        stove.setMaxT(0.0);
        stove.setMaxD(0.0);
        stove.setRelay(0);
        iotRef.setValue(stove);
        outputRef.child("realtime_temperature").setValue(0);
        outputRef.child("realtime_temperature").removeEventListener(getLiveTemp);
        getLiveTemp=null;
        iotRef.removeEventListener(getHeatingStatus);
        getHeatingStatus=null;
        IS_ACTIVITY_RUNNING=false;
        stopSelf();
    }

    private void getHeatingStatus() {
        getHeatingStatus = iotRef.addValueEventListener(new ValueEventListener() {
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
        IS_ACTIVITY_RUNNING=false;
        if (getLiveTemp!=null){
            outputRef.removeEventListener(getLiveTemp);
        }
        if (getHeatingStatus!=null){
            iotRef.removeEventListener(getHeatingStatus);
        }
        getLiveTemp=null;
        getHeatingStatus=null;
        stopSelf();
    }

}
