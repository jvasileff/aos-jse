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

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

/**
 *  Creates and manages <code>Timer</code>s.  <code>Timer</code>s are capable
 *  of running <code>Runnables</code> and <code>JseDateAwareJob</code>s on a
 *  schedule specified by a <code>JseSchedule</code>.
 *
 *  @version $Id: JseTimerService.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class JseTimerService extends Thread {
    private JseRunner runner;
    private long lastMillis;

    /** to help JobWrapper compareTo when nextDates are equal */
    private long idCounter = Long.MIN_VALUE;

    /**
     *  Tracks all actively managed jobs.  Values are JobWrappers.
     */
    private TreeSet managedJobs = new TreeSet();

    /**
     *  Create a new JseTimerService.
     */
    public JseTimerService() {
        this.lastMillis = System.currentTimeMillis() - 1;
        runner = new JseRunner();
        runner.start();
    }

    /**
     *  Adds a JseDateAwareJob with an associated schedule.
     *
     *  @param job The JseDateAwareJob to be run.  Must not be null.
     *  @param schedule The schedule, must not be null.
     */
    public synchronized JseTimer createTimer(JseDateAwareJob daJob, JseSchedule schedule) {
        JseTimer timer = new JobWrapper(daJob, schedule).getTimer();
        this.notify();
        return timer;
    }

    /**
     *  Adds a Runnable with an associated schedule.
     *
     *  @param job The Runnable to be run.  Must not be null.
     *  @param schedule The schedule, must not be null.
     */
    public synchronized JseTimer createTimer(Runnable job, JseSchedule schedule) {
        JseTimer timer = new JobWrapper(job, schedule).getTimer();
        this.notify();
        return timer;
    }

    /**
     *  Retuns the number of jobs currently waiting to be run.
     */
    public int getWaitingCount() {
        return runner.getWaitingCount();
    }

    /**
     *  Retuns the number of jobs is currently running.
     */
    public int getRunningCount() {
        return runner.getRunningCount();
    }

    /**
     *  Retuns the number jobs currently queued (# waiting + # running.)
     */
    public int getCount() {
        return runner.getCount();
    }

    public void setMaxConcurrent(int max) {
        runner.setMaxConcurrent(max);
    }

    public int getMaxConcurrent() {
        return runner.getMaxConcurrent();
    }

    public synchronized void run() {
        while(true) {
            while(managedJobs.size() == 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            JobWrapper jw = (JobWrapper) managedJobs.first();
            long nextTime = jw.nextDate.getTime();
            long millis = System.currentTimeMillis();
            if(null != jw && millis >= nextTime) {
                jw.queue();
            } else {
                try {
                    if(null != jw) {
                        wait(nextTime - millis);
                    } else {
                        wait();
                    }
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     *  Wraps a JseDateAwareJob with a Runnable interface and provides methods to
     *  manage itself including its own addition and removal from the outer
     *  class's <code>managedJobs</code>;  also supports management for
     *  Runnables.
     */
    private final class JobWrapper implements Runnable, Comparable {

        private JseTimer timer;

        /** to help compareTo when nextDates are equal */
        private long id = idCounter++;

        /** one of daJob or job must not be null */
        private JseDateAwareJob daJob;
        private Runnable job;

        /** immutable schedule */
        private JseSchedule schedule;
        /** next call to queue() should use this date */
        private Date nextDate;
        /** Used by run(Date) method of JseDateAwareJob */
        private List dateQueue = new LinkedList();
        /** Number of iterations remaining; -1 == infinity */
        private int remainingIterations;

        /** constructor for JseDateAwareJob */
        private JobWrapper(JseDateAwareJob daJob, JseSchedule schedule) {
            this.daJob = daJob;
            setSchedule(schedule);
            timer = new JseTimerImpl(this);
        }

        /** constructor for Runnable */
        private JobWrapper(Runnable job, JseSchedule schedule) {
            this.job = job;
            setSchedule(schedule);
            timer = new JseTimerImpl(this);
        }

        /** only called by constructors */
        private void setSchedule(JseSchedule schedule) {
            this.schedule = schedule;
            // setup iterations & nextDate var.
            remainingIterations = schedule.getNumberOfIterations() >= 0 ?
                schedule.getNumberOfIterations() + 1: -1;
            nextDate = new Date(System.currentTimeMillis() - 1);
            nextDate();
        }

        /** get the timer for this JobWrapper */
        private JseTimer getTimer() {
            return timer;
        }

        /** should only be called internally to this class */
        private Date nextDate() {
            Date date = nextDate;
            // must remove, then add since updating nextDate
            managedJobs.remove(this);
            nextDate = schedule.getNextTimeout(new Date(date.getTime() + 1));
            if (null == nextDate) {
                // this is the last one
                remainingIterations = 0;
            } else if (remainingIterations > 0) {
                remainingIterations--;
            }
            // if done...
            if (remainingIterations == 0) {
                cancel();
            } else {
                managedJobs.add(this);
            }
            return date;
        }

        /** called when curtime == nextDate */
        private void queue() {
            // ASSERT: remainingIterations < 0 || remainingIterations > 0

            // the date for this run
            Date date = nextDate();
            // make sure room left in queue
            if (schedule.getMaxQueue() == -1 || getCount() < schedule.getMaxQueue()) {
                // good, queue has room
                if (daJob != null) {
                    dateQueue.add(date);
                }
                runner.queue(this);
            } // else throw away
        }

        /** May be called from outside JseTimerService. */
        private void cancel() {
            synchronized(JseTimerService.this) {
                remainingIterations = 0;
                managedJobs.remove(this);
            }
        }

        /**
         *  Retuns the number of times this job is currently waiting to be run.
         *  May be called from outside JseTimerService.
         */
        private int getWaitingCount() {
            return runner.getWaitingCount(this);
        }

        /**
         *  Retuns the number of times this job is currently running.
         *  May be called from outside JseTimerService.
         */
        private int getRunningCount() {
            return runner.getRunningCount(this);
        }

        /**
         *  Retuns the number of times this job is currently queued (# waiting
         *  + # running).  May be called from outside JseTimerService.
         */
        private int getCount() {
            return runner.getCount(this);
        }

        /**
         *  From Runnable interface. May be called from outside JseTimerService.
         */
        public void run() {
            if (daJob != null) {
                Date date;
                synchronized(JseTimerService.this) {
                    date = (Date) dateQueue.remove(0);
                }
                daJob.run(date);
            } else {
                job.run();
            }
        }

        /**
         *  Compares to another JobWrapper in terms of nextDate.  Since two
         *  jobs may share a nextDate, care is taken to ensure no otherwise
         *  unequal jobs are equal.  In other words, this Comparator is
         *  consistent with equals at the expense of showing as equal two jobs
         *  with the same nextDate.
         */
        public int compareTo(Object obj) {
            JobWrapper that = (JobWrapper) obj;

            int result = this.nextDate.compareTo(that.nextDate);
            if (result == 0) {
                // ensure consistent with equals
                if (this.equals(that)) {
                    result = 0;
                } else if (this.id < that.id) {
                    result = -1;
                } else if (this.id > that.id) {
                    result = 1;
                } else {
                    throw new Error("id space exhausted - shouldn't happen.");
                }
            }
            return result;
        }

    }

    private class JseTimerImpl implements JseTimer {
        WeakReference ref;

        private JseTimerImpl(JobWrapper scheduledJob) {
            ref = new WeakReference(scheduledJob);
        }
        private JobWrapper getJobWrapper() throws JseTimerExpiredException {
            JobWrapper jw = (JobWrapper) ref.get();
            if (null == jw || jw.remainingIterations == 0) {
                throw new JseTimerExpiredException();
            } else {
                return jw;
            }
        }

        public int getWaitingCount() throws JseTimerExpiredException {
            return getJobWrapper().getWaitingCount();
        }
        public int getRunningCount() throws JseTimerExpiredException {
            return getJobWrapper().getRunningCount();
        }
        public int getCount() throws JseTimerExpiredException {
            return getJobWrapper().getCount();
        }
        public void cancel() throws JseTimerExpiredException {
            getJobWrapper().cancel();
        }
    }

}
