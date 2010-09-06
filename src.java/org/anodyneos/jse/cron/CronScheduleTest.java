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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/** @TODO investigate daylight savings time shifts.  Need to set cal more than
 * once?
 *
 *  @version $Id: CronScheduleTest.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class CronScheduleTest {

    GregorianCalendar startCal;
    GregorianCalendar endCal;
    CronSchedule schedule;

    public static void main(String[] args) throws CronParseException {
        CronScheduleTest obj = new CronScheduleTest();
        Date[] dates;
        long start;

        // warmup
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();
        dates = obj.run();

        start = System.currentTimeMillis();
        dates = obj.run();
        System.out.println("Time: " + (System.currentTimeMillis() - start));

        for(int i = 0; i < dates.length; i++) {
            System.out.println(dates[i]);
        }
        System.out.println("Number of dates: " + dates.length);

    }

    public CronScheduleTest() throws CronParseException {
        //startCal = new GregorianCalendar(2002, 0, 19, 22, 59);
        //endCal = new GregorianCalendar(2003, 0, 20, 0, 0);

        //startCal = new GregorianCalendar(2001, 0, 19, 22, 59);
        //endCal = new GregorianCalendar(2002, 5, 20, 10, 10);

        startCal = new GregorianCalendar(2000, 0, 1, 0, 0);
        endCal = new GregorianCalendar(2001, 0, 1, 0, 0);

        schedule = new CronSchedule("0 */15 12 1 jan-dec/3 *", TimeZone.getDefault());
        //schedule = new CronSchedule("0 0 0 31 * *");
        //schedule = new CronSchedule("0 30 1,2 * * *");

        // inspect daylight saving time
        //schedule = new CronSchedule("0 * 0,1,2 * apr,oct sun");
        //schedule = new CronSchedule("0 1 0,1,2 * apr,oct sun");
    }

    public Date[] run() {
        ArrayList<Date> al = new ArrayList<Date>();
        Date startDate = startCal.getTime();
        Date endDate = endCal.getTime();
        while(true) {
            startDate = schedule.getNextTimeout(startDate);
            if (startDate == null || startDate.after(endDate)) {
                break;
            } else {
                al.add(startDate);
                startDate = new Date(startDate.getTime() + 1);
            }
        }
        return al.toArray(new Date[al.size()]);
    }

}
