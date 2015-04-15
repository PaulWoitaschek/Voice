package de.ph1b.audiobook.utils;

import android.support.annotation.NonNull;

import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public class Validate {

    public Validate notEmpty(@NonNull String... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("")) {
                throw new IllegalArgumentException("Argument #" + i + " must not be empty");
            }
        }
        return this;
    }

    public Validate notNull(Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                throw new NullPointerException("Argument #" + i + " must not be null");
            }
        }
        return this;
    }

    public Validate notEmpty(@NonNull List... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].size() == 0) {
                throw new IllegalArgumentException("Argument #" + i + " must not be empty");
            }
        }
        return this;
    }
}
