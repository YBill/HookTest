package com.bill.hooktest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Hook ClickListener
 */
public class HookClickListenerAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_click_listener);

        AppCompatButton button = findViewById(R.id.btn_click_listener);
        View view = findViewById(R.id.view_click_listener);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Bill", "Button Click");
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Bill", "View Click");
            }
        });

        hookOnClickListener(button);
        hookOnClickListener(view);

    }

    private void hookOnClickListener(View view) {
        try {
            // 获取 View 的 getListenerInfo() 方法
            Method getListenerInfo = View.class.getDeclaredMethod("getListenerInfo");
            getListenerInfo.setAccessible(true);
            // 调用 View 的 getListenerInfo() 方法 得到 ListenerInfo 对象
            Object listenerInfo = getListenerInfo.invoke(view);
            // 获取 View 的内部类 ListenerInfo 类
            Class<?> listenerInfoClz = Class.forName("android.view.View$ListenerInfo");
            // 获取 ListenerInfo 类的属性 mOnClickListener
            Field mOnClickListener = listenerInfoClz.getDeclaredField("mOnClickListener");
            mOnClickListener.setAccessible(true);
            // 获取 mOnClickListener 的值（即原始设置setOnClickListener的OnClickListener）
            View.OnClickListener originOnClickListener = (View.OnClickListener) mOnClickListener.get(listenerInfo);
            // 用自定义的 HookedOnClickListener 替换原始的 OnClickListener
            View.OnClickListener proxyOnClickListener = new HookedOnClickListener(originOnClickListener);
            // 重新为 ListenerInfo 的 mOnClickListener 字段赋值为我们的代理 proxyOnClickListener
            mOnClickListener.set(listenerInfo, proxyOnClickListener);
        } catch (Exception e) {
            Log.e("Bill", "Hook ClickListener Failed!", e);
        }
    }

    private class HookedOnClickListener implements View.OnClickListener {

        // 原始的 OnClickListener
        private final View.OnClickListener mOriginClickListener;

        HookedOnClickListener(View.OnClickListener originClickListener) {
            this.mOriginClickListener = originClickListener;
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "Hook Click", Toast.LENGTH_SHORT).show();
            Log.i("Bill", "Before Click");

            if (mOriginClickListener != null) {
                mOriginClickListener.onClick(v);
            }

            Log.i("Bill", "After Click");
        }
    }


}