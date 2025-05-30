package com.pejaver.toaster;

public class Utils {

    // convert secs into a displayable text string
    public static String convertSecs(int secs) {
        secs = Math.round(secs);
        if (secs <= 0)
            return ("0 seconds");
        String result = "";
        int hours = Math.floorDiv(secs, 3600);
        secs = secs % 3600;
        if (hours > 0) {
            result = hours + " hour";
            if (hours > 1)
                result += 's';
            if (secs > 0)
                result += ", ";
        }
        int minutes = Math.floorDiv(secs, 60);
        secs = secs % 60;
        if (minutes > 0) {
            result += minutes + " minute";
            if (minutes > 1)
                result += 's';
            if (secs > 0)
                result += ", ";
        }

        if (secs > 0) {
            result += secs + " second";
            if (secs != 1)
                result += 's';		// say '1 sec', '2 secs'
        }

        return result;
    }
}
