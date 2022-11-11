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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static org.jboss.as.controller.client.helpers.ClientConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

/**
 * Tests on setting global tracking modes on the servlet container level
 *
 * @author Lin Gao
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(GlobalSessionTrackingModesTestCase.SetupTask.class)
public class GlobalSessionTrackingModesTestCase {
    static class SetupTask implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            final ModelNode op = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, "undertow.session.tracking-modes.default"));
            op.get(VALUE).set("COOKIE");
            runOperationAndReload(op, managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            final ModelNode op = Util.createRemoveOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, "undertow.session.tracking-modes.default"));
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

    @ArquillianResource
    @OperateOnDeployment("deployment1")
    private URL url1;

    @ArquillianResource
    @OperateOnDeployment("deployment2")
    private URL url2;

    @ArquillianResource
    @OperateOnDeployment("deployment3")
    private URL url3;

    @Deployment(name = "deployment1")
    public static Archive<?> deployment1() {
        // set COOKIE & URL in web.xml
        return ShrinkWrap.create(WebArchive.class, "deployment1.war")
                .addAsWebInfResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd\"\n" +
                        "         version=\"6.0\">\n" +
                        "    <session-config>\n" +
                        "        <tracking-mode>COOKIE</tracking-mode>\n" +
                        "        <tracking-mode>URL</tracking-mode>\n" +
                        "    </session-config>\n" +
                        "</web-app>"), "web.xml")
                .addClasses(SessionTrackModesTestServlet.class);
    }

    @Deployment(name = "deployment2")
    public static Archive<?> deployment2() {
        // set nothing, using the configuration from the setup task above
        return ShrinkWrap.create(WebArchive.class, "deployment2.war")
                .addClasses(SessionTrackModesTestServlet.class);
    }

    @Deployment(name = "deployment3")
    public static Archive<?> deployment3() {
        // set tracking modes to URL only using a ServletContextListener
        return ShrinkWrap.create(WebArchive.class, "deployment3.war")
                .addClasses(SessionTrackModesTestServlet.class, SessionTrackModesListener.class);
    }

    @Test
    public void testGlobalTrackingModes() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // in deployment1, web.xml takes precedence, COOKIE and URL are used
            HttpGet get = new HttpGet(url1 + "/session-track-modes");
            HttpResponse res = client.execute(get);
            String content1 = getContent(res);
            String sessionFromCookie1 = sessionIdFromCookie(res);
            Assert.assertNotNull(content1);
            Assert.assertNotNull(sessionFromCookie1);
            String sessionFromContent1 = sessionIdFromContent(content1);
            Assert.assertNotNull(sessionFromContent1);
            Assert.assertEquals(sessionFromContent1, sessionFromCookie1);

            // in deployment2, server side global configuration is used, it is COOKIE only in the expected setup
            get = new HttpGet(url2 + "/session-track-modes");
            res = client.execute(get);
            String content2 = getContent(res);
            String sessionFromCookie2 = sessionIdFromCookie(res);
            Assert.assertNotNull(content2);
            Assert.assertNotNull(sessionFromCookie2);
            Assert.assertEquals("/next", content2);
            String sessionFromContent2 = sessionIdFromContent(content2);
            Assert.assertNull(sessionFromContent2);

            // in deployment3, a ServletContextListener sets to use URL only for the deployment
            get = new HttpGet(url3 + "/session-track-modes");
            res = client.execute(get);
            String content3 = getContent(res);
            String sessionFromCookie3 = sessionIdFromCookie(res);
            Assert.assertNotNull(content3);
            Assert.assertNull(sessionFromCookie3);
            String sessionFromContent3 = sessionIdFromContent(content3);
            Assert.assertNotNull(sessionFromContent3);
        }
    }

    private String sessionIdFromCookie(HttpResponse res) {
        for (Header cookie : res.getHeaders("Set-Cookie")) {
            if (cookie.getValue().startsWith("JSESSIONID=")) {
                return cookie.getValue().split("=")[1].split("\\.")[0];
            }
        }
        return null;
    }

    private String sessionIdFromContent(String content) {
        final String keyword = "jsessionid=";
        final int idx = content.indexOf(keyword);
        if (idx != -1) {
            return content.substring(idx + keyword.length()).split("\\.")[0];
        }
        return null;
    }

    private String getContent(HttpResponse res) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.getEntity().getContent()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

}
