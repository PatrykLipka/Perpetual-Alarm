package com.example.perpetualalarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public static TextView mTextView;
    private Double delay;
    private int howManyTimes;
    public static boolean active = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View contentView = (View)findViewById(R.id.wholeActivity);
        contentView.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeTop() {
                Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
            }
            public void onSwipeRight() {
                openSettings();
            }
            public void onSwipeLeft() {
                openSettings();
            }
            public void onSwipeBottom() {
                Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
            }

        });
        mTextView = findViewById(R.id.textView);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String timeText = sharedPreferences.getString("timeText", "No alarm set");
        mTextView.setText(timeText);

        Button buttonSet = findViewById(R.id.button_set);
        buttonSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (!mTextView.getText().toString().equals("No alarm set")) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    //Yes button clicked
                                    hourSet();
                                    String input = sharedPreferences.getString("timeTextNoDate", "No alarm set");
                                    int desiredBursts = sharedPreferences.getInt("desiredBursts", 0);
                                    if(desiredBursts>1){
                                        input+= "  Remaining bursts: " + desiredBursts;
                                    }
                                    startService(input);
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    //No button clicked
                                    break;
                            }
                        }
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Do you want to change the alarm?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();
                } else {
                    hourSet();
                    String input = sharedPreferences.getString("timeTextNoDate", "No alarm set");
                    int desiredBursts = sharedPreferences.getInt("desiredBursts", 0);
                    if(desiredBursts>1){
                        input+= "  Remaining bursts: " + desiredBursts;
                    }
                    startService(input);
                }
            }
        });

        Button buttonCancelAlarm = findViewById(R.id.button_cancel);
        buttonCancelAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlarm();
                stopService();
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

    public void startService(String input) {
        if(!input.equals("No alarm set")) {
            Intent serviceIntent = new Intent(this, ForegroundNotification.class);
            serviceIntent.putExtra("inputExtra", input);
            serviceIntent.putExtra("inputType", "Alarm set");
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }
    public void stopService() {
        Intent serviceIntent = new Intent(this, ForegroundNotification.class);
        stopService(serviceIntent);
    }


    private void hourSet(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
            int lowerRestriction = Math.min(howManyTimes, howManyTimesFromHour);
            Toast.makeText(MainActivity.this, "Both restrictions "+lowerRestriction, Toast.LENGTH_LONG).show();
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
        editor.putString("timeTextNoDate", "No alarm set");
        editor.apply();

        alarmManager.cancel(pendingIntent);
        Toast.makeText(MainActivity.this, "Alarm cancelled", Toast.LENGTH_LONG).show();
        mTextView.setText("No alarm set");

    }

    private void updateTimeText(double tmpDelay) {
        String timeText = "Next alarm set for: \n\n";
        String timeTextNoDate = "Next alarm set for: ";
        double hoursTemp = tmpDelay / 3600000;
        int hours = (int) hoursTemp;
        int minutes = (int) (tmpDelay - hours * 3600000) / 60000;

        Calendar calendar = Calendar.getInstance();
        int currentHours = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinutes = calendar.get(Calendar.MINUTE);

        int hoursInMinutes=hours*60+minutes;
        int addedDays=0;
        int hoursForDays=(currentHours*60+currentMinutes+hoursInMinutes)/60;

        while(hoursForDays>23)
        {
            addedDays++;
            hoursForDays-=24;
        }

        calendar.add(Calendar.DATE,addedDays);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = df.format(calendar.getTime());

        hours += currentHours;
        minutes += currentMinutes;
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
            timeText += hours + ":0" + minutes + "\n" + "Date: " + formattedDate+"\n";
            timeTextNoDate += hours + ":0" + minutes;

        } else {
            timeText += hours + ":" + minutes + "\n" + "Date: " + formattedDate+"\n";
            timeTextNoDate += hours + ":" + minutes;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("timeText", timeText);
        editor.putString("timeTextNoDate", timeTextNoDate);
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

        mTextView = findViewById(R.id.textView);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String timeText = sharedPreferences.getString("timeText", "No alarm set");
        mTextView.setText(timeText);

        active = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        mTextView = findViewById(R.id.textView);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String timeText = sharedPreferences.getString("timeText", "No alarm set");
        mTextView.setText(timeText);

        active = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        active = false;
        finishAffinity();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        active = false;
    }
}
