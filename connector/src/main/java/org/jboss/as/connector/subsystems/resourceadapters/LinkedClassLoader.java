/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.resourceadapters;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.jboss.modules.Module;

/**
 * LinkedClassLoader is used to delegate class loading to other ClassLoaders.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
public class LinkedClassLoader extends ClassLoader {

    private final Set<ClassLoader> classLoaders;

    private final boolean parentFirst;

    /**
     * create an instance.
     *
     * @param classLoaders The extra class loaders to delegate.
     */
    public LinkedClassLoader(Set<ClassLoader> classLoaders) {
        this(classLoaders, true);
    }

    /**
     * create an instance.
     *
     * @param parent, the parent ClassLoader, default to Caller ModuleClassLoader.
     * @param classLoaders The extra class loaders to delegate.
     */
    public LinkedClassLoader(ClassLoader parent, Set<ClassLoader> classLoaders) {
        this(parent, classLoaders, true);
    }

    /**
     * create an instance.
     *
     * @param classLoaders The extra class loaders to delegate.
     * @param parentFirst Whether loads from parent first.
     */
    public LinkedClassLoader(Set<ClassLoader> classLoaders, boolean parentFirst) {
        this(Module.getCallerModule().getClassLoader(), classLoaders, parentFirst);
    }

    /**
     * create an instance
     * @param parent, the parent ClassLoader, default to Caller ModuleClassLoader.
     * @param classLoaders The extra class loaders to delegate.
     * @param parentFirst Whether loads from parent first.
     */
    public LinkedClassLoader(ClassLoader parent, Set<ClassLoader> classLoaders, boolean parentFirst) {
        super(parent);
        this.classLoaders = classLoaders;
        this.parentFirst = parentFirst;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> theClass = null;
        if (parentFirst) {
            try {
                theClass = super.loadClass(name);
                return theClass;
            } catch(ClassNotFoundException ce){
                // ignore it
            }
        }
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                try {
                    theClass = cl.loadClass(name);
                    return theClass;
                } catch(ClassNotFoundException ce){
                    // ignore it
                }
            }
        }
        if (!parentFirst) {
            try {
                theClass = super.loadClass(name);
                return theClass;
            } catch(ClassNotFoundException ce){
                // ignore it
            }
        }
        return theClass;
    }

    @Override
    public URL getResource(String name) {
        URL resource = null;
        if (parentFirst) {
            resource = super.getResource(name);
        }
        if (resource != null) {
            return resource;
        }
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                if ((resource = cl.getResource(name)) != null) {
                    return resource;
                }
            }
        }
        if (!parentFirst) {
            resource = super.getResource(name);
        }
        return resource;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = null;
        if (parentFirst) {
            is = super.getResourceAsStream(name);
        }
        if (is != null) {
            return is;
        }
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                if ((is = cl.getResourceAsStream(name)) != null) {
                    return is;
                }
            }
        }
        if (!parentFirst) {
            is = super.getResourceAsStream(name);
        }
        return is;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> combinedList = new ArrayList<URL>();
        if (parentFirst) {
            combinedList.addAll(Collections.list(super.getResources(name)));
        }
        if (this.classLoaders != null) {
            for(ClassLoader cl: classLoaders) {
                combinedList.addAll(Collections.list(cl.getResources(name)));
            }
        }
        if (!parentFirst) {
            combinedList.addAll(Collections.list(super.getResources(name)));
        }
        return Collections.enumeration(combinedList);
    }

}
