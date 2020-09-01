package org.jboss.as.test.integration.ejb.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@Stateless
public class DummyEjb implements EchoChamber {
    private static final Logger log = LoggerFactory.getLogger(DummyEjb.class);

    @Inject
    private DummyFlag dummyFlag;

    @Override
    public Response validateEchoChamberThroughAbstractClass(DummySubclass payload) {
        log.info("Executing service call for direction {} and speed {} ", payload.getDirection(), payload.getSpeed());
        dummyFlag.setFlag(true);
        return Response.ok(payload.getDirection()).build();
    }

    @Override
    public Response validateEchoChamberThroughClass(DummyClass payload) {
        log.info("Executing service call for direction {} and speed {} ", payload.getDirection(), payload.getSpeed());
        dummyFlag.setFlag(true);
        return Response.ok(payload.getDirection()).build();
    }
}
