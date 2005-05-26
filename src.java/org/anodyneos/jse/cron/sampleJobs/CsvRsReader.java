package org.anodyneos.jse.cron.sampleJobs;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import org.anodyneos.commons.text.CsvWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * CsvRsReader allows a ResultSet to be read as a CSV text file.  Internally a CsvWriter is used.
 * In order to avoid creating a new thread, PipedReader/PipedWriter are not used.  Instead,
 * CsvWriter fills a buffer that is used to return content when read() is called on Objects of
 * this Class.  The buffer is filled one line at a time as needed based on calls to read().
 *
 * TODO: Better handling of close()
 * TODO: simplify state variables
 *
 * @author jvas
 */
public class CsvRsReader extends Reader {

    private static Log log = LogFactory.getLog(CsvRsReader.class);

    private StringWriter buffer = new StringWriter();
    private CsvWriter csvWriter = new CsvWriter(buffer);
    private ResultSet rs;
    private ResultSetMetaData md;
    private int columnCount;
    private boolean noMoreRows = false;
    private boolean closeRsWhenDone = true;
    private boolean rsClosed = false;
    private boolean eof = false;

    public CsvRsReader(ResultSet rs) throws SQLException {
        this(rs, true);
    }

    public CsvRsReader(ResultSet rs, boolean closeRsWhenDone) throws SQLException {
        this.closeRsWhenDone = closeRsWhenDone;
        this.rs = rs;
        this.md = rs.getMetaData();
        this.columnCount = md.getColumnCount();
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("read() called");
        }
        if (eof) {
            return -1;
        }

        StringBuffer sb = buffer.getBuffer();
        while (sb.length() == 0 && !noMoreRows) {
            try {
                nextLine();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        if (sb.length() == 0) {
            eof = true;
            return -1;
        }

        // copy buffer to array
        int numRead;
        if (len < sb.length()) {
            numRead = len;
        } else {
            numRead = sb.length();
        }
        for (int i = off; i < off + numRead; i++) {
            cbuf[i] = sb.charAt(i - off);
        }
        log.debug(sb.substring(0, numRead));
        sb.delete(0, numRead);

        return numRead;
    }

    public void close() throws IOException {
        try {
            close(rs);
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    private void nextLine() throws SQLException, IOException {
        if(log.isDebugEnabled()) {
            log.debug("nextLine() called");
        }
        if(! rs.next()) {
            if(log.isDebugEnabled()) {
                log.debug("ResultSet exhausted");
            }
            noMoreRows = true;
            close(rs);
        } else {
            for (int i = 1; i <= columnCount; i++) {
                Object o;
                if ((o = rs.getObject(i)) != null) {
                    int colType = md.getColumnType(i);
                    String colClass = md.getColumnClassName(i).toUpperCase();
                    if (o instanceof Number) {
                        csvWriter.writeField((Number) o);
                    } else if (colType == Types.DATE || colType == Types.TIMESTAMP ||
                            (colClass.indexOf("TIMESTAMP") != -1)) {
                        Timestamp ts = rs.getTimestamp(i);
                        if (null != ts) {
                            //out.writeField(isof.format(rs.getTimestamp(i)));
                            csvWriter.writeField((rs.getTimestamp(i)).toString());
                        } else {
                            csvWriter.endField();
                        }
                    } else if (colType == Types.CLOB) {
                        Clob c = rs.getClob(i);
                        Reader r = c.getCharacterStream();
                        char[] buff = new char[1024];
                        int num;
                        while (-1 != (num = r.read(buff))) {
                            csvWriter.write(buff, 0, num);
                        }
                        csvWriter.endField();
                        r.close();
                    } else {
                        csvWriter.writeField(o.toString());
                    }
                } else {
                    csvWriter.endField();
                }
            }
            csvWriter.endRecord();
        }
    }

    private void close(ResultSet rs) throws SQLException {
        if (closeRsWhenDone && !rsClosed) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

}
