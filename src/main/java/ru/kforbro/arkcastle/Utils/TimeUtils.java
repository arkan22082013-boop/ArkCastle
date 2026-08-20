package ru.kforbro.arkcastle.Utils;

public class TimeUtils {
    public static String prettyTime(long totalSeconds) {
        long days = totalSeconds / 86400L;
        long hours = totalSeconds % 86400L / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        StringBuilder formattedTime = new StringBuilder();
        if (days > 0L) {
            formattedTime.append(days).append(" д.");
        }
        if (hours > 0L) {
            if (!formattedTime.isEmpty()) {
                formattedTime.append(" ");
            }
            formattedTime.append(hours).append(" ч.");
        }
        if (minutes > 0L) {
            if (!formattedTime.isEmpty()) {
                formattedTime.append(" ");
            }
            formattedTime.append(minutes).append(" мин.");
        }
        if (seconds > 0L || formattedTime.isEmpty()) {
            if (!formattedTime.isEmpty()) {
                formattedTime.append(" ");
            }
            formattedTime.append(seconds).append(" сек.");
        }
        return formattedTime.toString();
    }
}