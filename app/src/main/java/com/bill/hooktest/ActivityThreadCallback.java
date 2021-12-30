package com.bill.hooktest;

import android.os.Build;
import android.os.DeadSystemException;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * author ywb
 * date 2021/12/30
 * desc
 */
public class ActivityThreadCallback implements Handler.Callback {

    private static final String TAG = "Bill";

    public final Set<String> mIgnorePackages;
    private static final String EXCEPTION_IS_TOP_OF_TASK = "com.android.server.am.ActivityManagerService.isTopOfTask";
    private static final String EXCEPTION_IS_TOP_OF_TASK2 = "android.app.IActivityManager$Stub$Proxy.isTopOfTask";
    private static final String EXCEPTION_IS_TOP_OF_TASK3 = "android.app.ActivityManagerProxy.isTopOfTask";
    private static final String[] SYSTEM_PACKAGE_PREFIXES = {"java.", "android.", "androidx.", "dalvik.", "com.android."};
    public Handler mHandler;

    public ActivityThreadCallback() {
        HashSet hashSet = new HashSet<>(Arrays.asList(SYSTEM_PACKAGE_PREFIXES));
        this.mIgnorePackages = Collections.unmodifiableSet(hashSet);
        mHandler = getHandler(getActivityThread());
    }

    public static Object getActivityThread() {
        Object activityThread = null;
        try {
            activityThread = getFieldValue(Class.forName("android.app.ActivityThread"), "sCurrentActivityThread");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (activityThread != null) {
            Log.e(TAG, "ActivityThread instance is inaccessible");
            return activityThread;
        }
        Log.e(TAG, "Hook ActivityThread fail");
        return null;
    }

    public static Handler getHandler(Object activityThread) {
        if (activityThread == null) {
            return null;
        }
        Handler mH = (Handler) getFieldValue(activityThread, "mH");
        if (mH != null) {
            Log.e(TAG, "Handler is mH ");
            return mH;
        }

        Class<?>[] clsArr = new Class[0];
        Object[] objArr = new Object[0];
        Method method = getMethod(activityThread.getClass(), "getHandler", clsArr);
        Handler getHandler = null;
        if (method != null) {
            method.setAccessible(true);
            try {
                getHandler = (Handler) method.invoke(activityThread, objArr);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            if (getHandler != null) {
                Log.e(TAG, "Handler is getHandler ");
                return getHandler;
            }
        }


        Handler h = (Handler) getFieldValue(activityThread, "android.app.ActivityThread$H");
        if (h != null) {
            Log.e(TAG, "Handler is H ");
            return h;
        }

        Log.e(TAG, "Hook Handler Fail !!!");
        return null;
    }

    public boolean hook() {
        if (mHandler != null) {
            try {
                Field mCallback = getField(mHandler.getClass(), "mCallback");
                if (mCallback != null) {
                    mCallback.setAccessible(true);
                    mCallback.set(mHandler, this);
                    Log.e(TAG, "Hook mCallback success ");
                    return true;
                }
            } catch (Throwable th) {
                Log.e(TAG, "set field mCallback of " + mHandler + " error");
            }
        }
        return false;
    }

    @Override
    public final boolean handleMessage(Message message) {
        Log.e(TAG, "#handleMessage " + message);

        try {
            if (mHandler != null) {
                mHandler.handleMessage(message);
            }
        } catch (Exception e) {
            Log.e(TAG, "#handleMessage error： " + e.getLocalizedMessage());
            // TODO
            /*Throwable cause = e.getCause();
            if (isCausedBy(cause, IllegalArgumentException.class, RemoteException.class) && hasStackTraceElement(e, EXCEPTION_IS_TOP_OF_TASK, EXCEPTION_IS_TOP_OF_TASK2, EXCEPTION_IS_TOP_OF_TASK3)) {
                return true;
            } else if (isCausedBy(cause, WindowManager.BadTokenException.class)) {
                return true;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isCausedBy(cause, DeadSystemException.class)) {
                return true;
            } else {
                //由于业务问题导致的崩溃，非系统崩溃，继续抛除异常
                throw e;
            }*/
        }
        return true;
    }

    @SafeVarargs
    private static boolean isCausedBy(Throwable th, Class<? extends Throwable>... clsArr) {
        return isCausedBy(th, new HashSet<>(Arrays.asList(clsArr)));
    }

    private static boolean isCausedBy(Throwable th, Set<Class<? extends Throwable>> set) {
        if (th == null) {
            return false;
        }
        if (set.contains(th.getClass())) {
            return true;
        }
        return isCausedBy(th.getCause(), set);
    }

    private boolean isCausedByUser(Throwable th) {
        if (th == null) {
            return false;
        }
        while (th != null) {
            for (StackTraceElement stackTraceElement : th.getStackTrace()) {
                if (isUserStackTrace(stackTraceElement)) {
                    return true;
                }
            }
            th = th.getCause();
        }
        return false;
    }

    private static boolean hasStackTraceElement(Throwable th, String... strArr) {
        return hasStackTraceElement(th, new HashSet(Arrays.asList(strArr)));
    }

    private static boolean hasStackTraceElement(Throwable th, Set<String> set) {
        if (th == null || set == null || set.isEmpty()) {
            return false;
        }
        StackTraceElement[] stackTrace = th.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTrace) {
            if (set.contains(stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName())) {
                return true;
            }
        }
        return hasStackTraceElement(th.getCause(), set);
    }

    private boolean isUserStackTrace(StackTraceElement stackTraceElement) {
        String className = stackTraceElement.getClassName();
        for (String str : this.mIgnorePackages) {
            if (className.startsWith(str)) {
                return false;
            }
        }
        return true;
    }

    private void rethrowIfCausedByUser(RuntimeException runtimeException) {
        if (isCausedByUser(runtimeException)) {
            throw runtimeException;
        }
    }

    public static <T> T getFieldValue(Object obj, String str) {
        if (obj == null) {
            return null;
        }
        try {
            Field a = null;
            if (obj instanceof Class) {
                a = getField((Class<?>) obj, str);
            } else {
                a = getField(obj.getClass(), str);
            }
            if (a == null) {
                return null;
            }
            a.setAccessible(true);
            return (T) a.get(obj);
        } catch (Throwable th) {
            Log.e(TAG, "get field " + str + " of " + obj + " error");
            return null;
        }
    }

    public static Field getField(Class<?> cls, String str) {
        try {
            return cls.getDeclaredField(str);
        } catch (NoSuchFieldException unused) {
            Class<?> superclass = cls.getSuperclass();
            if (superclass == null) {
                Log.e(TAG, "get field fail with:" + str);
                return null;
            }
            return getField(superclass, str);
        }
    }

    public static Method getMethod(Class<?> cls, String str, Class<?>[] clsArr) {
        try {
            return cls.getDeclaredMethod(str, clsArr);
        } catch (NoSuchMethodException unused) {
            Class<?> superclass = cls.getSuperclass();
            if (superclass == null) {
                return null;
            }
            return getMethod(superclass, str, clsArr);
        }
    }

}