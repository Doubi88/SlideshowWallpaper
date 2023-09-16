package io.github.doubi88.slideshowwallpaper.utilities;

public abstract class CompatibilityHelpers {

    public static int getNextAvailableSecondsEntry(int seconds, String[] entries) {
        int result = Integer.parseInt(entries[0]);
        int distance = Math.abs(result - seconds);
        for (int i = 1; i < entries.length; i++) {
            int entry = Integer.parseInt(entries[i]);
            if (Math.abs(entry - seconds) < distance) {
                result = entry;
                distance = Math.abs(result - seconds);
            }
            else {
                break; // If the distance gets larger, it won't get smaller again, so break here.
            }
        }
        return result;
    }

}
