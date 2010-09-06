/*
 * Created on Apr 7, 2005
 */
package org.anodyneos.jse.cron.sampleJobs;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.anodyneos.jse.cron.CronContext;
import org.anodyneos.jse.cron.CronJob;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author jvas
 */
public class URLMonitor implements CronJob {

    private static final Log log = LogFactory.getLog(URLMonitor.class);

    private URL targetURL;
    private String matchText;
    private String smtpServer;
    private List<InternetAddress> toList = new ArrayList<InternetAddress>();
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 5000;

    // timeout alert {0} = URL, {1} = exception msg, {2} full exception
    // ioexception: {0} = URL, {1} = exception msg, {2} full exception
    // mismatch: {0} = URL, {1} = matchText, {2} actualText

    private InternetAddress from;

    private String timeoutSubject = "timeout occured accessing {0}";
    private String timeoutBody = "Timeout occured accessing {0}\n{1}\n{2}";

    private String ioExceptionSubject = "IOException occured accessing {0}";
    private String ioExceptionBody = "IOException occured accessing {0}\n{1}\n{2}";

    private String stringNotFoundSubject = "string not found accessing {0}";
    private String stringNotFoundBody = "string not found accessing {0}\nExpected Text: {1}";

    @Override
    public void setCronContext(CronContext ctx) {
        // noop
    }

    @Override
    public void run(Date runDate) {
        log.info("urlmon run: "+runDate);
        String content = null;
        try {
            URLConnection conn = targetURL.openConnection();
            conn.setConnectTimeout(connectTimeoutMillis);
            conn.setReadTimeout(readTimeoutMillis);
            conn.connect();
            Reader reader = new InputStreamReader(targetURL.openConnection().getInputStream());
            StringBuffer sb = new StringBuffer();
            int i;
            for (char[] buffer = new char[256]; -1 != (i = reader.read(buffer));) {
                sb.append(buffer, 0, i);
            }
            reader.close();
            content = sb.toString();
        } catch (SocketTimeoutException e) {
            Object[] params =  new Object[] {targetURL, e.toString(), getStackTrace(e)};
            String subject = MessageFormat.format(timeoutSubject, params);
            String body = MessageFormat.format(timeoutBody, params);
            sendError(subject, body);
            return;
        } catch (IOException e) {
            Object[] params =  new Object[] {targetURL, e.toString(), getStackTrace(e)};
            String subject = MessageFormat.format(ioExceptionSubject, params);
            String body = MessageFormat.format(ioExceptionBody, params);
            sendError(subject, body);
            return;
        }

        if (-1 == content.indexOf(matchText)) {
            Object[] params =  new Object[] {targetURL, matchText, content};
            String subject = MessageFormat.format(stringNotFoundSubject, params);
            String body = MessageFormat.format(stringNotFoundBody, params);
            sendError(subject, body);
        }
    }

    /**
     * @param targetURL The targetURL to set.
     */
    public void setTargetURL(String targetURL) throws MalformedURLException {
        this.targetURL = new URL(targetURL);
    }

    /**
     * @param bodyText The bodyText to set.
     */
    public void setMatchText(String matchText) {
        this.matchText = matchText;
    }

    /**
     * @param emailList The emailList to set.
     */
    public void setTo(String email) throws AddressException {
        InternetAddress ia = new InternetAddress(email);
        this.toList.add(ia);
    }

    /**
     * @param smtpServer The smtpServer to set.
     */
    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    /**
     * @param from The from to set.
     */
    public void setFrom(String from) throws AddressException {
        this.from = new InternetAddress(from);
    }
    /**
     * @param ioExceptionBody The ioExceptionBody to set.
     */
    public void setIoExceptionBody(String ioExceptionBody) {
        this.ioExceptionBody = ioExceptionBody;
    }
    /**
     * @param ioExceptionSubject The ioExceptionSubject to set.
     */
    public void setIoExceptionSubject(String ioExceptionSubject) {
        this.ioExceptionSubject = ioExceptionSubject;
    }
    /**
     * @param stringNotFoundBody The stringNotFoundBody to set.
     */
    public void setStringNotFoundBody(String stringNotFoundBody) {
        this.stringNotFoundBody = stringNotFoundBody;
    }
    /**
     * @param stringNotFoundSubject The stringNotFoundSubject to set.
     */
    public void setStringNotFoundSubject(String stringNotFoundSubject) {
        this.stringNotFoundSubject = stringNotFoundSubject;
    }
    /**
     * @param timeoutBody The timeoutBody to set.
     */
    public void setTimeoutBody(String timeoutBody) {
        this.timeoutBody = timeoutBody;
    }
    /**
     * @param timeoutSubject The timeoutSubject to set.
     */
    public void setTimeoutSubject(String timeoutSubject) {
        this.timeoutSubject = timeoutSubject;
    }
    /**
     * @param connectTimeoutMillis The connectTimeoutMillis to set.
     */
    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }
    /**
     * @param readTimeoutMillis The readTimeoutMillis to set.
     */
    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }
    private static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.getBuffer().toString();
    }

    private void sendError(String subject, String body) {
        // create some properties and get the default Session
        Properties props = new java.util.Properties();
        props.put("mail.smtp.host", smtpServer);
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(false);

        log.info("-----");
        log.info("SUBJECT:" + subject);
        log.info("BODY:" + body);
        log.info("-----");

        try {
            Message message = new MimeMessage(session);
            message.setSentDate(new java.util.Date());
            message.setSubject(subject);
            message.setText(body);

            message.addFrom(new InternetAddress[] {from});
            for (int i=0; i < toList.size(); i++) {
                message.addRecipient(Message.RecipientType.TO, toList.get(i));
            }

            message.saveChanges();
            Transport.send(message);

        } catch (MessagingException e) {
            log.error("Failed to send email", e);
        }
    }

}
