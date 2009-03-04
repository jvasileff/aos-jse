/*
 * Copyright (c) 2002 John Vasileff
 *
 * Permission  is  hereby  granted,  free of  charge,  to  any  person
 * obtaining  a copy  of  this software  and associated  documentation
 * files   (the  "Software"),   to  deal   in  the   Software  without
 * restriction, including without limitation  the rights to use, copy,
 * modify, merge, publish, distribute,  sublicense, and/or sell copies
 * of the  Software, and  to permit  persons to  whom the  Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright  notice and  this permission  notice shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE  IS  PROVIDED  "AS   IS",  WITHOUT  WARRANTY  OF  ANY
 * KIND,  EXPRESS  OR  IMPLIED,  INCLUDING  BUT  NOT  LIMITED  TO  THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES  OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT,  TORT OR OTHERWISE, ARISING FROM, OUT  OF OR IN
 * CONNECTION WITH  THE SOFTWARE OR THE  USE OR OTHER DEALINGS  IN THE
 * SOFTWARE.
 */

package org.anodyneos.jse.cron;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;

import org.anodyneos.jse.JseSchedule;

/**
 *  @TODO The schedule should have a timeZone, not the passed-in dates.
 */

/**
 *  Implementation of JseSchedule that acts like Unix cron.
 *  <p>From crontab(5):</p>
 *  <pre>
 *  The time and date fields are:
 *
 *        field          allowed values
 *        -----          --------------
 *        second         0-59
 *        minute         0-59
 *        hour           0-23
 *        day of month   1-31
 *        month          1-12 (or names, see below)
 *        day of week    0-7 (0 or 7 is Sun, or use names)
 *
 *  A field may be an asterisk (*), which always stands for ``first-last''.
 *
 *  Ranges of numbers are allowed.  Ranges are two numbers separated  with  a
 *  hyphen.   The specified range is inclusive.  For example, 8-11 for an
 *  ``hours'' entry specifies execution at hours 8, 9, 10 and 11.
 *
 *  Lists are allowed.  A list is a set of numbers (or ranges) separated by
 *  commas.   Examples: ``1,2,5,9'', ``0-4,8-12''.
 *
 *  Step  values  can  be  used in conjunction with ranges.  Following a range
 *  with ``/&lt;number&gt;'' specifies skips of the number's value through the
 *  range.  For example, ``0-23/2'' can be used in the hours field to specify
 *  command execution every other hour (the alternative in the V7 standard is
 *  ``0,2,4,6,8,10,12,14,16,18,20,22'').  Steps are  also  permitted after an
 *  asterisk, so if you want to say ``every two hours'', just use ``&#42;/2''.
 *
 *  Names  can  also  be  used  for the ``month'' and ``day of week'' fields.
 *  Use the first three letters of the particular day or month (case doesn't
 *  matter).  Ranges or lists  of names are not allowed.
 *  </pre>
 *
 *  @version $Id: CronSchedule.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public final class CronSchedule implements JseSchedule {

    private int numIterations;
    private int maxQueue;
    private Date notBeforeDate;
    private Date notAfterDate;

    private static final HashMap<String, Integer> dayMap;
    private static final HashMap<String, Integer> monthMap;

    static {
        dayMap = new HashMap<String, Integer>();
        dayMap.put("sun", new Integer(0));
        dayMap.put("mon", new Integer(1));
        dayMap.put("tue", new Integer(2));
        dayMap.put("wed", new Integer(3));
        dayMap.put("thu", new Integer(4));
        dayMap.put("fri", new Integer(5));
        dayMap.put("sat", new Integer(6));
        dayMap.put("7", new Integer(0));
    }

    static {
        monthMap = new HashMap<String, Integer>();
        monthMap.put("jan", new Integer(1));
        monthMap.put("feb", new Integer(2));
        monthMap.put("mar", new Integer(3));
        monthMap.put("apr", new Integer(4));
        monthMap.put("may", new Integer(5));
        monthMap.put("jun", new Integer(6));
        monthMap.put("jul", new Integer(7));
        monthMap.put("aug", new Integer(8));
        monthMap.put("sep", new Integer(9));
        monthMap.put("oct", new Integer(10));
        monthMap.put("nov", new Integer(11));
        monthMap.put("dec", new Integer(12));
    }

    private String scheduleString;
    private boolean[] secondBitmap;
    private boolean[] minuteBitmap;
    private boolean[] hourBitmap;
    private boolean[] dayOfMonthBitmap;
    private boolean[] monthBitmap;
    private boolean[] dayOfWeekBitmap;

    private boolean allDaysOfMonthSet = false;
    private boolean allDaysOfWeekSet = false;

    private boolean allDaysOfMonth;
    private boolean allDaysOfWeek;

    private TimeZone timeZone;
    private Calendar dummyCal;
    private CalcNext calc = new CalcNext();

    /**
     *  Creates a CronSchedule and parses the schedule string.
     *
     *  @param scheduleString Schedule string as described above.
     */
    public CronSchedule(String scheduleString, TimeZone tz) throws CronParseException {
        this(scheduleString, tz, -1, -1, null, null);
    }

    /**
     *  Creates a CronSchedule and parses the schedule string.
     *
     *  @param scheduleString Schedule string as described above.
     */
    public CronSchedule(String scheduleString, TimeZone tz, int numIterations, int maxQueue,
            Date notBeforeDate, Date notAfterDate) throws CronParseException {
        this.numIterations = numIterations;
        this.maxQueue = maxQueue;
        this.notBeforeDate = notBeforeDate;
        this.notAfterDate = notAfterDate;

        this.scheduleString = scheduleString;

        this.timeZone = tz;
        this.dummyCal = new GregorianCalendar(timeZone);
        this.dummyCal.setLenient(false);

        ArrayList<String> l = split(scheduleString);

        if (l.size() != 6) {
            throw new CronParseException("invalid schedule, must have 6 tokens: '" + scheduleString + "'");
        }

        int i = 0;
        secondBitmap = processSeconds(l.get(i++));
        minuteBitmap = processMinutes(l.get(i++));
        hourBitmap = processHours(l.get(i++));
        dayOfMonthBitmap = processDaysOfMonth(l.get(i++));
        monthBitmap = processMonths(l.get(i++));
        dayOfWeekBitmap = processDaysOfWeek(l.get(i++));
    }

    public final Date getNextTimeout(Date start) {
        if (null != getNotBefore() && start.before(getNotBefore())) {
            start = getNotBefore();
        }
        Date next = calc.findNext(start);
        if (null != getNotAfter() && next.after(getNotAfter())) {
            return null;
        } else {
            return next;
        }
    }

    public final Date getNotBefore() {
        return notBeforeDate;
    }
    public final Date getNotAfter() {
        return notAfterDate;
    }
    public final int getNumberOfIterations() {
        return numIterations;
    }
    public final int getMaxQueue() {
        return maxQueue;
    }

    /**
     *  Returns schedule string that was passed to the constructor.
     *  @return Schedule string that was passed to the constructor.
     */
    public final String getScheduleString() {
        return scheduleString;
    }

    public final Date[] getDates(Date startDate, Date endDate) {
        ArrayList<Date> al = new ArrayList<Date>();
        while(true) {
            startDate = getNextTimeout(startDate);
            if (startDate == null || startDate.after(endDate)) {
                break;
            } else {
                al.add(startDate);
                startDate = new Date(startDate.getTime() + 1);
            }
        }
        return al.toArray(new Date[al.size()]);
    }

    private synchronized int lastDayInMonth(int year, int month) {
        dummyCal.clear();
        dummyCal.set(Calendar.YEAR, year);
        dummyCal.set(Calendar.MONTH, month);
        dummyCal.set(Calendar.DAY_OF_MONTH, 1);

        int day = dummyCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return day;
    }

    private synchronized int dayOfFirstDayOfMonth(int year, int month) {
        dummyCal.clear();
        dummyCal.set(Calendar.YEAR, year);
        dummyCal.set(Calendar.MONTH, month);
        dummyCal.set(Calendar.DAY_OF_MONTH, 1);

        int day = dummyCal.get(Calendar.DAY_OF_WEEK);
        return day;
    }

    private synchronized int dayOfYear(int year, int month, int dayOfMonth) {
        dummyCal.clear();
        dummyCal.set(Calendar.YEAR, year);
        dummyCal.set(Calendar.MONTH, month);
        dummyCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        int day = dummyCal.get(Calendar.DAY_OF_YEAR);
        return day;
    }

    /**
     *  Returns a Date for the given params or null if no such time exists due
     *  to daylight savings times or other calendar anomalies.
     *
     *  Daylight savings time issues:
     *
     *  When an hour is lost, such as on April 2nd, 2000 NY, the 2 o'clock hour
     *  doesn't exist.  With jdk1.3, 4/2/00 02:00 NY translates to 4/2/00 01:00
     *  EST, which happens to be the same as 4/2/00 01:00 NY.  With jdk1.4, the
     *  time would be 4/2/00 03:00 EDT which is one hour after 1am EST.
     *
     *  For its intended usage, niether treatment is desired.  To handle this,
     *  after setting the calendar, the original specs are checked.  If they do
     *  not match what was intended, null is returned.
     */
    private synchronized Date newDate(
            int year, int dayOfYear, int hour, int minute, int second) {

        dummyCal.clear();
        dummyCal.set(Calendar.YEAR, year);
        dummyCal.set(Calendar.DAY_OF_YEAR, dayOfYear);
        dummyCal.set(Calendar.HOUR_OF_DAY, hour);
        dummyCal.set(Calendar.MINUTE, minute);
        dummyCal.set(Calendar.SECOND, second);
        dummyCal.set(Calendar.MILLISECOND, 0);

        if(         dummyCal.get(Calendar.YEAR) == year
                &&  dummyCal.get(Calendar.DAY_OF_YEAR) == dayOfYear
                &&  dummyCal.get(Calendar.HOUR_OF_DAY) == hour
                &&  dummyCal.get(Calendar.MINUTE) == minute
                &&  dummyCal.get(Calendar.SECOND) == second
                &&  dummyCal.get(Calendar.MILLISECOND) == 0) {
            return dummyCal.getTime();
        } else {
            return null;
        }
    }

    // INITIALIZE INTERNAL STRUCTURES FOR CRON STRING
    private static final boolean[] processSeconds(String s) throws CronParseException {
        try {
            return processRangeString(s, 0, 59, null, false);
        } catch (CronParseException e) {
            throw new CronParseException("seconds: " + e.getMessage());
        }
    }

    private static final boolean[] processMinutes(String s) throws CronParseException {
        try {
            return processRangeString(s, 0, 59, null, false);
        } catch (CronParseException e) {
            throw new CronParseException("minutes: " + e.getMessage());
        }
    }

    private static final boolean[] processHours(String s) throws CronParseException {
        try {
            return processRangeString(s, 0, 23, null, false);
        } catch (CronParseException e) {
            throw new CronParseException("hours: " + e.getMessage());
        }
    }

    private static final boolean[] processDaysOfMonth(String s) throws CronParseException {
        try {
            return processRangeString(s, 1, 31, null, false);
        } catch (CronParseException e) {
            throw new CronParseException("days of month: " + e.getMessage());
        }
    }

    private static final boolean[] processMonths(String s) throws CronParseException {
        try {
            return processRangeString(s, 1, 12, monthMap, false);
        } catch (CronParseException e) {
            throw new CronParseException("months: " + e.getMessage());
        }
    }

    private static final boolean[] processDaysOfWeek(String s) throws CronParseException {
        try {
            return processRangeString(s, 0, 6, dayMap, true);
        } catch (CronParseException e) {
            throw new CronParseException("days of week: " + e.getMessage());
        }
    }

    private static final boolean[] processRangeString(
            String s,
            int lowerBounds, int upperBounds,
            HashMap<String, Integer> valueMap, boolean wrap) throws CronParseException {
        boolean[] bitmap = new boolean[upperBounds - lowerBounds + 1];
        ArrayList<String> ranges = split(s, ',');
        if (ranges.size() == 0) {
            throw new CronParseException("range must not be empty: '" + s + "'");
        }

        for(int i = 0; i < ranges.size(); i++) {
            String range = ranges.get(i);
            if (range.length() == 0) {
                throw new CronParseException("range part must not be empty: '" + s + "'");
            }

            if ("*".equals(range)) {
                setAll(bitmap, true);
                break;
            }

            ArrayList<String> parts = split(range, '/');
            if (parts.size() > 2) {
                throw new CronParseException("syntax error (/): '" + range + "'");
            }
            String rangePart = parts.get(0);
            int stepPart = 1;
            int start;
            int end;
            if (parts.size() == 2) {
                try {
                    stepPart = Integer.parseInt(parts.get(1));
                } catch (NumberFormatException e) {
                    throw new CronParseException("syntax error (step): '" + range + "'");
                }
            }
            if ("*".equals(rangePart)) {
                start = lowerBounds;
                end = upperBounds;
            } else {
                ArrayList<String> rangeParts = split(rangePart, '-');
                if (rangeParts.size() > 2) {
                    throw new CronParseException("syntax error (-): '" + range + "'");
                }
                try {
                    if (null != valueMap && valueMap.containsKey( rangeParts.get(0).toLowerCase() )) {
                        start = valueMap.get( rangeParts.get(0).toLowerCase()).intValue();
                    } else {
                        start = Integer.parseInt(rangeParts.get(0));
                    }
                } catch (NumberFormatException e) {
                    throw new CronParseException("syntax error (start): '" + range + "'");
                }
                // don't causes problem with step: start = Math.max(start, lowerBounds);
                if (rangeParts.size() == 2) {
                    try {
                        if (null != valueMap && valueMap.containsKey( rangeParts.get(1).toLowerCase() )) {
                            end = valueMap.get( rangeParts.get(1).toLowerCase()).intValue();
                        } else {
                            end = Integer.parseInt(rangeParts.get(1));
                        }
                    } catch (NumberFormatException e) {
                        throw new CronParseException("syntax error (end): '" + range + "'");
                    }
                    // don't for consistency: end = Math.min(end, upperBounds);
                } else {
                    end = start;
                }
                if (start > upperBounds || start < lowerBounds) {
                    throw new CronParseException("start out of range: '" + range + "'");
                }
                if (end > upperBounds || end < lowerBounds) {
                    throw new CronParseException("end out of range: '" + range + "'");
                }
                if (start > end) {
                    if (!wrap) {
                        throw new CronParseException("start must be less than or equal to end: '" + range + "'");
                    } else {
                        // start month 10, end month 9
                        // start month 10, end month 21
                        end += upperBounds - lowerBounds + 1;
                    }
                }
                if (stepPart < 1) {
                    throw new CronParseException("step must be a positive integer: '" + range + "'");
                }
            }

            if (!wrap) {
                for (int j = start; j <= end; j += stepPart) {
                    bitmap[j - lowerBounds] = true;
                }
            } else {
                // offset = 1 for months
                int offset = lowerBounds;
                for (int j = start-offset; j <= end-offset; j += stepPart) {
                    // for months:
                    // when j=3, month is 4 (april) = 3 % 12 + 1
                    // when j=12, month is 1 (january) = 12 % 12 + 1
                    int val = (j % (upperBounds - lowerBounds + 1)) + offset;
                    bitmap[val - lowerBounds] = true;
                }
            }
        }

        return bitmap;
    }

    private static final ArrayList<String> split(String s, char c) {
        ArrayList<String> list = new ArrayList<String>();
        int startIndex = 0;
        int sepIndex;

        while ((sepIndex = s.indexOf(c, startIndex)) != -1) {
            String range = s.substring(startIndex,sepIndex);
            list.add(range);
            startIndex = sepIndex + 1;
        }

        String range = s.substring(startIndex);
        list.add(range);
        return list;
    }

    private static final ArrayList<String> split(String s, String sep) {
        ArrayList<String> list = new ArrayList<String>();
        int sepLength = sep.length();
        int startIndex = 0;
        int sepIndex;

        while ((sepIndex = s.indexOf(sep, startIndex)) != -1) {
            String range = s.substring(startIndex,sepIndex);
            list.add(range);
            startIndex = sepIndex + sepLength;
        }

        String range = s.substring(startIndex);
        list.add(range);
        return list;
    }

    private static final ArrayList<String> split(String s) {
        char c = ' ';
        ArrayList<String> list = new ArrayList<String>();
        int startIndex = 0;
        int sepIndex;

        while ((sepIndex = s.indexOf(c, startIndex)) != -1) {
            String range = s.substring(startIndex,sepIndex).trim();
            if (range.length() > 0) {
                list.add(range);
            }
            startIndex = sepIndex + 1;
        }

        String range = s.substring(startIndex).trim();
        if (range.length() > 0) {
            list.add(range);
        }
        return list;
    }

    private final boolean cronSecond(int i) {
        return secondBitmap[i];
    }
    private final boolean cronMinute(int i) {
        return minuteBitmap[i];
    }
    private final boolean cronHour(int i) {
        return hourBitmap[i];
    }
    private final boolean cronDayOfMonth(int i) {
        return dayOfMonthBitmap[i - 1];
    }
    private final boolean cronMonth(int i) {
        return monthBitmap[i - 1];
    }
    private final boolean cronDayOfWeek(int i) {
        return dayOfWeekBitmap[i];
    }

    private final boolean calendarSecond(int i) {
        return cronSecond(i);
    }
    private final boolean calendarMinute(int i) {
        return cronMinute(i);
    }
    private final boolean calendarHour(int i) {
        return cronHour(i);
    }
    private final boolean calendarDayOfMonth(int i) {
        return cronDayOfMonth(i);
    }
    private final boolean calendarMonth(int i) {
        // jan = 1 in cron package, jan = 0 in Calendar class
        return cronMonth(i + 1);
    }
    private final boolean calendarDayOfWeek(int i) {
        // sun = 0 in cron package, sun = 1 in Calendar class
        // range is 0-6 in both
        int cronDay = (i+6) % 7;
        return cronDayOfWeek(cronDay);
    }

    private final boolean allDaysOfMonth() {
        if (!allDaysOfMonthSet) {
            allDaysOfMonth = allTrue(dayOfMonthBitmap);
            allDaysOfMonthSet = true;
        }
        return allDaysOfMonth;
    }
    private final boolean allDaysOfWeek() {
        if (!allDaysOfWeekSet) {
            allDaysOfWeek = allTrue(dayOfWeekBitmap);
            allDaysOfWeekSet = true;
        }
        return allDaysOfWeek;
    }

    private static final boolean allTrue(boolean[] ba) {
        boolean ret = true;
        for (int i = 0; i < ba.length; i++) {
            if (! ba[i]) {
                ret = false;
                break;
            }
        }
        return ret;
    }

    private static final void setAll(boolean[] ba, boolean val) {
        for (int i = 0; i < ba.length; i++) {
            ba[i] = val;
        }
    }

    /**
     *  CalcNext can calculate the Date for getNextTimeout(Date start).  One
     *  instance should exist for each schedule and the only method called
     *  should be findNext() which is synchronized.
     */
    private final class CalcNext {
        private Calendar notBefore;
        private long nbMillis;
        private int nbYear;
        private int nbMonth;
        private int nbDayOfMonth;
        private int nbDayOfYear;
        private int nbHourOfDay;
        private int nbMinute;
        private int nbSecond;
        private int nbMillisecond;

        private CalcNext() {
        }

        /**
         *  Finds the next Date in the schedule greater or equal to the given
         *  Date.  Note: this method does not check notBefore, notAfter, and
         *  iterations.
         *
         *  @return the next Date or null if one doesn't exist.  CronSchedules
         *  will generally always have next Dates, the exception being a
         *  schedule that only specifies a Date that doesn't exist such as Feb
         *  30th.
         */
        private synchronized Date findNext(Date startDate) {
            Date result;
            nbMillis = startDate.getTime();
            // round up to nearest second
            long extraMillis = nbMillis % (1000);
            if (extraMillis > 0) {
                nbMillis = nbMillis - extraMillis + 1000;
            } else if (extraMillis < 0) {
                nbMillis = nbMillis - extraMillis;
            }
            notBefore = new GregorianCalendar(timeZone);
            notBefore.setTime(new Date(nbMillis)); // setTimeInMillis(nbMillis); in jdk1.4

            nbYear = notBefore.get(Calendar.YEAR);
            nbMonth = notBefore.get(Calendar.MONTH);
            nbDayOfMonth = notBefore.get(Calendar.DAY_OF_MONTH);
            nbDayOfYear = notBefore.get(Calendar.DAY_OF_YEAR);
            nbHourOfDay = notBefore.get(Calendar.HOUR_OF_DAY);
            nbMinute = notBefore.get(Calendar.MINUTE);
            nbSecond = notBefore.get(Calendar.SECOND);
            nbMillisecond = notBefore.get(Calendar.MILLISECOND);

            boolean checkPartial = true;
            if (        nbDayOfYear == 1
                    &&  nbHourOfDay == 0
                    &&  nbMinute == 0
                    &&  nbSecond == 0
                    &&  nbMillisecond == 0) {
                checkPartial = false;
            }

            result = searchYear(checkPartial, nbYear);
            if (result == null && checkPartial) {
                // try next full year
                result = searchYear(false, nbYear + 1);
            }
            return result;
        }

        private Date searchYear(boolean checkPartial, int year) {
            Date next = null;
            int numTried;
            int month;

            if ( checkPartial ) {
                month = nbMonth;
                numTried = 0;
            } else {
                month = 0;
                numTried = 1;
            }

            for( ; next == null && month < 12 && numTried < 2; month++) {
                if(calendarMonth(month)) {
                    // try this month
                    next = searchMonth(checkPartial, year, month);
                    numTried++;
                }
                checkPartial = false; // if not first
            }
            return next;
        }

        private Date searchMonth(boolean checkPartial, int year, int month) {
            Date next = null;
            int numTried;
            int day;

            if ( checkPartial ) {
                day = nbDayOfMonth;
                numTried = 0;
            } else {
                day = 1;
                numTried = 1;
            }

            int lastDay = lastDayInMonth(year, month);

            /*
                Variations
                    all daysOfMonth OK and all daysOfWeek OK: obvious
                    all daysOfMonth OK and some daysOfWeek: check daysOfWeek
                    some daysOfMonth OK and all daysOfWeek: check daysOfMonth
                    some daysOfMonth and some daysOfWeek: either will do
            */

            int dayOfFirstDayOfMonth = dayOfFirstDayOfMonth(year, month);
            int refDayOfYear = dayOfYear(year, month, 1);

            for( ; next == null && day <= lastDay && numTried < 2; day++) {
                boolean doit = false;
                if(allDaysOfWeek() && allDaysOfMonth()) {
                    // all days match
                    doit = true;
                } else if (! (allDaysOfWeek() || allDaysOfMonth())) {
                    // check either
                    doit = calendarDayOfMonth(day) ||
                            calendarDayOfWeek((dayOfFirstDayOfMonth + day - 1) % 7);
                } else if (allDaysOfWeek()) {
                    doit = calendarDayOfMonth(day);
                } else {
                    doit = calendarDayOfWeek((dayOfFirstDayOfMonth + day - 1) % 7);
                }
                if(doit) {
                    next = searchDayOfYear(checkPartial, year, refDayOfYear + day - 1);
                    numTried++;
                }
                checkPartial = false; // if not first
            }

            return next;
        }

        private Date searchDayOfYear(boolean checkPartial, int year, int dayOfYear) {
            Date next = null;
            int numTried;
            int hour;

            if ( checkPartial ) {
                hour = nbHourOfDay;
                numTried = 0;
            } else {
                hour = 0;
                numTried = 1;
            }

            for( ; next == null && hour < 24 && numTried < 2; hour++) {
                if(calendarHour(hour)) {
                    // try this hour
                    next = searchHour(checkPartial, year, dayOfYear, hour);
                    numTried++;
                }
                checkPartial = false; // if not first
            }
            return next;
        }

        private Date searchHour(boolean checkPartial, int year, int dayOfYear, int hour) {
            Date next = null;
            int numTried;
            int minute;

            if ( checkPartial ) {
                minute = nbMinute;
                numTried = 0;
            } else {
                minute = 0;
                numTried = 1;
            }

            for( ; next == null && minute < 60 && numTried < 2; minute++) {
                if(calendarMinute(minute)) {
                    // try this minute
                    next = searchMinute(checkPartial, year, dayOfYear, hour, minute);
                    numTried++;
                }
                checkPartial = false; // if not first
            }
            return next;
        }

        private Date searchMinute(boolean checkPartial, int year, int dayOfYear, int hour, int minute) {
            Date next = null;
            int numTried;
            int second;

            if ( checkPartial ) {
                second = nbSecond;
                numTried = 0;
            } else {
                second = 0;
                numTried = 1;
            }

            for( ; next == null && second < 60 && numTried < 2; second++) {
                if(calendarSecond(second)) {
                    // try this second
                    next = searchSecond(checkPartial, year, dayOfYear, hour, minute, second);
                    numTried++;
                }
                checkPartial = false; // if not first
            }
            return next;
        }

        private Date searchSecond(boolean checkPartial, int year, int dayOfYear, int hour, int minute, int second) {
            Date next = newDate(year, dayOfYear, hour, minute, second);
            if (null == next || (checkPartial && next.getTime() < nbMillis)) {
                return null;
            } else {
                return next;
            }
        }

    }

}
