/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */

@RunWith(Arquillian.class)
@RunAsClient
public class JaxrsDeploymentTestCase extends ContainerResourceMgmtTestBase {

    private static final String DEPLOY_NAME = "jaxrs-app.war";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOY_NAME);
        war.addPackage(HttpRequest.class.getPackage());
        war.addClasses(JaxrsDeploymentTestCase.class,
                HelloWorldResource.class,
                HelloWorldPathApplication.class,
                ContainerResourceMgmtTestBase.class,
                AbstractMgmtTestBase.class);
        return war;
    }

    @Test
    public void testShowResource() throws Exception {
        ModelNode result = getModelControllerClient().execute(showResourceOperation(DEPLOY_NAME)).get("result");
        Assert.assertTrue(result.isDefined());

        System.out.println(result);
        ModelNode methodGetMsg = result.get(0);
        Assert.assertTrue(methodGetMsg.isDefined());
        Assert.assertEquals("org.jboss.as.test.integration.jaxrs.packaging.war.HelloWorldResource", methodGetMsg.get("resource-class").asString());
        Assert.assertEquals("helloworld", methodGetMsg.get("resource-path").asString());
        Assert.assertEquals("GET /jaxrs-app/hellopath/helloworld - org.jboss.as.test.integration.jaxrs.packaging.war.HelloWorldResource.getMessage() : java.lang.String", methodGetMsg.get("resource-methods").get(0).asString());

        ModelNode methodSayHello = result.get(1);
        Assert.assertTrue(methodSayHello.isDefined());
        Assert.assertEquals("org.jboss.as.test.integration.jaxrs.packaging.war.HelloWorldResource", methodSayHello.get("resource-class").asString());
        Assert.assertEquals("helloworld/hello", methodSayHello.get("resource-path").asString());
        Assert.assertEquals("GET /jaxrs-app/hellopath/helloworld/hello - org.jboss.as.test.integration.jaxrs.packaging.war.HelloWorldResource.sayHello(arg0 : java.lang.String, arg1 : boolean) : java.lang.String", methodSayHello.get("resource-methods").get(0).asString());

        ModelNode methodSayHelloGood = result.get(2);
        Assert.assertTrue(methodSayHelloGood.isDefined());
        Assert.assertEquals("org.jboss.as.test.integration.jaxrs.packaging.war.HelloWorldResource", methodSayHelloGood.get("resource-class").asString());
        Assert.assertEquals("helloworld/helloGood", methodSayHelloGood.get("resource-path").asString());
        Assert.assertEquals("GET /jaxrs-app/hellopath/helloworld/helloGood - org.jboss.as.test.integration.jaxrs.packaging.war.HelloWorldResource.sayHelloGood(arg0 : boolean) : java.lang.String", methodSayHelloGood.get("resource-methods").get(0).asString());

    }

    private ModelNode showResourceOperation(String deployName) {
        ModelNode address = new ModelNode().add(DEPLOYMENT, deployName).add(SUBSYSTEM, "jaxrs");
        address.protect();
        ModelNode operation = new ModelNode();
        operation.get(OP).set("show-resources");
        operation.get(OP_ADDR).set(address);
        return operation;
    }

}
