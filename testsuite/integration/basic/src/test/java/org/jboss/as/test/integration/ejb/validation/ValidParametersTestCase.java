package org.jboss.as.test.integration.ejb.validation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class ValidParametersTestCase {

    @ArquillianResource
    private URL url;

    static Client client;

    @BeforeClass
    public static void setUpClient() {
        client = ClientBuilder.newClient();
    }

    @AfterClass
    public static void close() {
        client.close();
    }

    @ApplicationPath("")
    public static class TestApplication extends Application {
    }

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "sample.war")
                .addPackage(HttpRequest.class.getPackage())
                .addClasses(DummyEjb.class,
                        EchoChamber.class,
                        DummySubclass.class,
                        DummyAbstractClass.class,
                        DummyClass.class,
                        DummyFlag.class,
                        TestApplication.class);
        return archive;
    }

//    @Test
//    public void VerifyValidationOnSubclassThatExtendsAbstractClass() throws Exception {
//        WebTarget target = client.target(url.toURI().toString() + "/sample");
//        ResteasyWebTarget rtarget = (ResteasyWebTarget) target;
//        EchoChamber customerResource = rtarget.proxy(EchoChamber.class);
//        DummyFlag dummyFlag = rtarget.proxy(DummyFlag.class);
//
//        // Create subclass of DummyAbstractClass with valid values
//        DummySubclass validDummySubclass = new DummySubclass();
//        validDummySubclass.setDirection("north");
//        validDummySubclass.setSpeed(10);
//
//        // Create subclass of DummyAbstractClass with an invalid value (speed should be greater than 0)
//        DummySubclass invalidDummySubclass = new DummySubclass();
//        invalidDummySubclass.setDirection("north");
//        invalidDummySubclass.setSpeed(0);
//
//        Response response = customerResource.validateEchoChamberThroughAbstractClass(validDummySubclass);
//        assertEquals(200, response.getStatus());
//        assertEquals("north", response.readEntity(String.class));
//        // Reset flag
//        dummyFlag.clearExecution();
//
//        Response response2 = customerResource.validateEchoChamberThroughAbstractClass(invalidDummySubclass);
//        log.info("The response code was: {}", response2.getStatus());
//        log.info("The response headers were: {}", response2.getHeaders());
//        log.info("Response body: {}", response2.readEntity(String.class));
//
//        // Verify that we received a Bad Request Code from HTTP
//        assertTrue(String.format("Return code should either be 400 or 500. It was %d", response2.getStatus()), 400 == response2.getStatus() || 500 == response2.getStatus());
//
//        // Verify that the service call has not been executed (flag set to false)
//        assertFalse("Executed flag should be false", dummyFlag.getFlag());
//    }


    @Test
    public void VerifyValidationOnConcreteClass() throws Exception {
        WebTarget target = client.target(url.toURI().toString());
        String valid = "{\"speed\" : 5, \"direction\" : \"north\"}";
        String invalid = "{\"speed\" : 0}";

        // reset the status
        Response response = target.path("/status/reset").request().get();
        assertEquals(200, response.getStatus());
        response = target.path("/status").request().get();
        assertFalse(response.readEntity(Boolean.TYPE));

        response = target.path("/echo").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(valid));
        assertEquals(200, response.getStatus());
        assertEquals("north", response.readEntity(String.class));

        response = target.path("/status").request().get();
        assertTrue(response.readEntity(Boolean.TYPE));

        // reset again
        response = target.path("/status/reset").request().get();
        assertEquals(200, response.getStatus());
        response = target.path("/status").request().get();
        assertFalse(response.readEntity(Boolean.TYPE));

        response = target.path("/echo/").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(invalid));
        assertTrue(String.format("Return code should either be 400 or 500, it was %d", response.getStatus()), 400 == response.getStatus() || 500 == response.getStatus());

        response = target.path("/status").request().get();
        assertFalse(response.readEntity(Boolean.TYPE));

    }
}
