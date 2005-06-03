package org.anodyneos.jse.cron.sampleJobs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.anodyneos.commons.text.CsvWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * CsvRsInputStream allows a ResultSet to be read as a CSV text file.  Internally a CsvWriter is used.
 * In order to avoid creating a new thread, PipedInputStream/PipedWriter are not used.  Instead,
 * CsvWriter fills a buffer that is used to return content when read() is called on Objects of
 * this Class.  The buffer is filled one line at a time as needed based on calls to read().
 *
 * TODO: Better handling of close()
 * TODO: simplify state variables
 *
 * @author jvas
 */
public class CsvRsInputStream extends InputStream {

    private static Log log = LogFactory.getLog(CsvRsInputStream.class);

    public static final String NO_COMPRESSION = "NO_COMPRESSION";
    public static final String GZIP_COMPRESSION = "GZIP_COMPRESSION";
    public static final String ZIP_COMPRESSION = "ZIP_COMPRESSION";

    private ZipOutputStream zipOS;
    private GZIPOutputStream gzipOS;
    private ByteArrayOutputStream buffer;

    private CsvWriter csvWriter;
    private ResultSet rs;
    private ResultSetMetaData md;
    private int columnCount;
    private boolean noMoreRows = false;
    private boolean closeRsWhenDone = true;
    private boolean rsClosed = false;
    private boolean eof = false;

    public CsvRsInputStream(ResultSet rs, List cols, String encoding) throws SQLException, IOException,
    UnsupportedEncodingException {
        this(rs, cols, true, encoding, NO_COMPRESSION, null);
    }

    public CsvRsInputStream(ResultSet rs, List cols, boolean closeRsWhenDone, String encoding, String compression,
            String zipFileName)
    throws SQLException, IOException, UnsupportedEncodingException {
        this.closeRsWhenDone = closeRsWhenDone;
        this.rs = rs;
        this.md = rs.getMetaData();
        this.columnCount = md.getColumnCount();

        if (log.isDebugEnabled()) {
            log.debug("Using encoding: " + encoding);
        }

        this.buffer = new ByteArrayOutputStream();
        if (GZIP_COMPRESSION.equals(compression)) {
            this.gzipOS = new GZIPOutputStream(buffer);
            this.csvWriter = new CsvWriter(new OutputStreamWriter(gzipOS, encoding));
        } else if (ZIP_COMPRESSION.equals(compression)) {
            this.zipOS = new ZipOutputStream(buffer);
            ZipEntry zipEntry = new ZipEntry(zipFileName);
            zipEntry.setMethod(ZipEntry.DEFLATED);
            zipOS.putNextEntry(zipEntry);
            this.csvWriter = new CsvWriter(new OutputStreamWriter(zipOS, encoding));
        } else {
            this.csvWriter = new CsvWriter(new OutputStreamWriter(buffer, encoding));
        }

        // lets buffer the header now....
        if (cols == null) {
            int columnCount = md.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String colName = md.getColumnName(i);
                if (log.isDebugEnabled()) {
                    log.debug("Writing column header: " + colName);
                }
                csvWriter.writeField(colName);
            }
        } else {
            for (int i=0; i < cols.size(); i++) {
                String colName = cols.get(i).toString();
                if (log.isDebugEnabled()) {
                    log.debug("Writing column header: " + colName);
                }
                csvWriter.writeField(colName);
            }
        }
        csvWriter.endRecord();
        csvWriter.flush();
    }

    public final int read() throws IOException {
        byte[] bytes = new byte[1];
        int numRead = 0;
        while (numRead != -1 && numRead != 1) {
            numRead = read(bytes, 0, 1);
        }
        if (numRead == -1) {
            return -1;
        } else {
            return bytes[0];
        }
    }

    public final int read(byte[] buf, int off, int len) throws IOException {
        if (eof) {
            return -1;
        }

        // lets try to fill at least 1/2 the bytes requested
        // more than len/2 runs the risk of incurring the expensive write-back operation below too often
        while (buffer.size() <= len/2 && !noMoreRows) {
            try {
                if(log.isDebugEnabled()) {
                    log.debug("calling nextLine(); buffer size: " + buffer.size());
                }
                nextLine();
                csvWriter.flush();
            } catch (SQLException e) {
                throw new IOException(e.getMessage());
            }
        }

        if (buffer.size() == 0) {
            if (gzipOS != null) {
                // gzip doesn't like to "flush", but it will "finish"
                gzipOS.finish();
                csvWriter.flush();
                if (buffer.size() == 0 ) {
                    eof = true;
                    return -1;
                }
                log.debug("There were more bytes after finish()!");
            } else if (zipOS != null) {
                // zip doesn't like to "flush", but it will "finish"
                zipOS.finish();
                csvWriter.flush();
                if (buffer.size() == 0 ) {
                    eof = true;
                    return -1;
                }
                log.debug("There were more bytes after finish()!");
            } else {
                return -1;
            }
        }

        // copy buffer to array
        int numRead;
        int bufferSize = buffer.size();
        if (len < bufferSize) {
            numRead = len;
        } else {
            numRead = bufferSize;
        }

        if (numRead > 0) {
            byte[] bufferBytes = buffer.toByteArray();
            for (int i = off; i < off + numRead; i++) {
                buf[i] = bufferBytes[i - off];
            }

            // expensive, but necessary: delete out the head end of the buffer
            buffer.reset();
            log.debug("max bytes to read / numread: " + len + "/" + numRead);
            if (numRead < bufferSize) {
                log.debug("Storing back bytes:" + (bufferSize - numRead));
                buffer.write(bufferBytes, numRead, bufferSize - numRead);
            }
        }
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
        if(noMoreRows || !rs.next()) {
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
                log.warn("Exception closing ResultSet", e);
            }
        }
    }

}
