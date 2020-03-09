package com.example.perpetualalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    public static TextView mTextView;
    private Double delay;
    private int howManyTimes;
    public static boolean active = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        mTextView = findViewById(R.id.textView);


        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String timeText = sharedPreferences.getString("timeText", "No alarm set");
        mTextView.setText(timeText);


        Button buttonSet = findViewById(R.id.button_set);
        buttonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double delayHours=0;
                double delayMinutes=0;
                boolean amountRestricted = sharedPreferences.getBoolean("amountSwitchState", false);
                if (sharedPreferences.getString("delayHours", "0").equals("") && sharedPreferences.getString("delayMinutes", "0").equals("")) {
                    delay = 0.0;
                } else if(sharedPreferences.getString("delayHours", "0").equals("")){
                    delayMinutes = Double.parseDouble(sharedPreferences.getString("delayMinutes", "0"));
                    delay = 60*1000*delayMinutes;
                }else if(sharedPreferences.getString("delayMinutes", "0").equals("")){
                    delayHours = Double.parseDouble(sharedPreferences.getString("delayHours", "0"));
                    delay = (60 * 60 * 1000) * delayHours;
                }else{
                    delayMinutes = Double.parseDouble(sharedPreferences.getString("delayMinutes", "0"));
                    delayHours = Double.parseDouble(sharedPreferences.getString("delayHours", "0"));
                    delay = (60 * 60 * 1000) * delayHours+ (60*1000) * delayMinutes;
                }

                if (sharedPreferences.getString("amount", "0").equals("")) {
                    howManyTimes = 0;
                } else {
                    howManyTimes = Integer.parseInt(sharedPreferences.getString("amount", "0"));
                }

//                if (delay != 0.0) {
//                    delay = (60 * 60 * 1000) * delayHours+60*1000*delayMinutes;
//                }

                if (amountRestricted && howManyTimes <= 0) {
                    cancelAlarm();
                } else if (delay>0) {
                    double tmpDelay = delay;
                    startAlarm();
                    if(sharedPreferences.getInt("desiredBursts", 0)>0){
                        updateTimeText(tmpDelay);
                    }
                    else{
                        mTextView.setText("No alarm set");
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Set higher delay!", Toast.LENGTH_LONG).show();
                }
            }
        });

        Button buttonCancelAlarm = findViewById(R.id.button_cancel);
        buttonCancelAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlarm();
            }
        });

        Button buttonSettings = findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
    }


    private void startAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);


        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay.longValue(),
                delay.longValue(), pendingIntent);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean amountRestricted = sharedPreferences.getBoolean("amountSwitchState", false);
        boolean hourRestricted = sharedPreferences.getBoolean("hourSwitchState", false);

        Calendar calendar = Calendar.getInstance();
        int currentHours = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinutes = calendar.get(Calendar.MINUTE) + currentHours * 60;

        int minutesToLast=0;
        if(hourRestricted)
        {
            String setTime = sharedPreferences.getString("lastHour", "false");
            String[] parts = setTime.split(":");
            String part1 = parts[0]; // 004
            String part2 = parts[1];
            int setHours = Integer.parseInt(part1);
            int setMinutes = Integer.parseInt(part2) + setHours * 60;

            if (!setTime.equals("false")) {
                if (currentMinutes < setMinutes) {
                    minutesToLast = setMinutes - currentMinutes;
                } else {
                    minutesToLast = 24 * 60 - currentMinutes + setMinutes;
                }
            }
        }



        int howManyTimesFromHour=-1;
        int minutesTemp=minutesToLast;

        while(minutesTemp>=0)
        {
            minutesTemp-=delay/60000;
            howManyTimesFromHour++;
        }


        if(amountRestricted && hourRestricted)
        {
            editor.putInt("desiredBursts", Math.min(howManyTimes, howManyTimesFromHour));
            Toast.makeText(MainActivity.this, "Both restrictions "+howManyTimes+" and "+ howManyTimesFromHour, Toast.LENGTH_LONG).show();
            editor.putBoolean("setRestricted", true);
        }else if(amountRestricted){
            editor.putInt("desiredBursts", howManyTimes);
            Toast.makeText(MainActivity.this, "Amount restriction "+howManyTimes, Toast.LENGTH_LONG).show();
            editor.putBoolean("setRestricted", true);
        }else if(hourRestricted){
            editor.putInt("desiredBursts", howManyTimesFromHour);
            Toast.makeText(MainActivity.this, "Hour restriction "+ howManyTimesFromHour, Toast.LENGTH_LONG).show();
            editor.putBoolean("setRestricted", true);
        }else{
            Toast.makeText(MainActivity.this, "No restriction", Toast.LENGTH_LONG).show();
            editor.putInt("desiredBursts", 1);
            editor.putBoolean("setRestricted", false);
        }
        editor.putInt("elapsedBursts", 0);
        editor.putString("setDelay", delay.toString());

        editor.apply();


    }

    public void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("desiredBursts", 0);
        editor.putInt("elapsedBursts", 0);
        editor.putString("timeText", "No alarm set");
        editor.apply();

        alarmManager.cancel(pendingIntent);
        Toast.makeText(MainActivity.this, "Alarm cancelled", Toast.LENGTH_LONG).show();
        mTextView.setText("No alarm set");

    }

    private void updateTimeText(double tmpDelay) {
        String timeText = "Next alarm set for: \n\n";
       // Toast.makeText(MainActivity.this, "Updating time", Toast.LENGTH_LONG).show();
        double hoursTemp = tmpDelay / 3600000;
        int hours = (int) hoursTemp;
        int minutes = (int) (tmpDelay - hours * 3600000) / 60000;
        int seconds = (int) (tmpDelay - hours * 3600000 - minutes * 60000) / 1000;

        Calendar calendar = Calendar.getInstance();
        int currentHours = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinutes = calendar.get(Calendar.MINUTE);
        int currentSeconds = calendar.get(Calendar.SECOND);
        hours += currentHours;
        minutes += currentMinutes;
        seconds += currentSeconds;
        if (seconds > 59) {
            int iterator = 0;
            while (seconds > 59) {
                seconds -= 60;
                iterator++;
            }
            minutes += iterator;
        }
        if (minutes > 59) {
            int iterator = 0;
            while (minutes > 59) {
                minutes -= 60;
                iterator++;
            }
            hours += iterator;
        }
        if (hours > 23) {
            while (hours > 23) {
                hours -= 24;
            }
        }
        if (minutes < 10) {
            timeText += hours + ":0" + minutes + "\n";
        } else {
            timeText += hours + ":" + minutes + "\n";
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("timeText", timeText);
        editor.apply();
        mTextView.setText(timeText);
    }

    public static void changeText(String args) {
        mTextView.setText(args);
    }

    public static boolean isActive() {
        return active;
    }

    public void openSettings() {
        Intent intent = new Intent(this, Settings.class);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();

        active = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        active = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        active = false;
    }
}
