package com.example.perpetualalarm;


import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.Calendar;
import java.util.Date;


public class Settings extends AppCompatActivity {
    private EditText textDelayHours;
    String delayHours;
    String delayMinutes;
    private EditText textDelayMinutes;
    private EditText textAmountOfAlerts;
    private EditText textLastHour;
    private SwitchCompat hourSwitch;
    private SwitchCompat amountSwitch;
    private TimePickerDialog timePickerDialog;
    private boolean changedByUserHour;
    private boolean changedByUserAmount;
    private boolean changedByFocusChange;
    Calendar c;

    @Override
    protected void onStop() {
        super.onStop();

        reformatTime();
        String amount = textAmountOfAlerts.getText().toString();
        String lastHour = textLastHour.getText().toString();
        if (amount.equals("")) {
            amountSwitch.setChecked(false);
        }else{
            amountSwitch.setChecked(true);
        }
        boolean hourSwitchState = hourSwitch.isChecked();
        boolean amountSwitchState = amountSwitch.isChecked();
        changedByUserHour = false;
        changedByUserAmount = false;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("delayHours", delayHours);
        editor.putString("delayMinutes", delayMinutes);
        editor.putString("amount", amount);
        editor.putString("lastHour", lastHour);
        editor.putBoolean("hourSwitchState", hourSwitchState);
        editor.putBoolean("amountSwitchState", amountSwitchState);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        textDelayHours = findViewById(R.id.delayTextHours);
        textDelayMinutes = findViewById(R.id.delayTextMinutes);


        textDelayMinutes.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    reformatTime();
                }
            }
        });

        textAmountOfAlerts = findViewById(R.id.repeatsText);
        amountSwitch = findViewById(R.id.switch_number);


        textAmountOfAlerts.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {

                    String amountInput = textAmountOfAlerts.getText().toString();
                    int amount;
                    if(amountInput.equals("")){
                        amount=0;
                    }else{
                        amount=Integer.parseInt(amountInput);
                    }


                    if (amount==0 && amountSwitch.isChecked()) {
                        amountSwitch.setChecked(false);
                        textAmountOfAlerts.setText("");
                        changedByFocusChange=false;
                    } else if(amount==0 && !amountSwitch.isChecked()) {
                        textAmountOfAlerts.setText("");
                    } else{
                        changedByFocusChange=true;
                        amountSwitch.setChecked(true);
                    }

                }
            }
        });





        amountSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (changedByUserAmount) {
                        if(!changedByFocusChange){
                            if (!textAmountOfAlerts.isFocused()) {
                                textAmountOfAlerts.requestFocus();
                                textAmountOfAlerts.setFocusableInTouchMode(true);
                                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.showSoftInput(textAmountOfAlerts, InputMethodManager.SHOW_FORCED);
                                changedByFocusChange=false;
                            }
                        }
                    } else {
                        changedByUserAmount = true;
                        changedByFocusChange=false;
                    }

                } else {
                    textAmountOfAlerts.setText("");
                    changedByUserAmount = true;
                    changedByFocusChange=false;
                    if (textAmountOfAlerts.isFocused()) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                        View current = getCurrentFocus();
                        if (current != null) current.clearFocus();
                    }

                }
            }
        });

        textLastHour = findViewById(R.id.finalHourText);
        textLastHour.setFocusable(false);

        c = Calendar.getInstance();
        timePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                textLastHour = findViewById(R.id.finalHourText);
                if (minute > 9) {
                    textLastHour.setText(hourOfDay + ":" + minute);
                } else {
                    textLastHour.setText(hourOfDay + ":0" + minute);
                }

            }
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);

        timePickerDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Toast.makeText(Settings.this, "Time not picked!", Toast.LENGTH_LONG).show();
                hourSwitch.setChecked(false);
            }
        });


        hourSwitch = findViewById(R.id.switch_hour);


        hourSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    if (changedByUserHour) {
                        Date date = new Date();
                        c.setTime(date);
                        timePickerDialog.show();
                    } else {
                        changedByUserHour = true;
                    }
                } else {
                    textLastHour.setText("");
                    changedByUserHour = true;
                }


            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        textDelayHours.setText(sharedPreferences.getString("delayHours", ""));
        textDelayHours.setSelection(textDelayHours.getText().length());
        textDelayMinutes.setText(sharedPreferences.getString("delayMinutes", ""));
        textDelayMinutes.setSelection(textDelayMinutes.getText().length());
        textAmountOfAlerts.setText(sharedPreferences.getString("amount", ""));
        textAmountOfAlerts.setSelection(textAmountOfAlerts.getText().length());
        textLastHour.setText(sharedPreferences.getString("lastHour", ""));
        amountSwitch.setChecked(sharedPreferences.getBoolean("amountSwitchState", false));
        hourSwitch.setChecked(sharedPreferences.getBoolean("hourSwitchState", false));
        changedByUserHour = !hourSwitch.isChecked();
        changedByUserAmount = !amountSwitch.isChecked();

    }




    @Override
    protected void onResume() {
        super.onResume();
        changedByUserHour = !hourSwitch.isChecked();
        changedByUserAmount = !amountSwitch.isChecked();
    }

    @Override
    protected void onStart() {
        super.onStart();
        changedByUserHour = !hourSwitch.isChecked();
        changedByUserAmount = !amountSwitch.isChecked();
    }

    private void reformatTime() {
        int hoursTemp = 0;
        int minutesTemp = 0;
        delayHours = textDelayHours.getText().toString();
        delayMinutes = textDelayMinutes.getText().toString();
        if (!delayHours.equals("")) {
            hoursTemp = Integer.parseInt(delayHours);
        }
        if (!delayMinutes.equals("")) {
            minutesTemp = Integer.parseInt(delayMinutes);
        }

        while (minutesTemp > 59) {
            hoursTemp++;
            minutesTemp -= 60;
        }
        if (minutesTemp > 9) {
            delayMinutes = String.valueOf(minutesTemp);
        } else if (minutesTemp == 0) {
            delayMinutes = "";
        } else {
            delayMinutes = "0" + String.valueOf(minutesTemp);
        }
        if (hoursTemp == 0) {
            delayHours = "";
        } else {
            delayHours = String.valueOf(hoursTemp);
        }
        textDelayHours.setText(delayHours);
        textDelayMinutes.setText(delayMinutes);
    }


}
