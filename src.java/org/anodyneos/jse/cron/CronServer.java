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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.anodyneos.jse.JseDateAwareJob;
import org.anodyneos.jse.JseException;
import org.anodyneos.jse.JseTimerService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 *  @version $Id: CronServer.java,v 1.2 2004-05-13 03:42:03 jvas Exp $
 */
public class CronServer {

    private static final Log log = LogFactory.getLog(CronServer.class);

    private static final String ELEMENT = "element";
    private static final String ATTRIBUTE = "attribute";

    private static final String E_SCHEDULE = "schedule";
    private static final String E_SPRING_CONTEXT = "spring-context";
    private static final String E_CONFIG = "config";
    private static final String E_JOB_GROUP = "job-group";
    private static final String E_JOB = "job";
    private static final String E_PROPERTY = "property";

    private static final String A_TIME_ZONE = "time-zone";
    private static final String A_CLASS_PATH_RESOURCE = "class-path-resource";
    private static final String A_MAX_CONCURRENT = "max-concurrent";
    private static final String A_NAME = "name";
    private static final String A_CLASS_NAME = "class";
    private static final String A_SPRING_BEAN = "spring-bean";
    private static final String A_SCHEDULE = "schedule";
    private static final String A_MAX_ITERATIONS = "max-iterations";
    private static final String A_MAX_QUEUE = "max-queue";
    private static final String A_NOT_BEFORE = "not-before";
    private static final String A_NOT_AFTER = "not-after";
    private static final String A_TYPE = "type";
    private static final String A_SYSTEM_PROPERTY = "system-property";
    private static final String A_ID = "id";
    private static final String A_REF_ID = "ref-id";


    private ArrayList timerServices = new ArrayList();
    private SpringHelper springHelper;

    public CronServer(InputSource source) throws JseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        // parse source, create and load timerServices
        Document doc = null;
        try {
            DocumentBuilder builder =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(source);
        } catch (Exception e) {
            throw new JseException("Cannot parse " + source + ".", e);
        }

        TimeZone defaultTimeZone = TimeZone.getDefault();

        // E_SCHEDULE
        Element sch = (Element) doc.getElementsByTagName(E_SCHEDULE).item(0);
        if (sch.hasAttribute(A_TIME_ZONE)) {
            defaultTimeZone = getTimeZone(sch.getAttribute(A_TIME_ZONE));
        }

        // E_SPRING_CONTEXT
        NodeList springNL = doc.getElementsByTagName(E_SPRING_CONTEXT);
        if (springNL.getLength() > 0) {
            springHelper = new SpringHelper();
            Element springContext = (Element) springNL.item(0);
            NodeList configs = springContext.getElementsByTagName(E_CONFIG);
            for(int i = 0; i < configs.getLength(); i++) {
                Element config = (Element) configs.item(i);
                if(config.hasAttribute(A_CLASS_PATH_RESOURCE)) {
                    springHelper.addConfigLocation(config.getAttribute(A_CLASS_PATH_RESOURCE));
                }
            }
            springHelper.init();
        }

        // E_JOB_GROUP
        NodeList jobGroups = doc.getElementsByTagName(E_JOB_GROUP);
        for(int i = 0; i < jobGroups.getLength(); i++) {
            Element jobGroup = (Element) jobGroups.item(i);
            NodeList jobs = jobGroup.getElementsByTagName(E_JOB);
            if(jobs.getLength() == 0) {
                continue;
            }
            JseTimerService service = new JseTimerService();
            String jobGroupName = null;
            timerServices.add(service);
            // A_MAX_CONCURRENT
            if(jobGroup.hasAttribute(A_MAX_CONCURRENT)) {
                service.setMaxConcurrent(Integer.parseInt(jobGroup.getAttribute(A_MAX_CONCURRENT)));
            }
            // A_NAME
            if(jobGroup.hasAttribute(A_NAME)) {
                jobGroupName = jobGroup.getAttribute(A_NAME);
            }
            // E_JOB
            for(int j = 0; j < jobs.getLength(); j++) {
                TimeZone timeZone = defaultTimeZone;
                Element job = (Element) jobs.item(j);
                // track progress for error reporting
                String propertyName = null;
                String jobName = "UNDEFINED";
                String springBean = null;
                String step = E_JOB;
                String type = ELEMENT;
                try {
                    Object obj;
                    boolean isSpringBean = false;

                    // A_NAME
                    step = A_NAME;
                    type = ATTRIBUTE;
                    if(job.hasAttribute(A_NAME)) {
                        jobName = job.getAttribute(A_NAME);
                    }
                    // A_SPRING_BEAN
                    step = A_SPRING_BEAN;
                    type = ATTRIBUTE;
                    if (job.hasAttribute(A_SPRING_BEAN)) {
                        obj = springHelper.getBean(job.getAttribute(A_SPRING_BEAN));
                        isSpringBean = true;
                    } else {
                        // A_CLASS_NAME
                        step = A_CLASS_NAME;
                        type = ATTRIBUTE;
                        obj = BeanUtil.getInstance(job.getAttribute(A_CLASS_NAME));
                    }

                    // A_MAX_ITERATIONS
                    step = A_MAX_ITERATIONS;
                    type = ATTRIBUTE;
                    int numIterations = -1;
                    if(job.hasAttribute(A_MAX_ITERATIONS)) {
                        numIterations = Integer.parseInt(job.getAttribute(A_MAX_ITERATIONS));
                    }
                    // A_MAX_QUEUE
                    step = A_MAX_QUEUE;
                    type = ATTRIBUTE;
                    int maxQueue = -1;
                    if(job.hasAttribute(A_MAX_QUEUE)) {
                        maxQueue = Integer.parseInt(job.getAttribute(A_MAX_QUEUE));
                    }
                    // A_NOT_BEFORE
                    step = A_NOT_BEFORE;
                    type = ATTRIBUTE;
                    Date notBefore = null;
                    if(job.hasAttribute(A_NOT_BEFORE)) {
                        notBefore = dateFormat.parse(job.getAttribute(A_NOT_BEFORE));
                    }
                    // A_NOT_AFTER
                    step = A_NOT_AFTER;
                    type = ATTRIBUTE;
                    Date notAfter = null;
                    if(job.hasAttribute(A_NOT_AFTER)) {
                        notAfter = dateFormat.parse(job.getAttribute(A_NOT_AFTER));
                    }
                    // A_TIME_ZONE
                    step = A_TIME_ZONE;
                    type = ATTRIBUTE;
                    if (job.hasAttribute(A_TIME_ZONE)) {
                        timeZone = getTimeZone(job.getAttribute(A_TIME_ZONE));
                    }
                    // A_SCHEDULE
                    step = A_SCHEDULE;
                    type = ATTRIBUTE;
                    CronSchedule schedule =
                            new CronSchedule(job.getAttribute(A_SCHEDULE), timeZone, numIterations, maxQueue, notBefore, notAfter);

                    if (! isSpringBean) {
                        // E_PROPERTY
                        step = E_PROPERTY;
                        type = ELEMENT;
                        NodeList properties = job.getElementsByTagName(E_PROPERTY);
                        for(int p = 0; p < properties.getLength(); p++) {
                            Element property = (Element) properties.item(p);
                            property.normalize(); // join adjacent text nodes
                            propertyName = property.getAttribute(A_NAME);
                            String propType = property.getAttribute(A_TYPE);
                            String systemProperty = property.getAttribute(A_SYSTEM_PROPERTY);
                            String refId = property.getAttribute(A_REF_ID);

                            propertyName = "".equals(propertyName) ? null : propertyName;
                            propType = "".equals(propType) ? null : propType;
                            systemProperty = "".equals(systemProperty) ? null : systemProperty;
                            refId = "".equals(refId) ? null : refId;

                            String value = null;

                            if (null != systemProperty) {
                                // try to get value from system property;
                                value = System.getProperty(systemProperty);
                            }
                            if (null == value) {
                                // or get value from content of element
                                value = getTextContent(property);
                            }

                            BeanUtil.set(obj, propertyName, value, propType);
                        }
                    }
                    log.info("Adding job " + jobGroupName + "/" + jobName);
                    if (obj instanceof CronJob) {
                        ((CronJob) obj).setCronContext(new CronContext(jobGroupName, jobName, schedule));
                    }
                    if (obj instanceof JseDateAwareJob) {
                        service.createTimer((JseDateAwareJob) obj, schedule);
                    } else if (obj instanceof Runnable) {
                        service.createTimer((Runnable) obj, schedule);
                    } else {
                        throw new JseException("Job must implement Runnable or JseDateAwareJob");
                    }
                } catch (Exception exception) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Skipping job " + jobGroupName + "/" + jobName);
                    if (ELEMENT.equals(type)) {
                        sb.append(" - exception while processing element '" + step + "'");
                    } else {
                        sb.append(" - exception while processing attribute '" + step + "'");
                    }
                    log.error(sb.toString(), exception);
                }
            }
        }
    }

    public void start() {
        Iterator it = timerServices.iterator();
        while(it.hasNext()) {
            JseTimerService service = (JseTimerService) it.next();
            log.info("Starting service thread: " + service.getName());
            service.start();
        }
    }

    public static void main(String[] args) throws Exception {
        InputSource source = new InputSource(args[0]);
        CronServer server = new CronServer(source);
        server.start();
    }

    public static final String getTextContent(Element el) {
        StringBuffer sb = new StringBuffer();
        NodeList nodes = el.getChildNodes();
        for(int i = 0; i < nodes.getLength(); i++) {
            short type = nodes.item(i).getNodeType();
            if(type == Node.TEXT_NODE || type == Node.CDATA_SECTION_NODE) {
                sb.append(nodes.item(i).getNodeValue());
            }
        }
        return sb.toString();
    }

    protected TimeZone getTimeZone(String tzs) throws JseException {
        String[] allTZ = TimeZone.getAvailableIDs();
        TimeZone tz = null;
        for(int i = 0; i < allTZ.length; i++) {
            if(allTZ[i].equals(tzs)) {
                tz = TimeZone.getTimeZone(tzs);
                break;
            }
        }
        if (null == tz) {
            throw new JseException("TimeZone not available: " + tzs);
        }
        return tz;
    }
}
