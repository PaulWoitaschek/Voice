package de.ph1b.audiobook.model;

import de.ph1b.audiobook.utils.Validate;

public class Chapter {

    private static final String TAG = Chapter.class.getSimpleName();

    private final String path;
    private final int duration;
    private final String name;

    public Chapter(String path, String name, int duration) {
        Validate.notNull(path, name);
        this.path = path;
        this.name = name;
        this.duration = duration;
    }

    @Override
    public String toString() {
        return TAG + "[" +
                "path=" + path + ", " +
                "duration=" + duration + ", " +
                "name=" + name +
                "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof Chapter) {
            Chapter that = (Chapter) o;
            if (that.path.equals(this.path) && that.name.equals(this.name) && that.duration == this.duration) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + path.hashCode();
        result = PRIME * result + duration;
        result = PRIME * result + name.hashCode();
        return result;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public String getPath() {
        return path;
    }

}
