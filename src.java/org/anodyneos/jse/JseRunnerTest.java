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


/**
 *  Executes a <code>Runnable</code> zero or more times as necessary given a date
 *  range.  Tracks running threads for this job and handles threading,
 *  maxQueue, etc.  Consider options such as maxRunTime, maxConcurrent, etc.
 *  Another idea - queue method adds Dates, don't expose schedule here.
 *
 *  @version $Id: JseRunnerTest.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class JseRunnerTest {
    public JseRunnerTest() {
    }

    public static void main(String[] args) throws JseException {
        JseRunner r;
        Runnable job = new SimpleJob();
        Runnable job2 = new SimpleJob();
        int i = 0;

        r = new JseRunner();
        r.setMaxConcurrent(2);
        //r.setMaxQueue(2);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        //r.queue(job2);
        System.out.println("**** getCount():           " + r.getCount());
        System.out.println("**** getCount(job):        " + r.getCount(job));
        System.out.println("**** getWaitingCount():    " + r.getWaitingCount());
        System.out.println("**** getWaitingCount(job): " + r.getWaitingCount(job));
        System.out.println("**** getRunningCount():    " + r.getRunningCount());
        System.out.println("**** getRunningCount(job): " + r.getRunningCount(job));

        System.out.println("-- start");
        r.start();

        System.out.println("-- sleeping 100");
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        System.out.println("**** getCount():           " + r.getCount());
        System.out.println("**** getCount(job):        " + r.getCount(job));
        System.out.println("**** getWaitingCount():    " + r.getWaitingCount());
        System.out.println("**** getWaitingCount(job): " + r.getWaitingCount(job));
        System.out.println("**** getRunningCount():    " + r.getRunningCount());
        System.out.println("**** getRunningCount(job): " + r.getRunningCount(job));

        System.out.println("-- sleeping 6000");
        try { Thread.sleep(6000); } catch (InterruptedException e) { }
        System.out.println("**** getCount():           " + r.getCount());
        System.out.println("**** getCount(job):        " + r.getCount(job));
        System.out.println("**** getWaitingCount():    " + r.getWaitingCount());
        System.out.println("**** getWaitingCount(job): " + r.getWaitingCount(job));
        System.out.println("**** getRunningCount():    " + r.getRunningCount());
        System.out.println("**** getRunningCount(job): " + r.getRunningCount(job));

        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);

        System.out.println("-- sleeping 3000");
        try { Thread.sleep(3000); } catch (InterruptedException e) { }

        System.out.println("-- maxConcurrent 0");
        r.setMaxConcurrent(0);

        System.out.println("-- sleeping 6000");
        try { Thread.sleep(6000); } catch (InterruptedException e) { }

        System.out.println("-- maxConcurrent 8");
        r.setMaxConcurrent(8);


        System.out.println("-- sleeping 6000");
        try { Thread.sleep(6000); } catch (InterruptedException e) { }

        r.queue(job);
        r.queue(job);
        r.queue(job);
        r.queue(job);

        System.out.println("-- release");
        r.release();

        System.out.println("-- joining");
        try { r.join(); } catch (InterruptedException e) { }

       System.out.println("-- done");
    }

    public static class SimpleJob implements Runnable {
        private int iter = 0;

        public SimpleJob() {
        }

        public void run() {
            System.out.println("SampleJseCronJob run ########### " + System.currentTimeMillis() + " ########" + ++iter);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
}
