package de.ph1b.audiobook.utils;

public class ArgumentValidator {
    public static void validate(Object... args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) {
                throw new IllegalArgumentException("Argument #" + i + " must not be null");
            }
        }
    }
}
