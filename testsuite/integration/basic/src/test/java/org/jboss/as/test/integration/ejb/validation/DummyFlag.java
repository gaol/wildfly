package org.jboss.as.test.integration.ejb.validation;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Startup
@Singleton
@Path("/status")
public class DummyFlag {

    private boolean flag;

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    @GET
    @Path("/reset")
    public Response reset() {
        flag = false;
        return Response.ok().build();
    }

    @GET
    public boolean getFlag() {
        return flag;
    }
}
