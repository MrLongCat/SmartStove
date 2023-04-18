package com.bayu.iotstove;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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



//------------------------NEW--------------------------------//
        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimePickerDialog timePickerDialog;
                int hour = 0;
                int minute = 0;
                timePickerDialog = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        String hourS = String.valueOf(i);
                        if(hourS.length()==1){
                            hourS="0"+hourS;
                        }
                        String minuteS = String.valueOf(i1);
                        if (minuteS.length()==1){
                            minuteS="0"+minuteS;
                        }
                        time.setText(hourS + ":" + minuteS);
                    }
                },hour,minute,true);
                timePickerDialog.setTitle("Set Duration");
                timePickerDialog.show();
            }
        });

//---------------------------NEW------------------------------//
        onOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Float temp = circularSeekBar.getProgress();
                String[] durationArr = time.getText().toString().split(":");
                Integer totalDuration = (Integer.parseInt(durationArr[0])*60)+Integer.parseInt(durationArr[1]);
                if (temp<40){
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("Temperature Too Low");
                    alertDialog.setMessage("Please, Set the temperature more than 40°C");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                } else if (totalDuration<1){
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("Duration Too Short");
                    alertDialog.setMessage("Please, Set the duration more than 1 Minute");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            (dialog, which) -> dialog.dismiss());
                    alertDialog.show();
                } else {
                    InitiateStove(temp,totalDuration);
                }

            }
        });

        if (StoveService.IS_ACTIVITY_RUNNING) {
            Log.d("Stove Servoce","MASIH AKTIF CUUUK");
        } else {
            Log.d("Stove Servoce","Yaaayyy Udah Mati :D");
        }

        getRelay();

    }

    private void InitiateStove(Float temp, Integer totalDuration) {
        Stove stove = new Stove();
        stove.setMaxD(Double.valueOf(totalDuration));
        stove.setMaxT(Double.valueOf(temp));
        stove.setRelay(1);
        stove.setHeating(true);
        stove.setReported(true);
//        stove.setStartTime(dateFormat.format(dt));
        stove.setStartTime("");

        databaseReference.setValue(stove);
        Intent intent = new Intent(MainActivity.this, StoveOn.class);
        intent.putExtra("maxT",stove.getMaxT());
        intent.putExtra("maxD",stove.getMaxD());
        startActivity(intent);
        finish();
    }


    //------------------------------NEW------------------------------//
    private void getRelay() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Stove stove = snapshot.getValue(Stove.class);
                if (!stove.isReported()){
                    Intent intent = new Intent(MainActivity.this, Report.class);
                    startActivity(intent);
                    finish();
                }
                if (stove.getRelay()==1){
                    Intent intent = new Intent(MainActivity.this, StoveOn.class);
                    intent.putExtra("maxT",stove.getMaxT());
                    intent.putExtra("maxD",stove.getMaxD());
                    startActivity(intent);
                    finish();
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                    content.setVisibility(View.VISIBLE);
                }
                databaseReference.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Ups...");
                alertDialog.setMessage("Please Check your Connection and Try Again...");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
                finish();
            }
        });
    }
}