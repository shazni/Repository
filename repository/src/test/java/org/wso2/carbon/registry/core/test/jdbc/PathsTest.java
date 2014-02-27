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
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;

public class PathsTest extends BaseTestCase {
    protected static Repository registry = null;
    
    @BeforeTest
    public void setUp() {
        super.setUp();

        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getRepository("admin");
        } catch (RepositoryException e) {
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testGetOnPaths() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("some content");
        registry.put("/test/paths/r1", r1);

        Assert.assertTrue(registry.resourceExists("/test"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/test/"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/test/paths/r1"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/test/paths/r1/"), "Resource not found.");

        registry.get("/test");
        registry.get("/test/");
        registry.get("/test/paths/r1");
        registry.get("/test/paths/r1/");
    }

    @Test
    public void testColon() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("some content");
        registry.put("/con-delete/?aTest:/pp:", r1);

        Assert.assertTrue(registry.resourceExists("/con-delete"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/con-delete"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/con-delete/?aTest:/pp:"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/con-delete/?aTest:/pp:"), "Resource not found.");

        registry.get("/con-delete");
        registry.get("/con-delete/");
        registry.get("/con-delete/?aTest:/pp:");
        registry.get("/con-delete/?aTest:/pp:");
    }

    @Test
    public void testPutOnPaths() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("some content");
        registry.put("/test/paths2/r1", r1);

        Resource r2 = registry.newResource();
        r2.setContent("another content");
        registry.put("/test/paths2/r2/", r2);

        Collection c1 = registry.newCollection();
        registry.put("/test/paths2/c1", c1);

        Collection c2 = registry.newCollection();
        registry.put("/test/paths2/c2/", c2);

        Assert.assertTrue(registry.resourceExists("/test/paths2/r1/"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/test/paths2/r2"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/test/paths2/c1/"), "Resource not found.");
        Assert.assertTrue(registry.resourceExists("/test/paths2/c2"), "Resource not found.");
    }
}
