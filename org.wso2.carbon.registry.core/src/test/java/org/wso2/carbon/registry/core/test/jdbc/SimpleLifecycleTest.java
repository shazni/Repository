/*
 * Copyright (c) 2007, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.registry.core.test.jdbc;

import org.wso2.carbon.repository.Aspect;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.registry.core.caching.CacheBackedRegistry;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.RequestContext;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistry;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class SimpleLifecycleTest extends BaseTestCase {

    protected static Registry registry = null;
//    protected static EmbeddedRegistryService embeddedRegistryService = null;

    public static class SimpleLifecycle extends Aspect {
        public static final String STATE_PROP = "SimpleLifecycle.state";
        public static final String INIT = "init";
        public static final String FINAL = "final";

        public static final String ACTION = "transition";

        public void associate(Resource resource, Registry registry) throws RepositoryException {
            resource.setProperty(STATE_PROP, INIT);
        }

        public void dissociate(RequestContext context) {
            context.getResource().removeProperty(STATE_PROP);
        }

        public void invoke(RequestContext context, String action) throws RepositoryException {
            if (!ACTION.equals(action)) {
                throw new RepositoryException("Wrong action");
            }

            Resource r = context.getResource();
            String state = r.getProperty(STATE_PROP);
            if (state == null) {
                throw new RepositoryException("No state property");
            }

            if (!INIT.equals(state)) {
                throw new RepositoryException("Invalid state '" + state + "'");
            }

            r.setProperty(STATE_PROP, FINAL);
            context.getRegistry().put(r.getPath(), r);
        }

        static String [] actions = new String [] { ACTION };

        public String [] getAvailableActions(RequestContext context) {
            Resource r = context.getResource();
            String state = r.getProperty(STATE_PROP);

            if (INIT.equals(state)) {
                return actions;
            }

            return null;
        }
    }

    public void setUp() {
        super.setUp();
//        if (embeddedRegistryService != null) {
//            return;
//        }
        try {
//        	embeddedRegistryService.init(ctx);
//            embeddedRegistryService = ctx.getEmbeddedRegistryService();
            RealmUnawareRegistryCoreServiceComponent comp =
                    new RealmUnawareRegistryCoreServiceComponent();
            comp.setRealmService(ctx.getRealmService());
            comp.registerBuiltInHandlers(embeddedRegistryService);
            
            // get the realm config to retrieve admin username, password
            RealmConfiguration realmConfig = ctx.getRealmService().getBootstrapRealmConfiguration();
            registry = embeddedRegistryService.getUserRegistry(
                realmConfig.getAdminUserName(), realmConfig.getAdminPassword());
        } catch (RepositoryException e) {
                fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    public void testSimpleLifecycle() throws Exception {
        final String RESOURCE = "/r1";
        final String LIFECYCLE = "simpleLifecycle";

//        RegistryContext context = ((EmbeddedRegistry) registry).getRegistryContext();
    	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
    	
        registryContext.selectDBConfig("h2-db");
        registryContext.addAspect(LIFECYCLE, new SimpleLifecycle(), MultitenantConstants.SUPER_TENANT_ID);

        String [] aspects = registry.getAvailableAspects();
        assertTrue(aspects.length > 0);
        boolean found = false;
        for (String aspect : aspects) {
            if (aspect.equals(LIFECYCLE)) {
                found = true;
            }
        }
        assertTrue("Lifecycle not found in available aspects", found);

        Resource resource = registry.newResource();
        resource.setDescription("My thing");
        registry.put(RESOURCE, resource);

        registry.associateAspect(RESOURCE, LIFECYCLE);

        resource = registry.get(RESOURCE);
        assertNotNull(resource);
        String propValue = resource.getProperty(SimpleLifecycle.STATE_PROP);
        assertNotNull(propValue);
        assertEquals("Wrong initial state!", SimpleLifecycle.INIT, propValue);

        String [] actions = registry.getAspectActions(RESOURCE, LIFECYCLE);
        assertNotNull("No available actions", actions);
        assertEquals("Wrong # of available actions", 1, actions.length);
        assertEquals("Wrong available action", SimpleLifecycle.ACTION, actions[0]);

        registry.invokeAspect(RESOURCE, LIFECYCLE, SimpleLifecycle.ACTION);

        resource = registry.get(RESOURCE);

        // OK, now we should be in the next state
        propValue = resource.getProperty(SimpleLifecycle.STATE_PROP);
        assertNotNull(propValue);
        assertEquals("Wrong state!", SimpleLifecycle.FINAL, propValue);
    }
}
