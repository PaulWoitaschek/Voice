package de.paul_woitaschek.audiobook.utils;

import android.support.annotation.NonNull;

import java.util.List;

public class Validate {

    public static void notEmpty(@NonNull String... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("")) {
                throw new IllegalArgumentException("Argument #" + i + " must not be empty");
            }
        }
    }

    public static void notNull(Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                throw new NullPointerException("Argument #" + i + " must not be null");
            }
        }
    }

    public static void notEmpty(@NonNull List... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].size() == 0) {
                throw new IllegalArgumentException("Argument #" + i + " must not be empty");
            }
        }
    }
}
