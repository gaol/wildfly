/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.web.session;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.runner.RunWith;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Tests on setting global tracking modes on the servlet container level
 *
 * @author Lin Gao
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(GlobalSessionTrackingModesAttributeTestCase.SetupTask.class)
public class GlobalSessionTrackingModesAttributeTestCase extends GlobalSessionTrackingModesTestCase {
    static class SetupTask implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final ModelNode op = Util.getWriteAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "undertow"),
                    PathElement.pathElement("servlet-container", "default")), "session-tracking-modes" , new ModelNode().add("COOKIE"));
            runOperationAndReload(op, managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            final ModelNode op = Util.getUndefineAttributeOperation(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, "undertow"),
                    PathElement.pathElement("servlet-container", "default")), "session-tracking-modes");
            runOperationAndReload(op, managementClient);
        }

    }

    static void runOperationAndReload(ModelNode operation, ManagementClient client) throws Exception {
        final ModelNode result = client.getControllerClient().execute(operation);
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(FAILURE_DESCRIPTION).toString();
            throw new RuntimeException(failureDesc);
        }
        if (!result.hasDefined(OUTCOME) || !SUCCESS.equals(result.get(OUTCOME).asString())) {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(OUTCOME));
        }
        ServerReload.reloadIfRequired(client);
    }

}
