package com.cybexmobile.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    public static final String PATTERN_yyyy_MM_dd = "yyyy/MM/dd";
    public static final String PATTERN_yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
    public static final String PATTERN_MM_dd_HH_mm_ss = "MM/dd HH:mm:ss";
    public static final String PATTERN_yyyy_MM_dd_T_HH_mm_ss = "yyyy-MM-dd'T'HH:mm:ss";

    public static String formatToDate(String pattern, long timeStamp){
        try{
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
            return sdf.format(new Date(timeStamp));
        } catch(Exception ex){
            return "xx";
        }
    }

    public static long formatToMillis(String timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(PATTERN_yyyy_MM_dd_T_HH_mm_ss, Locale.getDefault());
        Calendar calendar = new GregorianCalendar();
        TimeZone mTimeZone = calendar.getTimeZone();
        int mOffset = mTimeZone.getRawOffset();
        try {
            Date parsedDate = dateFormat.parse(timestamp);
            return parsedDate.getTime() + mOffset;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
