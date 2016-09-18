/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jaxrs;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.jaxrs.JaxrsAttributes.CLASSNAME;
import static org.jboss.as.jaxrs.JaxrsAttributes.CONSUMES;
import static org.jboss.as.jaxrs.JaxrsAttributes.JAVA_METHOD;
import static org.jboss.as.jaxrs.JaxrsAttributes.JAXRS_RESOURCE;
import static org.jboss.as.jaxrs.JaxrsAttributes.METHODS;
import static org.jboss.as.jaxrs.JaxrsAttributes.PATH;
import static org.jboss.as.jaxrs.JaxrsAttributes.PRODUCES;
import static org.jboss.as.jaxrs.JaxrsAttributes.SHOW_RESOURCES;
import static org.wildfly.extension.undertow.DeploymentDefinition.CONTEXT_ROOT;
import static org.wildfly.extension.undertow.DeploymentDefinition.SERVER;
import static org.wildfly.extension.undertow.DeploymentDefinition.VIRTUAL_HOST;
import io.undertow.servlet.api.ThreadSetupAction.Handle;
import io.undertow.servlet.handlers.ServletHandler;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceLocatorInvoker;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceLocator;
import org.jboss.resteasy.spi.metadata.ResourceMethod;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentService;

/**
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 *
 */
public class ShowJaxrsResourcesHandler implements OperationStepHandler {
    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(SHOW_RESOURCES,
            JaxrsExtension.getResolver("deployment"))
            .setReadOnly()
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyParameters(JAXRS_RESOURCE).build();


    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        //Getting Undertow deployment Model to access Servlet informations.
        final ModelNode subModel = context.readResourceFromRoot(address.subAddress(0, address.size() - 1).append(
                SUBSYSTEM, UndertowExtension.SUBSYSTEM_NAME), false).getModel();
        final String host = VIRTUAL_HOST.resolveModelAttribute(context, subModel).asString();
        final String contextPath = CONTEXT_ROOT.resolveModelAttribute(context, subModel).asString();
        final String server = SERVER.resolveModelAttribute(context, subModel).asString();

        final ServiceController<?> controller = context.getServiceRegistry(false).getService(UndertowService.deploymentServiceName(server, host, contextPath));
        final UndertowDeploymentService deploymentService = (UndertowDeploymentService) controller.getService();
        Servlet resteasyServlet = null;
        Handle handle = deploymentService.getDeployment().getThreadSetupAction().setup(null);
        try {
            for (Map.Entry<String, ServletHandler> servletHandler : deploymentService.getDeployment().getServlets().getServletHandlers().entrySet()) {
                if (HttpServletDispatcher.class.isAssignableFrom(servletHandler.getValue().getManagedServlet().getServletInfo().getServletClass())) {
                    resteasyServlet = (Servlet) servletHandler.getValue().getManagedServlet().getServlet().getInstance();
                    break;
                }
            }
            if (resteasyServlet != null) {
                final Collection<String> servletMappings = resteasyServlet.getServletConfig().getServletContext().getServletRegistration(resteasyServlet.getServletConfig().getServletName()).getMappings();
                final ResourceMethodRegistry registry = (ResourceMethodRegistry) ((HttpServletDispatcher) resteasyServlet).getDispatcher().getRegistry();
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        if (registry != null) {
                            final List<JaxrsResourceDescription> jaxrsResDescriptions = scanEndpoints(contextPath, servletMappings, registry, null);
                            Collections.sort(jaxrsResDescriptions);
                            final ModelNode response = new ModelNode();
                            if (jaxrsResDescriptions.size() > 0) {
                                for (JaxrsResourceDescription resDes: jaxrsResDescriptions) {
                                    response.add(resDes.toModelNode());
                                }
                            }
                            context.getResult().set(response);
                        }
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        } catch (ServletException ex) {
            throw new RuntimeException(ex);
        } finally {
            handle.tearDown();
        }
    }

    private List<JaxrsResourceDescription> scanEndpoints(String contextPath, Collection<String> servletMappings,
            ResourceMethodRegistry registry, String query) throws OperationFailedException {
        List<JaxrsResourceDescription> jaxrsRess = new ArrayList<>();
        for (Map.Entry<String, List<ResourceInvoker>> resource : registry.getBounded().entrySet()) {
            String mapping = resource.getKey();
            List<ResourceInvoker> resouceInvokers = resource.getValue();
            for (ResourceInvoker resourceInvoker : resouceInvokers) {
                if (ResourceMethodInvoker.class.isAssignableFrom(resourceInvoker.getClass())) {
                    ResourceMethodInvoker methodInvoker = (ResourceMethodInvoker) resourceInvoker;
                    JaxrsResourceDescription jaxrsRes = resMethodInvokerDescription(methodInvoker, contextPath, mapping, servletMappings);
                    jaxrsRess.add(jaxrsRes);
                } else if (ResourceLocatorInvoker.class.isAssignableFrom(resourceInvoker.getClass())) {
                    // NOTE: sub-resource locator can only analysis the case when the declared returned type is the same as the concrete returned type.
                    ResourceLocatorInvoker locatorInvoker = (ResourceLocatorInvoker) resourceInvoker;
                    Method invokerMethod = locatorInvoker.getMethod();
                    Class<?> cls = invokerMethod.getReturnType();
                    ResourceClass resClass =  ResourceBuilder.locatorFromAnnotations(cls);
                    addSubJaxrsResourceLocators(jaxrsRess, resClass, contextPath, mapping, servletMappings, query);
                } else {
                    throw JaxrsLogger.JAXRS_LOGGER.unknownResourceInvokerType(resourceInvoker.getClass().getName());
                }
            }
        }
        return jaxrsRess;
    }

    private JaxrsResourceDescription resMethodInvokerDescription(ResourceMethodInvoker methodInvoker, String contextPath,
            String mapping, Collection<String> servletMappings) {
        JaxrsResourceDescription jaxrsRes = new JaxrsResourceDescription();
        jaxrsRes.consumeTypes = methodInvoker.getConsumes();
        jaxrsRes.contextPath = contextPath;
        jaxrsRes.httpMethods = methodInvoker.getHttpMethods();
        jaxrsRes.method = methodInvoker.getMethod();
        jaxrsRes.produceTypes = methodInvoker.getProduces();
        jaxrsRes.resourceClass = methodInvoker.getResourceClass();
        jaxrsRes.resourcePath = mapping;
        jaxrsRes.servletMappings = servletMappings;
        addMethodParameters(jaxrsRes, methodInvoker.getMethod());
        return jaxrsRes;
    }

    private void addSubJaxrsResourceLocators(List<JaxrsResourceDescription> jaxrsRess, ResourceClass resClass,
            String contextPath, String mapping, Collection<String> servletMappings, String query) {
        if (resClass.getClazz().equals(Object.class)) {
            return;
        }
        for (ResourceMethod resMethod : resClass.getResourceMethods()) {
            JaxrsResourceDescription jaxrsRes = new JaxrsResourceDescription();
            jaxrsRes.consumeTypes = resMethod.getConsumes();
            jaxrsRes.contextPath = contextPath;
            jaxrsRes.httpMethods = resMethod.getHttpMethods();
            jaxrsRes.method = resMethod.getMethod();
            jaxrsRes.produceTypes = resMethod.getProduces();
            jaxrsRes.resourceClass = resClass.getClazz();
            String resPath = new StringBuilder(mapping).append("/").append(resMethod.getFullpath()).toString().replace("//", "/");
            jaxrsRes.resourcePath = resPath;
            jaxrsRes.servletMappings = servletMappings;
            addMethodParameters(jaxrsRes, resMethod.getMethod());
            jaxrsRess.add(jaxrsRes);
        }
        for (ResourceLocator resLocator: resClass.getResourceLocators()) {
            ResourceClass subResClass = ResourceBuilder.locatorFromAnnotations(resLocator.getReturnType());
            String subMapping = new StringBuilder(mapping).append("/").append(resLocator.getFullpath()).toString().replace("//", "/");
            addSubJaxrsResourceLocators(jaxrsRess, subResClass, contextPath, subMapping, servletMappings, query);
        }
    }

    private void addMethodParameters(JaxrsResourceDescription jaxrsRes, Method method) {
        for (Parameter param: method.getParameters()) {
            ParamInfo paramInfo = new ParamInfo();
            paramInfo.cls = param.getType();
            paramInfo.defaultValue = null;
            paramInfo.name = null;
            paramInfo.type = null;
            Annotation annotation;
            if ((annotation = param.getAnnotation(PathParam.class)) != null) {
                PathParam pathParam = (PathParam)annotation;
                paramInfo.name = pathParam.value();
                paramInfo.type = "@" + PathParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(QueryParam.class)) != null) {
                QueryParam queryParam = (QueryParam)annotation;
                paramInfo.name = queryParam.value();
                paramInfo.type = "@" + QueryParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(HeaderParam.class)) != null) {
                HeaderParam headerParam = (HeaderParam)annotation;
                paramInfo.name = headerParam.value();
                paramInfo.type = "@" + HeaderParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(CookieParam.class)) != null) {
                CookieParam cookieParam = (CookieParam)annotation;
                paramInfo.name = cookieParam.value();
                paramInfo.type = "@" + CookieParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(MatrixParam.class)) != null) {
                MatrixParam matrixParam = (MatrixParam)annotation;
                paramInfo.name = matrixParam.value();
                paramInfo.type = "@" + MatrixParam.class.getSimpleName();
            } else if ((annotation = param.getAnnotation(FormParam.class)) != null) {
                FormParam formParam = (FormParam)annotation;
                paramInfo.name = formParam.value();
                paramInfo.type = "@" + FormParam.class.getSimpleName();
            }
            if (paramInfo.name == null) {
                paramInfo.name = param.getName();
            }
            if ((annotation = param.getAnnotation(DefaultValue.class)) != null) {
                DefaultValue defaultValue = (DefaultValue)annotation;
                paramInfo.defaultValue = defaultValue.value();
            }
            jaxrsRes.parameters.add(paramInfo);
        }
    }
    private static class JaxrsResourceDescription implements Comparable<JaxrsResourceDescription> {

        private Class<?> resourceClass;
        private String resourcePath;
        private Method method;
        private List<ParamInfo> parameters = new ArrayList<>();
        private Set<String> httpMethods = Collections.emptySet();
        private MediaType[] consumeTypes;
        private MediaType[] produceTypes;

        private Collection<String> servletMappings = Collections.emptyList();
        private String contextPath;

        @Override
        public int compareTo(JaxrsResourceDescription other) {
            int result = this.resourcePath.compareTo(other.resourcePath);
            if (result == 0) {
                result = this.resourceClass.getCanonicalName().compareTo(other.resourceClass.getCanonicalName());
            }
            if (result == 0) {
                result = this.method.getName().compareTo(other.method.getName());
            }
            return result;
        }

        ModelNode toModelNode() {
            ModelNode node = new ModelNode();
            node.get(CLASSNAME.getName()).set(resourceClass.getCanonicalName());
            node.get(PATH.getName()).set(resourcePath);
            ModelNode consumeNode = node.get(CONSUMES.getName());
            if (consumeTypes != null && consumeTypes.length > 0) {
                for (MediaType consume: consumeTypes) {
                    consumeNode.add(consume.toString());
                }
            }
            ModelNode produceNode = node.get(PRODUCES.getName());
            if (produceTypes != null && produceTypes.length > 0) {
                for (MediaType produce: produceTypes) {
                    produceNode.add(produce.toString());
                }
            }
            node.get(JAVA_METHOD.getName()).set(formatJavaMethod(resourceClass, method));
            for (final String servletMapping: servletMappings) {
                for (final String httpMethod : httpMethods) {
                    node.get(METHODS.getName()).add(httpMethod + " " + formatPath(servletMapping, contextPath, resourcePath));
                }
            }
            return node;
        }

        private String formatPath(String servletMapping, String ctxPath, String resPath) {
            StringBuilder sb = new StringBuilder();
            String servletPath = servletMapping.replaceAll("\\*", "");
            if(servletPath.charAt(0) == '/') {
                servletPath = servletPath.substring(1);
            }
            sb.append(ctxPath).append('/').append(servletPath).append(resPath);
            return sb.toString().replace("//", "/");
        }

        private String formatJavaMethod(Class<?> cls, Method md) {
            StringBuilder sb = new StringBuilder();
            sb.append(md.getReturnType().getCanonicalName())
              .append(" ")
              .append(cls.getCanonicalName())
              .append(".")
              .append(md.getName())
              .append('(');
            int i = 0;
            for (ParamInfo param: this.parameters) {
                if (param.type != null) {
                    sb.append(param.type).append(" ");
                }
                sb.append(param.cls.getCanonicalName())
                  .append(" ")
                  .append(param.name);
                if (param.defaultValue != null) {
                    sb.append(" = '");
                    sb.append(param.defaultValue);
                    sb.append("'");
                }
                if (++i < this.parameters.size()) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            return sb.toString();
        }
    }

    private static class ParamInfo {
        private String name; // if specified by @XXParam, it is the value() of that param.
        private Class<?> cls;
        private String type; // @PathParam, or @CookieParam, or QueryParam, etc
        private String defaultValue;
    }
}
