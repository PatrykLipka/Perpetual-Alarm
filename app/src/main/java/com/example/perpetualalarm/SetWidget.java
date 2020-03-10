package com.example.perpetualalarm;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Implementation of App Widget functionality.
 */
public class SetWidget extends AppWidgetProvider {

    static String CLICK_ACTION= "CLICKED";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Intent intent = new Intent(context, SetWidget.class);
        intent.setAction(CLICK_ACTION);
        PendingIntent pendingIntent=PendingIntent.getBroadcast(context,0,intent,0);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.set_widget);
        views.setOnClickPendingIntent(R.id.layout_wrapper,pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if(intent.getAction().equals(CLICK_ACTION)){

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            double delay;
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

            int howManyTimes=0;
            if (sharedPreferences.getString("amount", "0").equals("")) {
                howManyTimes = 0;
            } else {
                howManyTimes = Integer.parseInt(sharedPreferences.getString("amount", "0"));
            }



          if (delay>0) {
                Double tmpDelay = delay;
                startAlarm(tmpDelay,howManyTimes, context);
                if(sharedPreferences.getInt("desiredBursts", 0)>0){
                    updateTimeText(tmpDelay, context);
                }
                else{
                    editor.putString("timeText", "No alarm set");
                }
            } else {
                Toast.makeText(context,"Set higher delay!", Toast.LENGTH_SHORT).show();
            }



        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private void startAlarm(Double delay, int howManyTimes, Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);


        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay.longValue(),
                delay.longValue(), pendingIntent);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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
            Toast.makeText(context,"Alarm set!", Toast.LENGTH_SHORT).show();
            editor.putBoolean("setRestricted", true);
        }else if(amountRestricted){
            editor.putInt("desiredBursts", howManyTimes);
            Toast.makeText(context,"Alarm set!", Toast.LENGTH_SHORT).show();
            editor.putBoolean("setRestricted", true);
        }else if(hourRestricted){
            editor.putInt("desiredBursts", howManyTimesFromHour);
            Toast.makeText(context,"Alarm set!", Toast.LENGTH_SHORT).show();
            editor.putBoolean("setRestricted", true);
        }else{
            editor.putInt("desiredBursts", 1);
            Toast.makeText(context,"Alarm set!", Toast.LENGTH_SHORT).show();
            editor.putBoolean("setRestricted", false);
        }
        editor.putInt("elapsedBursts", 0);
        editor.putString("setDelay", delay.toString());

        editor.apply();
    }

    private void updateTimeText(double tmpDelay, Context context) {
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

        timeText+=formattedDate+"\n";
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("timeText", timeText);
        editor.apply();
    }
}

