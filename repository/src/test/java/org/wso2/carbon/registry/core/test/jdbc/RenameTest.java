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

public class RenameTest extends BaseTestCase {
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
    public void testRootLevelResourceRename() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "rename");
        r1.setContent("some text");
        registry.put("/rename1", r1);

        registry.rename("/rename1", "/rename2");

        boolean failed = false;
        
        try {
            registry.get("/rename1");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Resource should not be accessible from the old path after renaming.");

        Resource newR1 = registry.get("/rename2");
        Assert.assertEquals(newR1.getProperty("test"), "rename", "Resource should contain a property with name test and value rename.");
    }

    @Test
    public void testGeneralResourceRename() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "rename");
        r1.setContent("some text");
        registry.put("/tests/rename1", r1);

        registry.rename("/tests/rename1", "rename2");

        boolean failed = false;
        
        try {
            registry.get("/tests/rename1");
        } catch (RepositoryException e) {
            failed = true;
        }
        Assert.assertTrue(failed, "Resource should not be accessible from the old path after renaming.");

        Resource newR1 = registry.get("/tests/rename2");
        Assert.assertEquals(newR1.getProperty("test"), "rename", "Resource should contain a property with name test and value rename.");
    }

    @Test
    public void testRootLevelCollectionRename() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "rename");
        r1.setContent("some text");
        
        registry.put("/rename3/c1/dummy", r1);

        registry.rename("/rename3", "rename4");

        boolean failed = false;
        
        try {
            registry.get("/rename3/c1/dummy");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Resource should not be accessible from the old path after renaming the parent.");

        Resource newR1 = registry.get("/rename4/c1/dummy");
        Assert.assertEquals(newR1.getProperty("test"), "rename", "Resource should contain a property with name test and value rename.");
    }

    @Test
    public void testGeneralCollectionRename() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "rename");
        r1.setContent("some text");
        registry.put("/c2/rename3/c1/dummy", r1);

        registry.rename("/c2/rename3", "rename4");

        boolean failed = false;
        
        try {
            registry.get("/c2/rename3/c1/dummy");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Resource should not be accessible from the old path after renaming the parent.");

        Resource newR1 = registry.get("/c2/rename4/c1/dummy");
        Assert.assertEquals(newR1.getProperty("test"), "rename", "Resource should contain a property with name test and value rename.");
    }
}
