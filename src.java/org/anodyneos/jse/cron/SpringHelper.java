package org.anodyneos.jse.cron;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class SpringHelper {

    private static final Log log = LogFactory.getLog(SpringHelper.class);

    private GenericApplicationContext ctx;

    public ApplicationContext getApplicationContext() {
        return ctx;
    }

    public SpringHelper() {
        log.info("Creating Spring application context");
        ctx = new GenericApplicationContext();
    }

    public void init() {
        ctx.refresh();
    }

    public void addXmlClassPathConfigLocation(String location) {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
        reader.loadBeanDefinitions(new ClassPathResource(location));
    }

    public Object getBean(String name) {
        return ctx.getBean(name);
    }

    public void registerBean(String name, BeanDefinition bd) {
        ctx.registerBeanDefinition(name, bd);
    }

    public boolean containsBean(String name) {
        return ctx.containsBean(name);
    }

}
