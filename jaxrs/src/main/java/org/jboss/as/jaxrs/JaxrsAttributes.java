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
package org.jboss.as.jaxrs;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.dmr.ModelType;

/**
 * Class that stores the Attributes of JAX-RS subsystem.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */
public class JaxrsAttributes {

    public static final String SHOW_RESOURCES = "show-resources";

    public static final AttributeDefinition CLASSNAME = new SimpleAttributeDefinitionBuilder("resource-class",
            ModelType.STRING, true).setStorageRuntime().build();

    public static final AttributeDefinition PATH = new SimpleAttributeDefinitionBuilder("resource-path", ModelType.STRING, true)
            .setStorageRuntime().build();

    public static final AttributeDefinition METHOD = new SimpleAttributeDefinitionBuilder("jaxrs-resource-method",
            ModelType.STRING, false).setStorageRuntime().build();

    public static final AttributeDefinition METHODS = new SimpleListAttributeDefinition.Builder("resource-methods", METHOD)
            .setStorageRuntime().build();

    public static final AttributeDefinition CONSUME = new SimpleAttributeDefinitionBuilder("consume", ModelType.STRING, true)
            .setStorageRuntime().build();

    public static final AttributeDefinition CONSUMES = new SimpleListAttributeDefinition.Builder("consumes", CONSUME)
            .setStorageRuntime().build();

    public static final AttributeDefinition PRODUCE = new SimpleAttributeDefinitionBuilder("produce", ModelType.STRING, true)
            .setStorageRuntime().build();

    public static final AttributeDefinition PRODUCES = new SimpleListAttributeDefinition.Builder("produces", PRODUCE)
            .setStorageRuntime().build();

    public static final AttributeDefinition JAVA_METHOD = new SimpleAttributeDefinitionBuilder("java-method", ModelType.STRING,
            true).setStorageRuntime().build();

    public static final ObjectTypeAttributeDefinition RESOURCE_PATH = new ObjectTypeAttributeDefinition.Builder(
            "jaxrs-resource-path", PATH, CONSUMES, PRODUCES, JAVA_METHOD, METHODS).build();

    public static final ObjectListAttributeDefinition RESOURCE_PATHS = new ObjectListAttributeDefinition.Builder(
            "resource-paths", RESOURCE_PATH).build();

    public static final ObjectTypeAttributeDefinition SUB_RESOURCE_PATH = new ObjectTypeAttributeDefinition.Builder(
            "sub-jaxrs-resource-path", CLASSNAME, PATH, CONSUMES, PRODUCES, JAVA_METHOD, METHODS).build();

    public static final ObjectListAttributeDefinition SUB_RESOURCE_PATHS = new ObjectListAttributeDefinition.Builder(
            "sub-resource-paths", SUB_RESOURCE_PATH).build();

    public static final ObjectTypeAttributeDefinition JAXRS_RESOURCE = new ObjectTypeAttributeDefinition.Builder(
            "jaxrs-resource", CLASSNAME, RESOURCE_PATHS, SUB_RESOURCE_PATHS).setStorageRuntime().build();

}
