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

import org.anodyneos.jse.JseDateAwareJob;

/**
 *  @version $Id: SampleDateAwareJob.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class SampleDateAwareJob implements JseDateAwareJob {

    private int i = 1000;
    //private String dbPassword = "nonSet";
    private int dbPassword;

    public SampleDateAwareJob() {
        // super();
    }

    /*
    public void setDbPassword(String value) {
        this.dbPassword = value;
    }
    */
    public void setDbPassword(int value) {
        this.dbPassword = value;
    }

    public void run(Date runDate) {
        /*
        try {
            i = (int) (Math.random() * 100);
            Thread.sleep(i);
        } catch (Exception e) {}
        */
        System.out.println("SampleJseCronJob: run w/param: " + runDate + " " + dbPassword);
    }


}

