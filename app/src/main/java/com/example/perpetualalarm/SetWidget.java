package com.example.perpetualalarm;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class SetWidget extends AppWidgetProvider {
    private Double delay;
    private int howManyTimes;
    static String CLICK_ACTION = "CLICKED";




    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
           RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.set_widget);
           Intent intent = new Intent(context, DialogActivity.class);

            PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            remoteViews.setOnClickPendingIntent(R.id.widget, configPendingIntent);
            appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);

    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

}

