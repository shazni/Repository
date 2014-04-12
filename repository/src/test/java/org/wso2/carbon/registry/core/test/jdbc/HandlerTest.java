/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Filter;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.handlers.HandlerLifecycleManager;
import org.wso2.carbon.repository.core.handlers.builtin.URLMatcher;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashSet;
import java.util.Set;

public class HandlerTest extends BaseTestCase {

    /**
     * Registry instance for use in tests. Note that there should be only one Registry instance in a
     * JVM.
     */
    protected static Repository registry = null;
    protected static Repository systemRegistry = null;

    @BeforeTest
    public void setUp() {
        super.setUp();

        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getRepository("admin");
            systemRegistry = embeddedRegistryService.getSystemRepository();
        } catch (RepositoryException e) {
        	Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }
    
    @Test
    public void testCommitHandlerExecution() throws Exception {

        final class TestData {
            private boolean handlerExecuted = false;

            public boolean isHandlerExecuted() {
                return handlerExecuted;
            }

            public void setHandlerExecuted(boolean handlerExecuted) {
                this.handlerExecuted = handlerExecuted;
            }
        }

        final TestData testData = new TestData();

        Handler handler = new Handler() {
            public void put(HandlerContext requestContext) throws RepositoryException {
                testData.setHandlerExecuted(true);
            }
        };

        URLMatcher filter = new URLMatcher();
        filter.setPattern(".*");
        Set<Filter> filterSet = new LinkedHashSet<Filter>();
        filterSet.add(filter);
        handler.setFilters(filterSet);

        CurrentContext.setCallerTenantId(MultitenantConstants.SUPER_TENANT_ID);
        
        try {
            registry.getRepositoryService().addHandler(null, handler, HandlerLifecycleManager.COMMIT_HANDLER_PHASE);
        } finally {
            CurrentContext.removeCallerTenantId();
        }

        Resource r1 = registry.newResource();
        String str = "My Content";
        r1.setContentStream(new ByteArrayInputStream(str.getBytes()));
        registry.put("/c1/c2/c3/c4/r1", r1);

        Assert.assertTrue(testData.isHandlerExecuted());
    }
    
    @Test
    public void testRollbackHandlerExecution() throws Exception {

        final class TestData {
            private boolean handlerExecuted = false;

            public boolean isHandlerExecuted() {
                return handlerExecuted;
            }

            public void setHandlerExecuted(boolean handlerExecuted) {
                this.handlerExecuted = handlerExecuted;
            }
        }

        final TestData testData = new TestData();

        Handler handler = new Handler() {
            public void put(HandlerContext requestContext) throws RepositoryException {
                testData.setHandlerExecuted(true);
            }
        };

        Handler handler1 = new Handler() {
            public void put(HandlerContext requestContext) throws RepositoryException {
                throw new RepositoryException("Sample Test Failure");
            }
        };

        URLMatcher filter = new URLMatcher();
        filter.setPattern(".*");
        Set<Filter> filterSet = new LinkedHashSet<Filter>();
        filterSet.add(filter);
        handler1.setFilters(filterSet);

        registry.getRepositoryService().addHandler(null, handler1);

        filter = new URLMatcher();
        filter.setPattern(".*");
        filterSet = new LinkedHashSet<Filter>();
        filterSet.add(filter);
        handler.setFilters(filterSet);

        CurrentContext.setCallerTenantId(MultitenantConstants.SUPER_TENANT_ID);
        
        try {
            registry.getRepositoryService().addHandler(null, handler, HandlerLifecycleManager.ROLLBACK_HANDLER_PHASE);
        } finally {
            CurrentContext.removeCallerTenantId();
        }

        Resource r1 = registry.newResource();
        String str = "My Content";
        r1.setContentStream(new ByteArrayInputStream(str.getBytes()));
        
        try {
            registry.put("/c1/c2/c3/c4/r1", r1);
        } catch (RepositoryException ignored) {
        	registry.getRepositoryService().removeHandler(handler1, null);
        }
        
        Assert.assertTrue(testData.isHandlerExecuted());
    }
}
