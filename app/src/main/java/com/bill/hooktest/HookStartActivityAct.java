package com.bill.hooktest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Hook StartActivity
 */
public class HookStartActivityAct extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hook_start);
        hookStartActivityByActivity();
        hookStartActivityByContext();
    }

    public void handleStartActivityByContext(View view) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("tag", "HookStartActivity By Context");
        getApplicationContext().startActivity(intent);
    }

    public void handleStartActivityByActivity(View view) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("tag", "HookStartActivity By Activity");
        this.startActivity(intent);
    }

    // Hook 通过 Activity 启动的 Activity
    private void hookStartActivityByActivity() {
        try {
            // 获取当前 Activity 对象
            Class<?> currentActivityClz = Class.forName("android.app.Activity");

            // 获取 Activity 对象的 mInstrumentation 参数
            Field mInstrumentationField = currentActivityClz.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation mInstrumentation = (Instrumentation) mInstrumentationField.get(this);

            // 创建代理对象
            Instrumentation proxyInstrumentation = new HookedInstrumentation(mInstrumentation);

            // 替换掉 Activity 对象的 mInstrumentation 变量
            mInstrumentationField.set(this, proxyInstrumentation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hook 通过 Context 启动的 Activity
    private void hookStartActivityByContext() {
        try {
            // 获取到当前的 ActivityThread 对象
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            // 获取 ActivityThread 类的 currentActivityThread() 静态方法
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            // 获取到 ActivityThread 的实例对象
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);

            // 获取原始的 ActivityThread 中的 mInstrumentation 成员变量
            Field mInstrumentationField = activityThreadClass.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation mInstrumentation = (Instrumentation) mInstrumentationField.get(currentActivityThread);

            // 创建代理对象
            Instrumentation proxyInstrumentation = new HookedInstrumentation(mInstrumentation);

            // 替换掉 ActivityThread 对象的 mInstrumentation 变量
            mInstrumentationField.set(currentActivityThread, proxyInstrumentation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class HookedInstrumentation extends Instrumentation {

        // ActivityThread 中原始 Instrumentation 变量： mInstrumentation
        Instrumentation mOriginInstrumentation;

        public HookedInstrumentation(Instrumentation originInstrumentation) {
            mOriginInstrumentation = originInstrumentation;
        }

        // 重写 Instrumentation 的 execStartActivity 方法（调用startActivity最终会执行execStartActivity方法）
        // 注意 @Override 不支持，因为是系统隐藏方法，不允许重载的
        public ActivityResult execStartActivity(
                Context who, IBinder contextThread, IBinder token, Activity target,
                Intent intent, int requestCode, Bundle options) {

            Log.i("Bill", "Before StartActivity");

            Log.d("Bill", "\n执行了startActivity, 参数如下: \n" + "who = [" + who + "], " +
                    "\ncontextThread = [" + contextThread + "], \ntoken = [" + token + "], " +
                    "\ntarget = [" + target + "], \nintent = [" + intent +
                    "], \nrequestCode = [" + requestCode + "], \noptions = [" + options + "]");

            try {
                // 换掉之前的 tag 参数
                String originTag = intent.getStringExtra("tag");
                String modifyTag = originTag + "\nSuccess";
                Log.d("Bill", "之前的tag参数为：" + originTag + "，修改后的tag参数为：" + modifyTag);
                intent.putExtra("tag", modifyTag);

                // 开始调用原始的 execStartActivity 方法,由于这个方法是隐藏的,因此需要使用反射调用;首先找到这个方法
                // 如果不调用则 startActivity 就不会跳转了
                Method execStartActivity = Instrumentation.class.getDeclaredMethod(
                        "execStartActivity",
                        Context.class, IBinder.class, IBinder.class, Activity.class,
                        Intent.class, int.class, Bundle.class);
                execStartActivity.setAccessible(true);
                return (ActivityResult) execStartActivity.invoke(mOriginInstrumentation, who,
                        contextThread, token, target, intent, requestCode, options);
            } catch (Exception e) {
                // 可能是厂商修改了源码，需要查源码单独适配
                Log.e("Bill", "Hook Instrumentation Failed!", e);
                throw new RuntimeException("do not support!!! please adapt it");
            } finally {
                Log.i("Bill", "After StartActivity");
            }
        }
    }
}