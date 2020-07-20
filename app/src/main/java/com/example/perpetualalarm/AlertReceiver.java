package com.example.perpetualalarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class AlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationHelper notificationHelper = new NotificationHelper(context);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int desiredBursts = sharedPreferences.getInt("desiredBursts", 1);
        int elapsedBursts = sharedPreferences.getInt("elapsedBursts", 0);
        int remainingBursts = desiredBursts - elapsedBursts;
        String timeText = sharedPreferences.getString("timeText", "No alarm set");
        String timeTextNoDate = sharedPreferences.getString("timeTextNoDate", "No alarm set");
        NotificationCompat.Builder nb = null;
        nb = notificationHelper.getChannelNotification();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        if(sharedPreferences.getBoolean("setRestricted", false))
        {
            if (desiredBursts > elapsedBursts) {
                if (elapsedBursts + 1 >= desiredBursts) {
                    timeText = "No alarm set";
                    timeTextNoDate = "No alarm set";
                    editor.putString("timeText", timeText);
                    editor.putString("timeTextNoDate", timeTextNoDate);
                    AlarmManager alarmManager = ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
                    startService(timeTextNoDate, context);
                    alarmManager.cancel(pendingIntent);
                } else {
                    double delay = Double.parseDouble(sharedPreferences.getString("setDelay", ""));
                    timeText=updateTimeText(delay);
                    timeTextNoDate=updateTimeTextNoDate(delay);
                    editor.putString("timeTextNoDate", timeTextNoDate);
                    editor.putString("timeText", timeText);

                    remainingBursts--;
                    timeTextNoDate=updateTimeTextNoDate(delay) + "  Remaining bursts: " + remainingBursts;
                    startService(timeTextNoDate, context);
                }
                editor.putInt("elapsedBursts", elapsedBursts + 1);
                editor.apply();
            }
        }else{
            double delay = Double.parseDouble(sharedPreferences.getString("setDelay", ""));
            timeText=updateTimeText(delay);
            timeTextNoDate=updateTimeTextNoDate(delay);
            editor.putString("timeText", timeText);
            editor.putString("timeTextNoDate", timeTextNoDate);
            editor.apply();
            startService(timeTextNoDate, context);
        }


        if (MainActivity.isActive()) {
            MainActivity.changeText(timeText);
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isScreenOn();
        if (!isScreenOn) {
            @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "MyLock");
            wl.acquire(1000);
            @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyCpuLock");

            wl_cpu.acquire(1000);
        }
        //play actual alarmsound here either with soundpool or mediaplayer
        notificationHelper.getManager().notify(1, nb.build());
    }

    public void startService(String input, Context context) {
        if(!input.equals("No alarm set")) {
            Intent serviceIntent = new Intent(context, ForegroundNotification.class);
            serviceIntent.putExtra("inputExtra", input);
            serviceIntent.putExtra("inputType", "Alarm triggered!");
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }



    private String updateTimeText(double tmpDelay) {
        String timeText = "Next alarm set for: \n\n";
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
        } else {
            timeText += hours + ":" + minutes + "\n" + "Date: " + formattedDate+"\n";
        }
        return timeText;
    }

    private String updateTimeTextNoDate(double tmpDelay) {
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
            timeTextNoDate += hours + ":0" + minutes;
        } else {
            timeTextNoDate += hours + ":" + minutes;
        }
        return timeTextNoDate;
    }

}
