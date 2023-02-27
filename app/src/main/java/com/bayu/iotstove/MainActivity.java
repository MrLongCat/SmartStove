package com.bayu.iotstove;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.stefanodp91.android.circularseekbar.CircularSeekBar;
import com.github.stefanodp91.android.circularseekbar.OnCircularSeekBarChangeListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    LinearLayout progressBar;
    ImageView onOffBtn;
    CircularSeekBar circularSeekBar;
    TextView time;
    RelativeLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progress);
        circularSeekBar = findViewById(R.id.temperature);
        time = findViewById(R.id.time);
        onOffBtn = findViewById(R.id.on_off_btn);
        content = findViewById(R.id.content);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("iot");

        circularSeekBar.setOnRoundedSeekChangeListener(new OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChange(CircularSeekBar CircularSeekBar, float progress) {
                circularSeekBar.setText(Math.round(circularSeekBar.getProgress())+ "°C");
            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar CircularSeekBar) {
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar CircularSeekBar) {
            }
        });


        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog;
                int hour = 0;
                int minute = 0;
                timePickerDialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        time.setText(i + ":" + i1);
                    }
                },hour,minute,true);
                timePickerDialog.setTitle("TEST");
                timePickerDialog.show();
            }
        });


        onOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Float temp = circularSeekBar.getProgress();
                String[] durationArr = time.getText().toString().split(":");
                Integer totalDuration = (Integer.parseInt(durationArr[0])*60)+Integer.parseInt(durationArr[1]);
                if (temp<1){
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("Terjadi Kesalahan");
                    alertDialog.setMessage("Cek koneksi kamu dan coba lagi ya...");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                } else if (totalDuration==0){
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("Terjadi Kesalahan");
                    alertDialog.setMessage("Cek koneksi kamu dan coba lagi ya...");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                } else {
                    InitiateStove(temp,totalDuration);
                }

            }
        });


        getRelay();

    }

    private void InitiateStove(Float temp, Integer totalDuration) {

        Date dt = new Date();
        SimpleDateFormat dateFormat;
        dateFormat = new SimpleDateFormat("kk:mm");


        Stove stove = new Stove();
        stove.setMaxD(Double.valueOf(totalDuration));
        stove.setMaxT(Double.valueOf(temp));
        stove.setRelay(1);
        stove.setHeating(true);
        stove.setStartTime(String.valueOf(dateFormat.format(dt)));

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                databaseReference.setValue(stove);
                Intent intent = new Intent(MainActivity.this, StoveOn.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Terjadi Kesalahan");
                alertDialog.setMessage("Cek koneksi kamu dan coba lagi ya...");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            }
        });
    }


    private void getRelay() {
        databaseReference.child("relay").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Integer relayVal = snapshot.getValue(Integer.class);
                Toast.makeText(MainActivity.this, "SUCCESS", Toast.LENGTH_SHORT).show();
                if (relayVal==1){
                    Intent intent = new Intent(MainActivity.this, StoveOn.class);
                    startActivity(intent);
                    finish();
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                    content.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Terjadi Kesalahan");
                alertDialog.setMessage("Cek koneksi kamu dan coba lagi ya...");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
                finish();
            }
        });

    }
}