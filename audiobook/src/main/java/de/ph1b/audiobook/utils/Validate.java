package de.ph1b.audiobook.utils;

public class Validate {

    public static void notNull(Object... args) {

        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                throw new NullPointerException("Argument #" + i + " must not be null");
            }
        }
    }
}
