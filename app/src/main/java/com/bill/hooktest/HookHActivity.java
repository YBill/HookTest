package com.bill.hooktest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class HookHActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityThreadCallback mActivityThreadCallback = new ActivityThreadCallback();
        mActivityThreadCallback.hook();

        setContentView(R.layout.activity_hook_hactivity);
        createNotificationChannel();

    }

    // 创建渠道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_test_bad_notification";
            String channelName = "测试 Bad Notification";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 发送通知
    private void showNotification(int id) {
        String channelId = "channel_test_bad_notification";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.remote_views_layout);
        remoteViews.setImageViewResource(R.id.tv_title, -1);
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContent(remoteViews)
                .build();
        notificationManager.notify(id, notification);
    }

    public void handleSendNotify(View view) {
        showNotification(100);
    }
}