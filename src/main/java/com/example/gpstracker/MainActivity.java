package com.example.gpstracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    protected PowerManager.WakeLock mWakeLock;

    final Handler myHandler = new Handler();

    // GUI
    TextView inMotion;
    TextView inMotionSeconds;
    TextView checkInMotion;
    TextView checkInMotionSeconds;
    TextView airModeLbl;
    TextView gpsEnabledLbl;

    /**
     * Toggle screen backlight
     *
     * @param value
     */
    public void toggleBackLight(Boolean value) {
        if (value) {
            /* This code together with the one in onDestroy()
             * will make the screen be always on until this Activity gets destroyed. */
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
            this.mWakeLock.acquire();
        } else {
            if (mWakeLock != null) {
                this.mWakeLock.release();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity x = this;
        setContentView(R.layout.activity_main);

        // Disable backlight always on
        toggleBackLight(false);

        // Backlight
        ToggleButton toggle = (ToggleButton) findViewById(R.id.tBtnNetwork);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleBackLight(isChecked);
            }
        });

        // Alarm
        final AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, GpsSenderAlarm.class);
        Intent intent1 = new Intent(this, MotionSensorAlarm.class);

        final PendingIntent pintent = PendingIntent.getBroadcast(x, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        final PendingIntent pintent1 = PendingIntent.getBroadcast(x, 0, intent1, PendingIntent.FLAG_CANCEL_CURRENT);

        // Tracker
        ToggleButton tBtnService = (ToggleButton) findViewById(R.id.tBtnService);
        tBtnService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Calendar cal = Calendar.getInstance();

                    alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                            GpsSenderAlarm.ALARM_LOOP*60*1000, pintent);

                    alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                            MotionSensorAlarm.ALARM_LOOP*60*1000, pintent1);

                    Log.i("Tracker Toggle", "ON");
                } else {
                    alarm.cancel(pintent);
                    alarm.cancel(pintent1);

                    Log.i("Tracker Toggle", "OFF");
                }
            }
        });

        // Battery Save Mode
        ToggleButton tBtnBattery = (ToggleButton) findViewById(R.id.tBtnBattery);
        tBtnBattery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAirMode(x, isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        toggleBackLight(false);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public void setAirMode(Context context, boolean isEnabled) {
        // Toggle airplane mode.
        Settings.System.putInt(
                context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, isEnabled ? 1 : 0);

        // Post an intent to reload.
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", !isEnabled);
        sendBroadcast(intent);
    }
}
