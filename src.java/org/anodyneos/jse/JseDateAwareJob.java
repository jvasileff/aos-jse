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

package org.anodyneos.jse;

import java.util.Date;

/**
 *  Interface for jobs that wish to know the Date for which they are run.  Jobs
 *  must be thread safe since the run method may be called before a previous
 *  call to the run method was made.  This is will likely happen when the
 *  JseRunner is 'catching up', when system load is high, or when the job takes
 *  a long time to complete.
 *
 *  @see JseRunner
 *  @version $Id: JseDateAwareJob.java,v 1.1.1.1 2004-04-23 21:04:08 jvas Exp $
 */
public interface JseDateAwareJob {
    /**
     *  Method that called when running this job.  Must be thread safe.
     *
     *  @param runDate Date object representing the timestamp for this
     *  execution.  Note: this is not necessarily the current date, in fact, it
     *  may differ from the current date by days or even years if the system
     *  has a backlog of jobs or the JseRunner is 'catching up'.
     */
    void run(Date runDate);
}
