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

import java.io.PrintWriter;

/**
 *  @version $Id: CronContext.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class CronContext {
    private String jobName;
    private String jobGroupName;
    private CronSchedule schedule;
    private CronLogger logger;
    //private CronLoggerFactory loggerFactory;

    public CronContext(String jobGroupName, String jobName, CronSchedule schedule, CronLogger logger) {
        this.jobName = jobName;
        this.jobGroupName = jobGroupName;
        this.schedule = schedule;
        this.logger = logger;
    }

    public String getJobName() {
        return jobName;
    }
    public String getJobGroupName() {
        return jobGroupName;
    }
    public CronSchedule getCronSchedule() {
        return schedule;
    }

    public void logStandard(String message) {
        logger.logStandard(message, this);
    }
    public void logWarning(String message) {
        logger.logWarning(message, this);
    }
    public void logError(String message) {
        logger.logError(message, this);
    }

    public PrintWriter getPrintWriterStandard() {
        return logger.getPrintWriterStandard(this);
    }

    public PrintWriter getPrintWriterWarning() {
        return logger.getPrintWriterWarning(this);
    }

    public PrintWriter getPrintWriterError() {
        return logger.getPrintWriterError(this);
    }

}
