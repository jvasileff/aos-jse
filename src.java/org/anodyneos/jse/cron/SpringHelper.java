package org.anodyneos.jse.cron;

import java.util.ArrayList;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringHelper {

    private ClassPathXmlApplicationContext ctx;
    ArrayList configLocations = new ArrayList();

    public ApplicationContext getApplicationContext() {
        return ctx;
    }

    public void init() {
        ctx = new ClassPathXmlApplicationContext(
                (String[]) configLocations.toArray(new String[configLocations.size()]));
        ctx.refresh();
    }

    public void addConfigLocation(String location) {
        configLocations.add(location);
    }

    public Object getBean(String name) {
        return ctx.getBean(name);
    }

}
