package com.example.perpetualalarm;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;


public class NotificationHelper extends ContextWrapper {
    public static final String channelID1 = "channelID1";
    public static final String channelName1 = "Channel Name1";
    public static final String channelID2 = "channelID2";
    public static final String channelName2 = "Channel Name2";
    public static final String channelID3 = "channelID3";
    public static final String channelName3 = "Channel Name3";
    public static final String channelID4 = "channelID4";
    public static final String channelName4 = "Channel Name4";
    public int repeat;

    private NotificationManager mManager;

    public NotificationHelper(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel channel1 = new NotificationChannel(channelID1, channelName1, NotificationManager.IMPORTANCE_HIGH);
                channel1.enableVibration(true);
                channel1.setVibrationPattern(new long[] {0, 1000});

        NotificationChannel channel2 = new NotificationChannel(channelID2, channelName2, NotificationManager.IMPORTANCE_HIGH);
                channel2.enableVibration(true);
                channel2.setVibrationPattern(new long[] {0, 400, 600, 400});

        NotificationChannel channel3 = new NotificationChannel(channelID3, channelName3, NotificationManager.IMPORTANCE_HIGH);
                channel3.enableVibration(true);
                channel3.setVibrationPattern(new long[] {0, 400, 400, 500, 500, 600});

        NotificationChannel channel4 = new NotificationChannel(channelID4, channelName4, NotificationManager.IMPORTANCE_HIGH);
                channel4.enableVibration(true);
                channel4.setVibrationPattern(new long[] {0, 700, 800, 700, 800, 700});

        getManager().createNotificationChannel(channel1);
        getManager().createNotificationChannel(channel2);
        getManager().createNotificationChannel(channel3);
        getManager().createNotificationChannel(channel4);
    }

    public NotificationManager getManager() {
        if (mManager == null) {
            mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        return mManager;
    }


    public NotificationCompat.Builder getChannelNotification() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int typeOfAlarm=sharedPreferences.getInt("typeOfAlarm", 1);
        long[] vibration;
        String channelID;
        switch(typeOfAlarm){
            case 2:
                vibration = new long[] {0, 400, 600, 400};
                channelID=channelID2;
                break;
            case 3:
                vibration = new long[] {0, 400, 400, 500, 500, 600};
                channelID=channelID3;
                break;
            case 4:
                vibration= new long[] {0, 700, 800, 700, 800, 700};
                channelID=channelID4;
                break;
            default:
                vibration = new long[] {0, 1000};
                channelID=channelID1;
                break;
        }
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent=PendingIntent.getActivity(this,1,resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(getApplicationContext(), channelID)
                .setContentTitle("Alarm cycle ended")
                .setVibrate(vibration)
                .setSmallIcon(R.drawable.ic_android)
                .setAutoCancel(true)
                .setContentIntent(resultPendingIntent);
    }
}