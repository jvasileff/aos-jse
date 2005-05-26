package org.anodyneos.jse.cron.sampleJobs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Adapter from java.io.Reader to java.io.InputStream.  FIXME: This class is terribly written - charactersets are not
 * handled correctly and the other read methods should be implemented to vastly improve performance.
 *
 * @author jvas
 */
public class ReaderInputStream extends InputStream {

    private static final Log log = LogFactory.getLog(ReaderInputStream.class);

    private Reader reader;

    public ReaderInputStream(Reader reader) {
        this.reader = reader;
    }

    public int read() throws IOException {
        // FIXME big time
        int val = reader.read();
        log.debug("returning: " + val);
        return val;
    }
   /*
    public int read(byte[] b, int off, int len) throws IOException {
        int val = read();
        if (val == -1) {
            log.debug("returning: -1");
            return -1;
        } else {
            b[off] = 84;
            log.debug("returning: 1");
            return 1;
        }
        // TODO Auto-generated method stub
        //return super.read(b, off, len);
    }
    */

    public void close() throws IOException {
        log.debug("close called");
        reader.close();
    }

}
