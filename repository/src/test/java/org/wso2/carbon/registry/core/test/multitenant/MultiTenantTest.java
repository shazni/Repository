/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.registry.core.test.multitenant;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.exceptions.RepositoryResourceNotFoundException;
import org.wso2.carbon.repository.core.EmbeddedRepositoryService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

public class MultiTenantTest extends BaseTestCase {

    protected static EmbeddedRepositoryService embeddedRegistryService = null;

    @BeforeTest
    public void setUp() {
        super.setUp();
        
        if (embeddedRegistryService != null) {
            return;
        }
        
        try {
            embeddedRegistryService = ctx.getEmbeddedRepositoryService();
            RealmUnawareRegistryCoreServiceComponent comp =
                    new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);

        } catch (RepositoryException e) {
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testVirtualRoots() throws Exception {
    	Repository registry1 = embeddedRegistryService.getRepository("admin", MultitenantConstants.SUPER_TENANT_ID);
        Resource r = registry1.newResource();
        
        registry1.put("/test", r);

        r = registry1.get("/");
        r.addProperty("name", "value");
        registry1.put("/", r);

        Repository registry2 = embeddedRegistryService.getRepository("admin", 1);
        r = registry2.get("/");
        Properties p = r.getProperties();
        
        Assert.assertEquals(p.size(), 0, "The properties in the second registry should be 0");

        boolean notExist = false;
        
        try {
            registry2.get("/test");
        } catch (RepositoryResourceNotFoundException e) {
            notExist = true;
        }
        
        Assert.assertTrue(notExist, "The /test should be null in the second registry");

        Repository registry3 = embeddedRegistryService.getRepository("don1", MultitenantConstants.SUPER_TENANT_ID);
        r = registry3.get("/");
        Assert.assertEquals(r.getProperty("name"), "value", "The property name should be value");

        String[] children = (String[]) r.getContent();
        Assert.assertEquals(children[0], "/test", "child should be /test");
    }
}
