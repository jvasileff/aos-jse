package org.anodyneos.jse.cron;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultValidationEventHandler implements ValidationEventHandler {

    Log log = LogFactory.getLog(DefaultValidationEventHandler.class);

    @Override
    public boolean handleEvent(ValidationEvent ve) {
        // ignore warnings
        if (ve.getSeverity() != ValidationEvent.WARNING) {
            ValidationEventLocator vel = ve.getLocator();
            log.warn("Line:Col[" + vel.getLineNumber() +
                ":" + vel.getColumnNumber() +
                "]:" + ve.getMessage());
        }
        return true;
    }

}
