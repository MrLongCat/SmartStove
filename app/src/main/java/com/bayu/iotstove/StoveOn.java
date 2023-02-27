package com.bayu.iotstove;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

    CircularSeekBar circularSeekBar;

    TextView maxTemp,startTime,endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stove_on);

        circularSeekBar = findViewById(R.id.temperature);
        maxTemp = findViewById(R.id.max_temp);
        startTime = findViewById(R.id.start_time);
        endTime = findViewById(R.id.end_time);

        firebaseDatabase = FirebaseDatabase.getInstance();
        outputRef = firebaseDatabase.getReference("output");
        iotRef = firebaseDatabase.getReference("iot");

        getLiveTemp();

        getStoveVal();

    }

    private void getStoveVal() {

        iotRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Stove stove = snapshot.getValue(Stove.class);

                Intent service = new Intent(getApplicationContext(), StoveService.class);
                service.putExtra("maxT",stove.getMaxT());
                startService(service);

                maxTemp.setText(String.valueOf(stove.getMaxT()));
                startTime.setText(stove.getStartTime());
                try {
                    String myTime = stove.getStartTime();
                    SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                    Date d = df.parse(myTime);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(d);
                    cal.add(Calendar.MINUTE, (int) Math.round(stove.getMaxD()));
                    String newTime = df.format(cal.getTime());
                    endTime.setText(newTime);
                    if (stove.getRelay().equals(0)){
                        Intent i = new Intent(getApplicationContext(), StoveService.class);
                        stopService(i);
                        Intent intent = new Intent(StoveOn.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

    }

    private void getLiveTemp() {

        outputRef.child("realtime_temperature").addValueEventListener(new ValueEventListener() {
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
}