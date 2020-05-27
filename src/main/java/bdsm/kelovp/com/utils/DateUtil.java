package bdsm.kelovp.com.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
public class DateUtil {



    public static final String COMMON_DATE_FORMAT = "yyyyMMdd";
    public static final String COMMON_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String COMPACT_TIME_FORMAT = "yyyyMMddHHmmss";

    public static final String DATE_MONTH_DAY_FORMAT = "M月d日";

    /**
     * Base ISO 8601 Date format yyyyMMdd i.e., 20021225 for the 25th day of
     * December in the year 2002
     */
    public static final String ISO_DATE_FORMAT = "yyyyMMdd";

    /**
     * Expanded ISO 8601 Date format yyyy-MM-dd i.e., 2002-12-25 for the 25th day of
     * December in the year 2002
     */
    public static final String ISO_EXPANDED_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * yyyy-MM-dd HH:mm:ss
     */
    public static String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * Default lenient setting for getDate.
     */
    private static boolean LENIENT_DATE = false;

    /**
     * Returns the days between two dates. Positive values indicate that the second
     * date is after the first, and negative values indicate, well, the opposite.
     * Relying on specific times is problematic.
     *
     * @param early the "first date"
     * @param late  the "second date"
     * @return the days between the two dates
     */
    public static int daysBetween(Date early, Date late) {

        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        c1.setTime(early);
        c2.setTime(late);

        return daysBetween(c1, c2);
    }

    /**
     * Returns the days between two dates. Positive values indicate that the second
     * date is after the first, and negative values indicate, well, the opposite.
     *
     * @return the days between two dates.
     */
    public static int daysBetween(Calendar early, Calendar late) {

        return (int) (toJulian(late) - toJulian(early));
    }

    /**
     * Return a Julian date based on the input parameter. This is based from
     * calculations found at
     * <a href="http://quasar.as.utexas.edu/BillInfo/JulianDatesG.html">Julian Day
     * Calculations (Gregorian Calendar)</a>, provided by Bill Jeffrys.
     *
     * @param c a calendar instance
     * @return the julian day number
     */
    public static float toJulian(Calendar c) {
        int Y = c.get(Calendar.YEAR);
        int M = c.get(Calendar.MONTH);
        int D = c.get(Calendar.DATE);
        int A = Y / 100;
        int B = A / 4;
        int C = 2 - A + B;
        float E = (int) (365.25f * (Y + 4716));
        float F = (int) (30.6001f * (M + 1));
        return C + D + E + F - 1524.5f;
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String dateIncrease(String isoString, String fmt, int field, int amount) {
        try {
            Calendar cal = GregorianCalendar.getInstance(TimeZone
                    .getTimeZone("GMT"));
            cal.setTime(stringToDate(isoString, fmt, true));
            cal.add(field, amount);

            return dateToString(cal.getTime(), fmt);

        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Time Field Rolling function. Rolls (up/down) a single unit of time on the
     * given time field.
     *
     * @param field the time field.
     * @param up    Indicates if rolling up or rolling down the field value.
     */
    public static String roll(String isoString, String fmt, int field,
                              boolean up) {

        Calendar cal = GregorianCalendar.getInstance(TimeZone
                .getTimeZone("GMT"));
        cal.setTime(stringToDate(isoString, fmt));
        cal.roll(field, up);

        return dateToString(cal.getTime(), fmt);
    }

    /**
     * Time Field Rolling function. Rolls (up/down) a single unit of time on the
     * given time field.
     *
     * @param field the time field.
     * @param up    Indicates if rolling up or rolling down the field value.
     * @throws ParseException if an unknown field value is given.
     */
    public static String roll(String isoString, int field, boolean up)
            throws ParseException {

        return roll(isoString, DATETIME_PATTERN, field, up);
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static Date stringToDate(String dateText, String format,
                                    boolean lenient) {

        if (dateText == null) {

            return null;
        }

        DateFormat df;

        try {

            if (format == null) {
                df = new SimpleDateFormat();
            } else {
                df = new SimpleDateFormat(format);
            }

            df.setLenient(false);

            return df.parse(dateText);
        } catch (ParseException e) {

            return null;
        }
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static Date stringToDate(String dateString, String format) {

        return stringToDate(dateString, format, LENIENT_DATE);
    }

    /**
     * <Method Simple Description>
     */
    public static Date stringToDate(String dateString) {

        return stringToDate(dateString, ISO_EXPANDED_DATE_FORMAT, LENIENT_DATE);
    }

    public static Date stringToDateWithTime(String datetime) {
        return stringToDate(datetime, DATETIME_PATTERN);
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String dateToString(Date date, String pattern) {

        if (date == null) {

            return null;
        }

        try {
            SimpleDateFormat sfDate = new SimpleDateFormat(pattern);
            sfDate.setLenient(false);

            return sfDate.format(date);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String dateToString(Date date) {
        return dateToString(date, ISO_EXPANDED_DATE_FORMAT);
    }

    /**
     * <Method Simple Description>
     */
    public static Date getCurrentDateTime() {
        return Calendar.getInstance().getTime();
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String getCurrentDateString(String pattern) {
        return dateToString(getCurrentDateTime(), pattern);
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String getCurrentDateString() {
        return dateToString(getCurrentDateTime(), ISO_EXPANDED_DATE_FORMAT);
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String dateToStringWithTime(Date date) {

        return dateToString(date, DATETIME_PATTERN);
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static Date dateIncreaseByHour(Date date, int hours) {
        Calendar cal = GregorianCalendar.getInstance(TimeZone
                .getTimeZone("GMT"));
        cal.setTime(date);
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal.getTime();
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static Date dateIncreaseByDay(Date date, int days) {

        Calendar cal = GregorianCalendar.getInstance(TimeZone
                .getTimeZone("GMT"));
        cal.setTime(date);
        cal.add(Calendar.DATE, days);

        return cal.getTime();
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static Date dateIncreaseByMonth(Date date, int mnt) {

        Calendar cal = GregorianCalendar.getInstance(TimeZone
                .getTimeZone("GMT"));
        cal.setTime(date);
        cal.add(Calendar.MONTH, mnt);

        return cal.getTime();
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static Date dateIncreaseByYear(Date date, int mnt) {

        Calendar cal = GregorianCalendar.getInstance(TimeZone
                .getTimeZone("GMT"));
        cal.setTime(date);
        cal.add(Calendar.YEAR, mnt);

        return cal.getTime();
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String dateIncreaseByDay(String date, int days) {
        return dateIncreaseByDay(date, ISO_DATE_FORMAT, days);
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static Date dateIncreaseBySeconds(Date date, int seconds) {
        Calendar cal = GregorianCalendar.getInstance(TimeZone
                .getTimeZone("GMT"));
        cal.setTime(date);
        cal.add(Calendar.SECOND, seconds);
        return cal.getTime();
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String dateIncreaseByDay(String date, String fmt, int days) {
        return dateIncrease(date, fmt, Calendar.DATE, days);
    }

    /**
     * <Method Simple Description>
     *
     * @see
     */
    public static String stringToString(String src, String srcfmt, String desfmt) {
        return dateToString(stringToDate(src, srcfmt), desfmt);
    }

    /**
     * 判断时间是否在给出时间区间内，入参时间格式：HH:mm:ss
     */
    public static Boolean isBetweenHours(Date date, String from, String to) {
        String nowTime = dateToStringWithTime(date).substring(11);
        return 0 <= nowTime.compareTo(from) && nowTime.compareTo(to) <= 0;
    }

    public static final String getAddDateStr(Date beginDate, int nextDate) {
        SimpleDateFormat dft = new SimpleDateFormat("yyyy-MM-dd");
        Calendar date = Calendar.getInstance();
        date.setTime(beginDate);
        date.set(Calendar.DATE, date.get(Calendar.DATE) + nextDate);
        return dft.format(date.getTime());
    }

    public static Integer getMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        int month = cal.get(Calendar.MONTH) + 1;
        return month;
    }

    public static Integer getYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        int year = cal.get(Calendar.YEAR);
        return year;
    }

    public static Date maxTimeReturn(Date ...date){
        List<Date> dateList = new ArrayList<>(Arrays.asList(date));
        dateList = dateList.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (dateList.size() == 0){
            return null;
        }
        dateList.sort(Comparator.reverseOrder());
        return dateList.get(0);
    }

}
