/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.microprofile.health;

import java.util.function.Supplier;

import io.smallrye.health.ResponseProvider;
import io.smallrye.health.SmallRyeHealthReporter;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.network.SocketBinding;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class HealthReporterService implements Service<SmallRyeHealthReporter> {

    private SmallRyeHealthReporter healthReporter;

    private final Supplier<SocketBinding> httpSocketBinding;

    static void install(OperationContext context) {
        final ServiceBuilder<?> builder = context.getServiceTarget().addService(MicroProfileHealthSubsystemDefinition.HEALTH_REPORTER_SERVICE);
        Supplier<SocketBinding> httpSocketBinding = builder.requires(context.getCapabilityServiceName("org.wildfly.network.socket-binding", SocketBinding.class, "http"));
        HealthReporterService healthReporter = new HealthReporterService(httpSocketBinding);
        builder.setInstance(healthReporter);
        builder.install();
    }

    private HealthReporterService(Supplier<SocketBinding> httpSocketBinding) {
        this.httpSocketBinding = httpSocketBinding;
    }

    @Override
    public void start(StartContext context) {
        HealthCheckResponse.setResponseProvider(new ResponseProvider());
        this.healthReporter = new SmallRyeHealthReporter();
        this.healthReporter.addHealthCheck(new HealthCheck() {
            private static final String CHECK_NAME = "http-check";

            @Override
            public HealthCheckResponse call() {
                HealthCheckResponseBuilder hcb = HealthCheckResponse.named(CHECK_NAME);
                SocketBinding httpSB = httpSocketBinding.get();
                if (httpSB.isBound()) {
                    hcb = hcb.up().withData("port", httpSB.getAbsolutePort());
                } else {
                    hcb = hcb.down();
                }
                return hcb.build();
            }
        });
    }

    @Override
    public void stop(StopContext context) {
        this.healthReporter = null;
        HealthCheckResponse.setResponseProvider(null);
    }

    @Override
    public SmallRyeHealthReporter getValue() {
        return healthReporter;
    }
}
