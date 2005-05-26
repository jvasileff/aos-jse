/*
 * Created on May 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.anodyneos.jse.cron.sampleJobs;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import javax.activation.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jvas
 */
public class CsvRsDataSource implements DataSource {

    private ResultSet rs;
    private String name;
    private List cols;
    private boolean closeRs;

    public static final Log log = LogFactory.getLog(CsvRsDataSource.class);

    public CsvRsDataSource(ResultSet rs, boolean closeRs, String name, List cols) {
        this.rs = rs;
        //this.query = query;
        this.name = name;
        this.cols = cols;
        this.closeRs=closeRs;
    }

    public String getContentType() {
        return "text/csv; charset=UTF-8";
    }

    public String getName() {
        return name;
    }

    public InputStream getInputStream() throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("getInputStream() called.");
        }
        try {
            return new ReaderInputStream(new CsvRsReader(rs, closeRs));
            //return new FileInputStream("/tmp/myfile");
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {}
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Readonly DataSource");
    }

}
