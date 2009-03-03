package org.anodyneos.jse.cron;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringHelper {

    private static final Log log = LogFactory.getLog(SpringHelper.class);

    private ClassPathXmlApplicationContext ctx;
    ArrayList configLocations = new ArrayList();

    public ApplicationContext getApplicationContext() {
        return ctx;
    }

    public void init() {
        log.info("Creating Spring application context");
        ctx = new ClassPathXmlApplicationContext(
                (String[]) configLocations.toArray(new String[configLocations.size()]));
    }

    public void addConfigLocation(String location) {
        log.info("Adding spring config: " + location);
        configLocations.add(location);
    }

    public Object getBean(String name) {
        return ctx.getBean(name);
    }

}
