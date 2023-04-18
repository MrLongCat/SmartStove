package com.bayu.iotstove;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.stefanodp91.android.circularseekbar.CircularSeekBar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class StoveOn extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference outputRef;
    DatabaseReference iotRef;
    ValueEventListener getStoveVal, getLiveTemp, getLiveVol;

    CircularSeekBar circularSeekBar;

    TextView maxTemp, startTime, endTime, stoveStatus, volume;
    ImageView imageStatus;

    ImageView turnOff;

    Intent service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stove_on);

        circularSeekBar = findViewById(R.id.temperature);
        maxTemp = findViewById(R.id.max_temp);
        startTime = findViewById(R.id.start_time);
        endTime = findViewById(R.id.end_time);
        turnOff = findViewById(R.id.on_off_btn);
        stoveStatus = findViewById(R.id.status);
        imageStatus = findViewById(R.id.image_status);
        volume = findViewById(R.id.volume);

        firebaseDatabase = FirebaseDatabase.getInstance();
        outputRef = firebaseDatabase.getReference("output");
        iotRef = firebaseDatabase.getReference("iot");

        turnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(StoveOn.this);
                builder.setTitle("Turn Off");
                builder.setMessage("Are you sure want to Turn Off Stove?");
                builder.setCancelable(false);
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Stove stove = new Stove();
                        stove.setHeating(false);
//                        stove.setStartTime("");
//                        stove.setMaxT(0.0);
//                        stove.setMaxD(0.0);
                        stove.setRelay(0);
                        iotRef.setValue(stove);
                        outputRef.child("realtime_temperature").setValue(0);
                        stopService(service);
                        StoveService.IS_ACTIVITY_RUNNING = false;
                        Intent intent = new Intent(StoveOn.this, Report.class);
                        startActivity(intent);
                        finish();
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog alertDialog = builder.create();
                if (!isFinishing()) {
                    alertDialog.show();
                }
            }
        });

        if (!StoveService.IS_ACTIVITY_RUNNING) {
            service = new Intent(getApplicationContext(), StoveService.class);
            service.putExtra("maxT", getIntent().getDoubleExtra("maxT", 40));
            service.putExtra("maxD", getIntent().getDoubleExtra("maxD", 1));
            startService(service);
            Log.d("StoveService", "STARTED");
        }

        getLiveTemp();

        getLiveVolume();

        getStoveVal();

    }

    private void getLiveVolume() {

        getLiveVol = outputRef.child("realtime_volume").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                volume.setText(String.valueOf(snapshot.getValue(Float.class)));
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    //----------------------------NEW--------------------------//
    private void getStoveVal() {

        getStoveVal = iotRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Stove stove = snapshot.getValue(Stove.class);
                System.out.println(stove.getRelay());
                maxTemp.setText(String.valueOf(stove.getMaxT()));
                if (stove.getRelay() == 1) {
                    try {
                        if (stove.isHeating()){
                            stoveStatus.setText("HEATING");
                            imageStatus.setImageResource(R.drawable.small_fire);
                        } else {
                            stoveStatus.setText("ACTIVE");
                            imageStatus.setImageResource(R.drawable.active_fire);
                        }
                        imageStatus.setScaleType(ImageView.ScaleType.FIT_CENTER);

                        if (stove.getStartTime().equals("") && !stove.isHeating()) {
                            Date dt = new Date();
                            SimpleDateFormat dateFormat;
                            dateFormat = new SimpleDateFormat("kk:mm");
                            stove.setStartTime(dateFormat.format(dt));
                            iotRef.child("startTime").setValue(stove.getStartTime());
                            startTime.setText(stove.getStartTime());

                            String myTime = stove.getStartTime();
                            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                            Date d = df.parse(myTime);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(d);
                            cal.add(Calendar.MINUTE, (int) Math.round(stove.getMaxD()));
                            String newTime = df.format(cal.getTime());
                            endTime.setText(newTime);
                        } else if (!stove.getStartTime().equals("")){
                            startTime.setText(stove.getStartTime());
                            String myTime = stove.getStartTime();
                            SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                            Date d = df.parse(myTime);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(d);
                            cal.add(Calendar.MINUTE, (int) Math.round(stove.getMaxD()));
                            String newTime = df.format(cal.getTime());
                            endTime.setText(newTime);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
//                    outputRef.child("realtime_volume").addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(DataSnapshot snapshot) {
//                            iotRef.child("endVolume").setValue(snapshot.getValue());
//                        }
//                        @Override
//                        public void onCancelled(DatabaseError error) {
//
//                        }
//                    });
//                    iotRef.child("relay").setValue(0);
//                    iotRef.child("reported").setValue(false);
                    stopService(service);
                    StoveService.IS_ACTIVITY_RUNNING = false;

                    AlertDialog.Builder builder = new AlertDialog.Builder(StoveOn.this);
                    builder.setTitle("Time's Up!");
                    builder.setMessage("The Cooking has been completed!");
                    builder.setCancelable(false);
                    builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(StoveOn.this, Report.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    if (!isFinishing()) {
                        alertDialog.show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }

    private void getLiveTemp() {

        getLiveTemp = outputRef.child("realtime_temperature").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                circularSeekBar.setProgress(snapshot.getValue(Float.class));
                circularSeekBar.setText(snapshot.getValue(Float.class) + "°C");
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        iotRef.removeEventListener(getStoveVal);
        iotRef.removeEventListener(getLiveTemp);
        iotRef.removeEventListener(getLiveVol);
    }

}