package com.yi.player.util;

/**
 * Created by kevin on 17-3-13.
 */

public class TimeUtil {

    /**
     * @param timeValue //ms
     */
    public static String format(long timeValue) {
        if (timeValue < 0) {
            timeValue = 0;
        }
        int time = (int) Math.round((double) timeValue / 1000);
        int hour = time / 3600;
        int min = (time - 3600 * hour) / 60;
        int seconds = time % 60;
        if (hour > 0) {

            if (min < 10) {
                if (seconds < 10) {
                    return hour + ":" + "0" + min + ":" + "0" + seconds;
                } else {
                    return hour + ":" + "0" + min + ":" + seconds;
                }
            } else {
                if (seconds < 10) {
                    return hour + ":" + min + ":" + "0" + seconds;
                } else {
                    return hour + ":" + min + ":" + seconds;
                }
            }
        } else {
            if (min < 10) {
                if (seconds < 10) {
                    return "0" + min + ":" + "0" + seconds;
                } else {
                    return "0" + min + ":" + seconds;
                }
            } else {
                if (seconds < 10) {
                    return min + ":" + "0" + seconds;
                } else {
                    return min + ":" + seconds;
                }
            }
        }
    }

    public static int progress(long playTime, long allTime) {
        return (int) (playTime > allTime ? allTime : playTime);
    }

}
