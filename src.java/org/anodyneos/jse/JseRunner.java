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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 *  Launches and tracks threads running the <code>run</code> method of a
 *  Runnable.
 *
 *  @version $Id: JseRunner.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class JseRunner extends Thread {

    public static final int DEFAULT_MAX_CONCURRENT = 1;

    private int maxConcurrent = DEFAULT_MAX_CONCURRENT;
    private HashSet activeThreads = new HashSet();
    private LinkedList jobQueue = new LinkedList();
    private boolean done = false;

    /**
     *  <code>jobCountActive</code> and <code>jobCountQueue</code> map
     *  <code>Runnable</code>s to Integers that track the number of times a
     *  particular <code>Runnable</code> is currently queued or running.
     */
    private Counter jobRunningCounter = new Counter();
    private Counter jobWaitingCounter = new Counter();

    /**
     *  Create a new <code>JseRunner</code> thread.  After necessary
     *  configurations, the <code>start()</code> method should be used to
     *  launch this thread.  No jobs will be run prior to calling the
     *  <code>start()</code> method.
     */
    public JseRunner() {
        // super
    }

    /**
     *  Add job to the run queue.  The job added may not necessarily run
     *  immediately depending on the <code>maxConcurrent</code> property of
     *  this <code>JseRunner</code>.
     *
     *  @param job The job to be run.  It will be added to the end of the queue.
     */
    public synchronized void queue(Runnable job) {
        if (null != job) {
            jobQueue.addLast(job);
            jobWaitingCounter.incr(job);
            notify();
        }
    }

    /**
     *  Set the maximum number of concurrent threads.  When the number of
     *  threads grows to this number, no additional threads will be launched
     *  until previously run threads complete processing.
     *
     *  @param max The maximum number of concurrent threads to run.  Default is
     *  1.  Use -1 for unlimited.  If 0 is used, no new threads will be
     *  launched.
     */
    public synchronized void setMaxConcurrent(int max) {
        this.maxConcurrent = max;
        notify();
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    /**
     *  Instruct JseRunner to exit once all queued threads are launched.
     *  JseRunner may exit before all threads have completed execution.
     *  Normally, JseRunner will continue to run even when no items are queued.
     */
    public synchronized void release() {
        done = true;
        notify();
    }

    /**
     *  Called by start().
     */
    public synchronized void run() {
        // process queue.  When empty or maxConcurrent, wait.  Exit when told.
        while(true) {
            if (done && jobQueue.size() == 0) {
                break;
            }
            if (jobQueue.size() != 0 &&
                    (   maxConcurrent == -1 ||
                        maxConcurrent > activeThreads.size())) {
                // run next job
                Runnable job = (Runnable) jobQueue.removeFirst();
                jobWaitingCounter.decr(job);
                jobRunningCounter.incr(job);
                RunnableThread jobThread = new RunnableThread(job);
                activeThreads.add(jobThread);
                jobThread.start();
            } else {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     *  Used by RunnableThread when the Runnable has completed.
     */
    private synchronized void finished(RunnableThread thread) {
        activeThreads.remove(thread);
        jobRunningCounter.decr(thread.job);
        notify();
    }

    /**
     *  Retuns the number of times the <code>Runnable</code> is currently waiting
     *  to be run.
     */
    public final synchronized int getWaitingCount(Runnable job) {
        return jobWaitingCounter.value(job);
    }

    /**
     *  Retuns the number of times the <code>Runnable</code> is currently
     *  running.
     */
    public final synchronized int getRunningCount(Runnable job) {
        return jobRunningCounter.value(job);
    }

    /**
     *  Retuns the number of times the <code>Runnable</code> is currently queued
     *  (# waiting + # running.)
     */
    public final synchronized int getCount(Runnable job) {
        return jobWaitingCounter.value(job) + jobRunningCounter.value(job);
    }

    /**
     *  Retuns the total number of <code>Runnable</code>s currently waiting to be run.
     */
    public final synchronized int getWaitingCount() {
        return jobQueue.size();
    }

    /**
     *  Retuns the total number of <code>Runnable</code>s currently running.
     */
    public final synchronized int getRunningCount() {
        return activeThreads.size();
    }

    /**
     *  Retuns the total number of <code>Runnable</code>s currently queued (#
     *  waiting + # running.)
     */
    public final synchronized int getCount() {
        return jobQueue.size() + activeThreads.size();
    }

    private class RunnableThread extends Thread {

        Runnable job;

        RunnableThread(Runnable job) {
            this.job = job;
        }

        public void run() {
            try {
                job.run();
            //} catch(Throwable t) {
                // don't catch - jvm will print to console
            } finally {
                finished(this);
            }
        }
    }

    private final class Counter {
        HashMap objectCounts = new HashMap();

        public final void incr(Object obj) {
            Integer count = (Integer) objectCounts.get(obj);
            if (null == count) {
                count = new Integer(1);
            } else {
                count = new Integer(count.intValue() + 1);
            }
            objectCounts.put(obj, count);
        }

        public final void decr(Object obj) {
            Integer count = (Integer) objectCounts.get(obj);
            if (null == count) {
                // this should never happen - jdk1.4 assert
            } else if (count.intValue() == 1) {
                objectCounts.remove(obj);
            } else {
                objectCounts.put(obj, new Integer(count.intValue() - 1));
            }
        }

        public final int value(Object obj) {
            Integer count = (Integer) objectCounts.get(obj);
            if (null == count) {
                return 0;
            } else {
                return count.intValue();
            }
        }
    }
}
