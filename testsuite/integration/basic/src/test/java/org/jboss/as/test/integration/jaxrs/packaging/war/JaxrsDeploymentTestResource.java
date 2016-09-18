/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jaxrs.packaging.war;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("message")
@Produces({"text/plain"})
public class JaxrsDeploymentTestResource {

    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @GET
    @POST
    public String getMessage() {
        return "Hello World!";
    }

    @GET
    @Path("/sayHello")
    public String sayHello(@QueryParam("name") String name, @QueryParam("good") boolean good) {
        if (good) {
            return "Yes, I am good " + name;
        }
        return "No, I am not good " + name;
    }

    @GET
    @Path("/sayHello/{good}")
    @Produces({MediaType.APPLICATION_XML})
    public String sayHelloGood(@PathParam("good") boolean good) {
        if (good) {
            return "Yes, I am good ";
        }
        return "No, I am not good ";
    }

    @POST
    @Path("/postMessage")
    public String postMessagae(String message) {
        return message + " posted";
    }

    @PUT
    @Path("/putMessage")
    @Consumes({"text/plain"})
    public String putMessagae(String message) {
        return message + " put";
    }

    @GET
    @Path("/subMessage")
    public SubJaxrsDeploymentTestResource subMessage() {
        return new SubJaxrsDeploymentTestResource();
    }

}
