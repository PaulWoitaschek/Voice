package de.ph1b.audiobook.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.ph1b.audiobook.BuildConfig;

@SuppressWarnings("SameParameterValue")
public class L {

    public static void d(String tag, Object msg) {
        if (BuildConfig.DEBUG) {
            for (String s : getMessage(msg)) {
                Log.d(tag, s);
            }
        }
    }

    public static void d(String tag, Object msg, Throwable t) {
        if (BuildConfig.DEBUG) {
            for (String s : getMessage(msg)) {
                Log.d(tag, s, t);
            }
        }
    }

    private static List<String> getMessage(Object msg) {
        List<String> split = new ArrayList<>();
        if (msg == null) {
            split.add("null");
        } else if (msg.equals("")) {
            split.add("empty");
            return split;
        } else {
            String fullMsg = String.valueOf(msg);
            for (int i = 0; i < fullMsg.length(); i += 4000) {
                split.add(fullMsg.substring(i, Math.min(fullMsg.length(), i + 4000)));
            }
            return split;
        }
        return split;
    }

    public static void e(String tag, Object msg) {
        if (BuildConfig.DEBUG) {
            for (String s : getMessage(msg)) {
                Log.e(tag, s);
            }
        }
    }

    public static void e(String tag, Object msg, Throwable tr) {
        if (BuildConfig.DEBUG) {
            for (String s : getMessage(msg)) {
                Log.e(tag, s, tr);
            }
        }
    }

    public static void i(String tag, Object msg) {
        if (BuildConfig.DEBUG) {
            for (String s : getMessage(msg)) {
                Log.i(tag, s);
            }
        }
    }

    public static void v(String tag, Object msg) {
        if (BuildConfig.DEBUG) {
            for (String s : getMessage(msg)) {
                Log.v(tag, s);
            }
        }
    }
}
