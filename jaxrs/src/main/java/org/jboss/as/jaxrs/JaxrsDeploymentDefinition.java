/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.jaxrs;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class JaxrsDeploymentDefinition extends SimpleResourceDefinition {

    public static final JaxrsDeploymentDefinition DEPLOYMENT_INSTANCE = new JaxrsDeploymentDefinition(true);
    public static final JaxrsDeploymentDefinition SUBSYSTEM_INSTANCE = new JaxrsDeploymentDefinition(false);

    private boolean showResources;
    private JaxrsDeploymentDefinition(boolean showResources) {
         super(JaxrsExtension.SUBSYSTEM_PATH, JaxrsExtension.getResolver(), JaxrsSubsystemAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
         this.showResources = showResources;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        if(showResources) {
            resourceRegistration.registerOperationHandler(ShowJaxrsResourcesHandler.DEFINITION, new ShowJaxrsResourcesHandler());
        }
    }

}