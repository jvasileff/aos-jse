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

import java.io.PrintStream;

/**
 *  @version $Id: JseException.java,v 1.2 2005-04-08 04:02:47 jvas Exp $
 */
public class JseException extends Exception {

    private static final long serialVersionUID = 3258416140085573430L;

    private Throwable cause;

    public JseException() {
        super();
    }

    public JseException(String message) {
        super(message);
    }

    public JseException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }

    public JseException(Throwable cause) {
        super(cause==null ? null : cause.toString());
        this.cause = cause;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public void printStackTrace(PrintStream s) {
        synchronized (s) {
            super.printStackTrace(s);
            Throwable cause = getCause();
            if (cause != null) {
                s.println("Caused by: " + cause);
                cause.printStackTrace(s);
            }
        }
    }

}
