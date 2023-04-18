package com.bayu.iotstove;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Report extends AppCompatActivity {

    LinearLayout backToMenu;

    TextView startTime, endTime, initVolume, finalVolume, maxTemp, volChange;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        backToMenu = findViewById(R.id.back_to_menu);
        startTime = findViewById(R.id.start_time);
        endTime = findViewById(R.id.end_time);
        initVolume = findViewById(R.id.start_volume);
        finalVolume = findViewById(R.id.end_volume);
        maxTemp = findViewById(R.id.max_temp);
        volChange = findViewById(R.id.volume_change);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("iot");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Stove stove = snapshot.getValue(Stove.class);
                startTime.setText(stove.getStartTime());
                String myTime = stove.getStartTime();
                SimpleDateFormat df = new SimpleDateFormat("HH:mm");
                Date d = null;
                try {
                    d = df.parse(myTime);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                cal.add(Calendar.MINUTE, (int) Math.round(stove.getMaxD()));
                String newTime = df.format(cal.getTime());
                endTime.setText(newTime);
                initVolume.setText(String.valueOf(stove.getStartVolume()));
                finalVolume.setText(String.valueOf(stove.getEndVolume()));
                maxTemp.setText(String.valueOf(stove.getMaxT()));
                double volDiff = 100-((stove.getEndVolume()/stove.getStartVolume())*100);
                volChange.setText(String.valueOf(volDiff));
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

        backToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Stove stove = new Stove();
                stove.setReported(true);
                stove.setEndVolume(0.0);
                stove.setStartVolume(0.0);
                stove.setHeating(false);
                stove.setMaxD(0.0);
                stove.setMaxT(0.0);
                stove.setStartTime("");
                stove.setRelay(0);
                databaseReference.setValue(stove);
                Intent intent = new Intent(Report.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}