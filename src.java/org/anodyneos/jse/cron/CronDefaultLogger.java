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

import java.util.Date;

/**
 *  @version $Id: CronDefaultLogger.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class CronDefaultLogger extends CronLogger {
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public CronDefaultLogger() {
    }

    public void logStandard(String message, CronContext ctx) {
        System.out.print(formatMessage(message, "standard", ctx));
    }

    public void logWarning(String message, CronContext ctx) {
        System.out.print(formatMessage(message, "warning", ctx));
    }

    public void logError(String message, CronContext ctx) {
        System.out.print(formatMessage(message, "error", ctx));
    }

    protected String formatMessage(String message, String type, CronContext ctx) {
        StringBuffer sb = new StringBuffer();
        sb.append(" [" + new Date() + "]");
        sb.append("[" + type + "]");
        if (null != ctx) {
            sb.append(" [" + ctx.getJobGroupName() + "/" + ctx.getJobName() + "]");
        }
        if (-1 == message.indexOf(LINE_SEPARATOR)) {
            sb.append(" : ");
            sb.append(message);
            sb.append(LINE_SEPARATOR);
        } else {
            sb.append(LINE_SEPARATOR);
            sb.append(message);
        }
        return sb.toString();
    }

}
