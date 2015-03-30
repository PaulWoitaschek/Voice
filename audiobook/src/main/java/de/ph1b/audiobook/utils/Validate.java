package de.ph1b.audiobook.utils;

import java.util.List;

public class Validate {

    public static void notEmpty(String... args) {
        notNull(args);
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

    public static void notEmpty(List... args) {
        notNull(args);
        for (int i = 0; i < args.length; i++) {
            if (args[i].size() == 0) {
                throw new IllegalArgumentException("Argument #" + i + " must not be empty");
            }
        }
    }
}
