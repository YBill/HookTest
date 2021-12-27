package com.bill.hooktest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Hook NotificationManager
 */
public class HookNotificationAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hookNotificationManager(getApplicationContext());
        setContentView(R.layout.activity_hook_notification);
        createNotificationChannel();
    }

    private void hookNotificationManager(Context context) {
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            // 获取 NotificationManager 中的 getService() 方法 (静态的直接获取)
            Method getService = NotificationManager.class.getDeclaredMethod("getService");
            getService.setAccessible(true);
            // 得到 INotificationManager sService
            final Object sService = getService.invoke(notificationManager);

            Class INotificationManagerClz = Class.forName("android.app.INotificationManager");
            // 动态代理 INotificationManager
            Object proxyNotificationManager = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{INotificationManagerClz},
                    new InvocationHandler() {

                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            Log.i("Bill", "============== Start ==============");
                            // 打印方法名
                            Log.d("Bill", "invoke method：" + method.getName());
                            // 打印参数类型
                            if (args != null && args.length > 0) {
                                for (int i = 0; i < args.length; i++) {
                                    Object arg = args[i];
                                    Log.d("Bill", "parameter" + (i + 1) + "：" + (arg != null ? arg.getClass() : null));
                                }
                            }
                            Log.i("Bill", "============== End ==============");

                            // 操作交由 sService 处理（即不拦截通知）
//                            return method.invoke(sService, args);

                            // 什么也不做（即拦截通知）
//                            return null;

                            // 可以根据方法名、id、tag 等进行具体拦截
                            if ("enqueueNotificationWithTag".equals(method.getName())) {
                                if (args != null && args.length == 6) {
                                    Object parameterId = args[3]; // 第4个参数是id
                                    int id = (int) parameterId;
                                    // 将 id 为 1 的通知拦截
                                    if (id == 1) {
                                        Toast.makeText(getApplicationContext(), "通知被拦截", Toast.LENGTH_SHORT).show();
                                        Log.e("Bill", "[id 为 1，拦截通知]");
                                        return null;
                                    } else {
                                        Log.e("Bill", "[id 为 " + id + "，不拦截通知]");
                                    }
                                }
                            }
                            return method.invoke(sService, args);

                        }
                    });
            // 替换 sService
            Field sServiceField = NotificationManager.class.getDeclaredField("sService");
            sServiceField.setAccessible(true);
            sServiceField.set(notificationManager, proxyNotificationManager);
        } catch (Exception e) {
            Log.e("Bill", "Hook NotificationManager Failed!", e);
        }
    }

    // 创建渠道
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "channel_chat";
            String channelName = "新聊天消息";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 发送通知
    private void showNotification(int id) {
        String channelId = "channel_chat";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("title")
                .setContentText("content")
                .build();
        notificationManager.notify(id, notification);
    }

    public void handleIntercept(View view) {
        showNotification(1);
    }

    private int notifyId = 1;

    public void handleNotIntercept(View view) {
        notifyId++;
        showNotification(notifyId);
    }
}