/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.jsf.deployment;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.PrintWriter;
import java.io.StringWriter;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(NonExistingJsfImplDeploymentTestCase.SetupTask.class)
public class NonExistingJsfImplDeploymentTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(testable = false, managed = false, name = "test_jar")
    public static Archive<?> deploy() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addClass(NonExistingJsfImplDeploymentTestCase.class);
        return archive;
    }

    private static final PathAddress RA_ADDRESS = PathAddress.pathAddress(ModelDescriptionConstants.SUBSYSTEM, "jsf");

    @Test
    public void testDeploymentDoesntFailBecauseOfNPE() throws Exception {
        try {
            deployer.deploy("test_jar");
        } catch (Exception e) {
            if (e instanceof DeploymentException) {
                // awkward way to check if the failure is caused by NPE, but e.getCause() doesn't work
                StringWriter out = new StringWriter();
                e.printStackTrace(new PrintWriter(out));
                String stackTrace = out.getBuffer().toString();

                if (stackTrace.contains("Caused by: java.lang.NullPointerException")) {
                    throw e;
                }
            } else {
                throw e;
            }
        }
    }

    static class SetupTask extends SnapshotRestoreSetupTask {
        @Override
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            ModelControllerClient mcc = client.getControllerClient();
            ModelNode addRaOperation = Operations.createWriteAttributeOperation(RA_ADDRESS.toModelNode(), "default-jsf-impl-slot", "idontexist");
            addRaOperation.get("archive").set("wf-ra-ely-security.rar");
            addRaOperation.get("transaction-support").set("NoTransaction");
            ModelNode response = mcc.execute(addRaOperation);

            ServerReload.executeReloadAndWaitForCompletion(client);
        }
    }
}