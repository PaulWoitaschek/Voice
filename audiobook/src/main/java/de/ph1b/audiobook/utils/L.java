package de.ph1b.audiobook.utils;


import android.util.Log;

import de.ph1b.audiobook.BuildConfig;

@SuppressWarnings("SameParameterValue")
public class L {

    public static void d(String tag, Object msg, Exception e) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, getMessage(msg), e);
        }
    }

    public static void d(String tag, Object msg) {
        Log.d(tag, getMessage(msg));
    }

    public static void e(String tag, Object msg) {
        Log.e(tag, getMessage(msg));
    }

    public static void e(String tag, Object msg, Throwable tr) {
        Log.e(tag, getMessage(msg), tr);
    }

    public static void i(String tag, Object msg) {
        Log.i(tag, getMessage(msg));
    }

    public static void v(String tag, Object msg) {
        Log.v(tag, getMessage(msg));
    }

    private static String getMessage(Object msg) {
        if (msg == null) {
            return "null";
        } else if (msg.equals("")) {
            return "empty";
        } else {
            return String.valueOf(msg);
        }
    }
}
