package com.simonguest.QHotspot;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.RemoteViews;

import java.lang.reflect.Method;

public class MainWidgetProvider extends AppWidgetProvider {

    public static String ACTION_BUTTON_CLICK = "com.simonguest.QHotspot.MainWidgetProvider.ACTION_BUTTON_CLICK";
    private static Boolean HOTSPOT_ACTIVE = false;
    private static final String LOG_CLASS = "QHotspot";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        super.onUpdate(context, appWidgetManager, appWidgetIds);

        // Need to discover whether hotspot is enabled or not
        if (getWifiHotspotEnabled(context))
        {
            HOTSPOT_ACTIVE = true;
        }

        // bind the pending intent
        ComponentName thisWidget = new ComponentName(context, MainWidgetProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {

            Intent active = new Intent(context, MainWidgetProvider.class);
            active.setAction(ACTION_BUTTON_CLICK);
            PendingIntent activePendingIntent = PendingIntent.getBroadcast(context, 0, active, PendingIntent.FLAG_UPDATE_CURRENT);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.main);
            remoteViews.setOnClickPendingIntent(R.id.wifiToggleButton, activePendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        // update the icon
        updateWidgetIcon(context);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);
        if (intent.getAction() == ACTION_BUTTON_CLICK)
        {
            try
            {
                 // Set the hotspot state
                HOTSPOT_ACTIVE = !HOTSPOT_ACTIVE;

                // update the icon
                updateWidgetIcon(context);

                // try to change the status of the hotspot
                try
                {
                    setWifiHotspotEnabled(context, HOTSPOT_ACTIVE);
                }
                catch (Exception ex)
                {
                    // isolate this exception so that the UI has chance to recover
                    Log.d(LOG_CLASS, ex.toString());
                }

                if (HOTSPOT_ACTIVE == true)
                {
                    Thread.sleep(1500);
                    if (!getWifiHotspotEnabled(context))
                    {
                        // Hotspot didn't notify of positive change - revert back
                        HOTSPOT_ACTIVE = false;
                        updateWidgetIcon(context);
                    }
                }
            }
            catch (Exception ex)
            {
                Log.d(LOG_CLASS, ex.toString());
            }
        }
    }

    private void updateWidgetIcon(Context context)
    {
        try
        {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.main);
            remoteViews.setTextViewText(R.id.wifiText, (HOTSPOT_ACTIVE ? "On" : "Off"));
            remoteViews.setImageViewResource(R.id.wifiToggleButton, (HOTSPOT_ACTIVE ? R.drawable.wifi_light : R.drawable.wifi_dark));
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, MainWidgetProvider.class);
            manager.updateAppWidget(thisWidget, remoteViews);
        }
        catch (Exception ex)
        {
            Log.d(LOG_CLASS, ex.toString());
        }
    }

    private void setWifiHotspotEnabled(Context context, boolean enable) {

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        for (Method method : wifiManager.getClass().getDeclaredMethods()) {
            if (method.getName().equals("setWifiApEnabled")) {
                try
                {
                    method.invoke(wifiManager, null, enable);
                }
                catch (Exception ex)
                {
                    Log.d(LOG_CLASS, ex.toString());
                }
                break;
            }
        }
    }

    private Boolean getWifiHotspotEnabled(Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        for(Method method: wifiManager.getClass().getDeclaredMethods()){
            if(method.getName().equals("isWifiApEnabled")) {
                try {
                    Boolean result = (Boolean)method.invoke(wifiManager);
                    return result;
                } catch (Exception ex)
                {
                    Log.d(LOG_CLASS, ex.toString());
                }
            }
        }

        // No idea what the status is!
        return false;
    }


}
