package org.jboss.as.test.integration.ejb.validation;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/echo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface EchoChamber {
    @POST
    @Path("/echoThroughAbstract")
    Response validateEchoChamberThroughAbstractClass(@Valid DummySubclass payload);

    @POST
    Response validateEchoChamberThroughClass(@Valid DummyClass payload);
}
