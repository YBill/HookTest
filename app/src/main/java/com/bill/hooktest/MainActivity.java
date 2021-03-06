package com.bill.hooktest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void handleClickListener(View view) {
        Intent intent = new Intent(this, HookClickListenerAct.class);
        startActivity(intent);
    }

    public void handleNotification(View view) {
        Intent intent = new Intent(this, HookNotificationAct.class);
        startActivity(intent);
    }

    public void handleStartActivity(View view) {
        Intent intent = new Intent(this, HookStartActivityAct.class);
        startActivity(intent);
    }

    public void handleH(View view) {
        Intent intent = new Intent(this, HookHActivity.class);
        startActivity(intent);
    }
}