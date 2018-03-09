/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.jca;

import static org.jboss.as.controller.client.helpers.ClientConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;

import com.google.common.collect.Iterables;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DataSourceClassInfoTestCase extends ContainerResourceMgmtTestBase {
    private static final String DRIVER_WITH_WRONG_DS_CLASS_NAME = "h2-wrong-ds-class";

    private ModelNode getDsClsInfoOperation(String driverName) {
        ModelNode driverAddress = new ModelNode();
        driverAddress.add("subsystem", "datasources");
        driverAddress.add("jdbc-driver", driverName);
        ModelNode op = Operations.createReadResourceOperation(driverAddress);
        op.get(INCLUDE_RUNTIME).set(true);
        return op;
    }

    @Test
    public void testGetDsClsInfo() throws Exception {
        ModelNode operation = getDsClsInfoOperation("h2");
        ModelNode result = getManagementClient().getControllerClient().execute(operation);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result").get("datasource-class-info");
        Assert.assertNotNull(dsInfoList);
        ModelNode dsInfo = dsInfoList.get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());

    }

    @Test
    public void testGetDsClsInfoByReadAttribute() throws Exception {
        ModelNode driverAddress = new ModelNode();
        driverAddress.add("subsystem", "datasources");
        driverAddress.add("jdbc-driver", "h2");
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(driverAddress);
        op.get(OP).set(READ_ATTRIBUTE_OPERATION);
        op.get("name").set("datasource-class-info");

        ModelNode result = getManagementClient().getControllerClient().execute(op);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result");
        Assert.assertNotNull(dsInfoList);
        ModelNode dsInfo = dsInfoList.get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());

    }

    @Test
    public void testInstalledDriverList() throws Exception {
        // installed-drivers-list
        ModelNode subsysAddr = new ModelNode();
        subsysAddr.add("subsystem", "datasources");
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(subsysAddr);
        op.get(OP).set("installed-drivers-list");

        ModelNode result = getManagementClient().getControllerClient().execute(op);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result");
        Assert.assertNotNull(dsInfoList);
        ModelNode dsInfo = dsInfoList.get(0).get("datasource-class-info").get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());
    }

    @Test
    public void testGetInstalledDriver() throws Exception {
        // get-installed-driver(driver-name=h2)
        ModelNode subsysAddr = new ModelNode();
        subsysAddr.add("subsystem", "datasources");
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(subsysAddr);
        op.get(OP).set("get-installed-driver");
        op.get("driver-name").set("h2");

        ModelNode result = getManagementClient().getControllerClient().execute(op);

        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
        ModelNode dsInfoList = result.get("result");
        Assert.assertNotNull(dsInfoList);
        ModelNode dsInfo = dsInfoList.get(0).get("datasource-class-info").get(0).get("org.h2.jdbcx.JdbcDataSource");
        Assert.assertNotNull(dsInfo);

        Assert.assertEquals("java.lang.String", dsInfo.get("description").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("user").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("url").asString());
        Assert.assertEquals("java.lang.String", dsInfo.get("password").asString());
        Assert.assertEquals("int", dsInfo.get("loginTimeout").asString());
    }

    @Test
    public void testGetDsClsInfoWithWrongDataSourceClass() throws Exception {
        ModelNode driverWithWrongDsClsAddr = new ModelNode();
        driverWithWrongDsClsAddr.add("subsystem", "datasources");
        driverWithWrongDsClsAddr.add("jdbc-driver", DRIVER_WITH_WRONG_DS_CLASS_NAME);

        addH2DriverWithWrongDsClass(driverWithWrongDsClsAddr);

        ModelNode op = Operations.createReadResourceOperation(driverWithWrongDsClsAddr);
        op.get(INCLUDE_RUNTIME).set(true);

        try {
            ModelNode result = getManagementClient().getControllerClient().execute(op);
            Assert.assertNotNull(result);
            Assert.assertEquals("failed", result.get("outcome").asString());
            Assert.assertTrue(result.hasDefined("failure-description"));
        } finally {
            removeDriver(driverWithWrongDsClsAddr);
        }
    }

    @Test
    public void testGetDsClsInfoByReadAttributeWithWrongDataSourceClass() throws Exception {
        ModelNode driverWithWrongDsClsAddr = new ModelNode();
        driverWithWrongDsClsAddr.add("subsystem", "datasources");
        driverWithWrongDsClsAddr.add("jdbc-driver", DRIVER_WITH_WRONG_DS_CLASS_NAME);

        addH2DriverWithWrongDsClass(driverWithWrongDsClsAddr);

        ModelNode op = Operations.createReadAttributeOperation(driverWithWrongDsClsAddr, "datasource-class-info");

        try {
            ModelNode result = getManagementClient().getControllerClient().execute(op);
            Assert.assertNotNull(result);
            Assert.assertEquals("failed", result.get("outcome").asString());
            Assert.assertTrue(result.hasDefined("failure-description"));
        } finally {
            removeDriver(driverWithWrongDsClsAddr);
        }
    }

    private void addH2DriverWithWrongDsClass(ModelNode driverAddress) throws Exception {
        ModelNode op = Operations.createAddOperation(driverAddress);
        op.get("driver-name").set(Iterables.getLast(driverAddress.asList()).get("jdbc-driver"));
        op.get("driver-module-name").set("com.h2database.h2");
        op.get("driver-xa-datasource-class-name").set("non.existing.class");

        ModelNode result = getManagementClient().getControllerClient().execute(op);
        Assert.assertNotNull(result);
        Assert.assertEquals("success", result.get("outcome").asString());
    }

    private void removeDriver(ModelNode driverAddress) throws Exception {
        ModelNode op = Operations.createRemoveOperation(driverAddress);
        getManagementClient().getControllerClient().execute(op);
    }
}