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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 *  @version $Id: CronLogger.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public abstract class CronLogger {

    protected static final int STANDARD = 0;
    protected static final int WARNING = 1;
    protected static final int ERROR = 2;

    public abstract void logStandard(String message, CronContext ctx);
    public abstract void logWarning(String message, CronContext ctx);
    public abstract void logError(String message, CronContext ctx);

    public PrintWriter getPrintWriterStandard(CronContext ctx) {
        return new PrintWriter(new LogWriter(ctx, STANDARD));
    }

    public PrintWriter getPrintWriterWarning(CronContext ctx) {
        return new PrintWriter(new LogWriter(ctx, WARNING));
    }

    public PrintWriter getPrintWriterError(CronContext ctx) {
        return new PrintWriter(new LogWriter(ctx, ERROR));
    }

    protected class LogWriter extends StringWriter {
        protected boolean closed = false;
        protected CronContext cronContext;
        protected int type;

        protected LogWriter(CronContext ctx, int type) {
            this.cronContext = ctx;
            this.type = type;
        }

        public void close() throws IOException {
            super.close();
            if(! closed) {
                closed = true;
                switch(type) {
                    case STANDARD:
                        logStandard(this.toString(), cronContext);
                        break;
                    case WARNING:
                        logWarning(this.toString(), cronContext);
                        break;
                    case ERROR:
                        logError(this.toString(), cronContext);
                        break;
                }
            } else {
                throw new IOException("close() called when already closed");
            }
        }
    }

}
