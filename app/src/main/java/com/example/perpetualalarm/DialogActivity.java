package com.example.perpetualalarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DialogActivity extends AppCompatActivity {
    private Double delay;
    private int howManyTimes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        // getWindow().setBackgroundDrawable(new ColorDrawable(0));

        setContentView(R.layout.activity_dialog);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String timeText = sharedPreferences.getString("timeText", "No alarm set");

        if (!timeText.matches("No alarm set")) {
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            //Yes button clicked
                            hourSet();

                            finishAffinity();
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            //No button clicked

                            finishAffinity();
                            break;
                    }
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(DialogActivity.this);

            builder.setMessage("Do you want to change the alarm?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finishAffinity();
                }
            }).show();

        } else {
            hourSet();
            finishAffinity();
        }

    }


    private void hourSet() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        double delayHours = 0;
        double delayMinutes = 0;
        boolean amountRestricted = sharedPreferences.getBoolean("amountSwitchState", false);
        if (sharedPreferences.getString("delayHours", "0").equals("") && sharedPreferences.getString("delayMinutes", "0").equals("")) {
            delay = 0.0;
        } else if (sharedPreferences.getString("delayHours", "0").equals("")) {
            delayMinutes = Double.parseDouble(sharedPreferences.getString("delayMinutes", "0"));
            delay = 60 * 1000 * delayMinutes;
        } else if (sharedPreferences.getString("delayMinutes", "0").equals("")) {
            delayHours = Double.parseDouble(sharedPreferences.getString("delayHours", "0"));
            delay = (60 * 60 * 1000) * delayHours;
        } else {
            delayMinutes = Double.parseDouble(sharedPreferences.getString("delayMinutes", "0"));
            delayHours = Double.parseDouble(sharedPreferences.getString("delayHours", "0"));
            delay = (60 * 60 * 1000) * delayHours + (60 * 1000) * delayMinutes;
        }

        if (sharedPreferences.getString("amount", "0").equals("")) {
            howManyTimes = 0;
        } else {
            howManyTimes = Integer.parseInt(sharedPreferences.getString("amount", "0"));
        }

        if (delay > 0) {
            double tmpDelay = delay;
            startAlarm();
            if (sharedPreferences.getInt("desiredBursts", 0) > 0) {
                updateTimeText(tmpDelay);
            } else {
                editor.putString("timeText", "No alarm set");
                editor.apply();
            }
        } else {
            Toast.makeText(this, "Set higher delay!", Toast.LENGTH_LONG).show();
        }
    }

    private void startAlarm() {
        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
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

        int minutesToLast = 0;
        if (hourRestricted) {
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

        int howManyTimesFromHour = -1;
        int minutesTemp = minutesToLast;

        while (minutesTemp >= 0) {
            minutesTemp -= delay / 60000;
            howManyTimesFromHour++;
        }

        if (amountRestricted && hourRestricted) {
            editor.putInt("desiredBursts", Math.min(howManyTimes, howManyTimesFromHour));
            int lowerRestriction = Math.min(howManyTimes, howManyTimesFromHour);
            Toast.makeText(this, "Both restrictions " + lowerRestriction, Toast.LENGTH_LONG).show();
            editor.putBoolean("setRestricted", true);
        } else if (amountRestricted) {
            editor.putInt("desiredBursts", howManyTimes);
            Toast.makeText(this, "Amount restriction " + howManyTimes, Toast.LENGTH_LONG).show();
            editor.putBoolean("setRestricted", true);
        } else if (hourRestricted) {
            editor.putInt("desiredBursts", howManyTimesFromHour);
            Toast.makeText(this, "Hour restriction " + howManyTimesFromHour, Toast.LENGTH_LONG).show();
            editor.putBoolean("setRestricted", true);
        } else {
            Toast.makeText(this, "No restriction", Toast.LENGTH_LONG).show();
            editor.putInt("desiredBursts", 1);
            editor.putBoolean("setRestricted", false);
        }
        editor.putInt("elapsedBursts", 0);
        editor.putString("setDelay", delay.toString());

        editor.apply();
    }

    private void updateTimeText(double tmpDelay) {
        String timeText = "Next alarm set for: \n\n";
        double hoursTemp = tmpDelay / 3600000;
        int hours = (int) hoursTemp;
        int minutes = (int) (tmpDelay - hours * 3600000) / 60000;

        Calendar calendar = Calendar.getInstance();
        int currentHours = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinutes = calendar.get(Calendar.MINUTE);

        int hoursInMinutes = hours * 60 + minutes;
        int addedDays = 0;
        int hoursForDays = (currentHours * 60 + currentMinutes + hoursInMinutes) / 60;

        while (hoursForDays > 23) {
            addedDays++;
            hoursForDays -= 24;
        }

        calendar.add(Calendar.DATE, addedDays);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = df.format(calendar.getTime());

        timeText += formattedDate + "\n";
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
            timeText += hours + ":0" + minutes + "\n";
        } else {
            timeText += hours + ":" + minutes + "\n";
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("timeText", timeText);
        editor.apply();
    }
}


