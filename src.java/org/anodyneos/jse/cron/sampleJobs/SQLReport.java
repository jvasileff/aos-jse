package org.anodyneos.jse.cron.sampleJobs;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.anodyneos.jse.cron.CronContext;
import org.anodyneos.jse.cron.CronJob;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jvas
 */
public class SQLReport implements CronJob {

    private static final Log log = LogFactory.getLog(SQLReport.class);

    private CronContext ctx;

    // JDBC
    private String jdbcUser;
    private String jdbcPassword;
    private String jdbcURL;
    private String jdbcDriver;

    // The query
    /**
     * The contents of the query element OR the contents of the file pointed to
     * by the queryFile element.
     */
    private String query;

    /**
     * The name of the attachment, for example, reportOutput.csv.
     */
    private String attachmentName;
    private String mimeType = "application/csv";
    private String charset = "ISO-8859-1";

    // Email
    private String mailHost;

    /**
     * RFC 822 Email addresses, for example: Alfred Neuman <Neuman@BBN-TENEXA>
     */
    private List toAddressList = new ArrayList();
    private List ccAddressList = new ArrayList();
    private List bccAddressList = new ArrayList();
    private InternetAddress fromAddress;
    private String subject;
    private String messageText;

    /**
     * In cron.xml, this is a comma separated list of column display names.
     */
    private List columnNames;

    /**
     * Default constructor, required by Jse/Cron.
     */
    public SQLReport() {
        super();
    }

    public void setCronContext(CronContext ctx) {
        this.ctx = ctx;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

    /**
     * @param attachmentName The attachmentName to set.
     */
    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }
    /**
     * @param fromAddress The fromAddress to set.
     */
    public void setFromAddress(String fromAddress) throws AddressException {
        this.fromAddress = new InternetAddress(fromAddress);
    }
    /**
     * @param jdbcDriver The jdbcDriver to set.
     */
    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }
    /**
     * @param jdbcPassword The jdbcPassword to set.
     */
    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }
    /**
     * @param jdbcURL The jdbcURL to set.
     */
    public void setJdbcURL(String jdbcURL) {
        this.jdbcURL = jdbcURL;
    }
    /**
     * @param jdbcUser The jdbcUser to set.
     */
    public void setJdbcUser(String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }
    /**
     * @param messageText The messageText to set.
     */
    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }
    /**
     * @param query The query to set.
     */
    public void setQuery(String query) {
        this.query = query;
    }
    /**
     * @param subject The subject to set.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCharset() {
        return charset;
    }
    public void setCharset(String charset) {
        this.charset = charset;
    }
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setQueryFile(String queryFile) throws FileNotFoundException, IOException {
        Reader in  = new FileReader(queryFile);
        StringBuffer buf = new StringBuffer();
        char[] chars = new char[1024];
        int numRead = 0;
        while (-1 != (numRead = in.read(chars))) {
            buf.append(chars, 0, numRead);
        }
        in.close();
        query = buf.toString();
    }

    public void setToAddress(String rfc822Address) throws AddressException {
        toAddressList.add(new InternetAddress(rfc822Address));
    }

    public void setColumnNames(String s) {
        if (log.isDebugEnabled()) {
            log.debug("setColumnNames(\"" + s + "\")");
        }
        if (null == columnNames) {
            columnNames = new ArrayList();
        }
        StringTokenizer st = new StringTokenizer(s, ",");
        while (st.hasMoreTokens()) {
            String tok = st.nextToken().trim();
            if (log.isDebugEnabled()) {
                log.debug("Adding column '" + tok + "'");
            }
            this.columnNames.add(tok);
        }
    }

    /**
     * @see org.anodyneos.jse.JseDateAwareJob#run(java.util.Date)
     */
    public void run(Date runDate) {
        if (log.isDebugEnabled()) {
            log.debug("running query: " + query);
        }
        sendMessage();
    }

    private void sendMessage() {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", mailHost);
            Session session = Session.getDefaultInstance(props, null);
            session.setDebug(false);

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(fromAddress);
            msg.setRecipients(Message.RecipientType.TO,
                    (InternetAddress[])toAddressList.toArray(new InternetAddress[toAddressList.size()]));
            msg.setRecipients(Message.RecipientType.CC,
                    (InternetAddress[])ccAddressList.toArray(new InternetAddress[ccAddressList.size()]));
            msg.setRecipients(Message.RecipientType.BCC,
                    (InternetAddress[])bccAddressList.toArray(new InternetAddress[bccAddressList.size()]));
            msg.setSubject(subject);

            Multipart multiPart = new MimeMultipart();
            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setText(messageText);
            multiPart.addBodyPart(bodyPart);

            Connection con = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                con = getConnection();
                ps = con.prepareStatement(query);
                rs = ps.executeQuery();

                CsvRsDataSource ds = new CsvRsDataSource(rs, false, attachmentName, mimeType, charset, columnNames);
                bodyPart = new MimeBodyPart();
                bodyPart.setFileName(ds.getName());
                bodyPart.setDisposition(MimeBodyPart.ATTACHMENT);
                DataHandler dh = new DataHandler(ds);
                bodyPart.setDataHandler(dh);
                multiPart.addBodyPart(bodyPart);
                // The following line is very important; without it, the stream is read twice by Transport.send(),
                // once to determine and encoding, and once to craft the attachment.
                // Note: 7bit is no good - can't handle lines longer than 1000 chars
                //       base64 is ok, but not human readable - better for binary
                //       quoted-printable works pretty well
                //bodyPart.setHeader("Content-Transfer-Encoding", "7bit");
                bodyPart.setHeader("Content-Transfer-Encoding", "base64");
                //bodyPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
                msg.setContent(multiPart);
                msg.setSentDate(new Date());
                if (log.isDebugEnabled()) {
                    log.debug("Sending message...");
                }
                Transport.send(msg);
            } finally {
                close(rs);
                close(ps);
                close(con);
            }
        } catch (SQLException e) {
            log.error("Cannot send message", e);
        } catch (MessagingException e) {
            log.error("Cannot send message", e);
        }
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName(jdbcDriver).newInstance();
        } catch (InstantiationException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
        Connection con = DriverManager.getConnection(jdbcURL, jdbcUser, jdbcPassword);
        return con;
    }

    public static final void close(ResultSet obj) {
        if (null != obj) { try { obj.close(); } catch ( SQLException e ) {} }
    }

    public static final void close(PreparedStatement obj) {
        if (null != obj) { try { obj.close(); } catch ( SQLException e ) {} }
    }

    public static final void close(Connection obj) {
        if (null != obj) { try { obj.close(); } catch ( SQLException e ) {} }
    }
}
