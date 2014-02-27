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

public class MoveTest extends BaseTestCase {
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
    public void testResourceMoveFromRoot() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "move");
        r1.setContent("c");
        registry.put("/move1", r1);

        Collection c1 = registry.newCollection();
        registry.put("/test/move", c1);

        registry.move("/move1", "/test/move/move1");

        Resource newR1 = registry.get("/test/move/move1");
        Assert.assertEquals(newR1.getProperty("test"), "move", "Moved resource should have a property named 'test' with value 'move'.");

        boolean failed = false;
        
        try {
            registry.get("/move1");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Moved resource should not be accessible from the old path.");
    }

    @Test
    public void testResourceMoveToRoot() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "move");
        r1.setContent("c");
        registry.put("/test/move/move2", r1);

        registry.move("/test/move/move2", "/move2");

        Resource newR1 = registry.get("/move2");
        Assert.assertEquals(newR1.getProperty("test"), "move", "Moved resource should have a property named 'test' with value 'move'.");

        boolean failed = false;
        
        try {
            registry.get("/test/move/move2");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Moved resource should not be accessible from the old path.");
    }

    @Test
    public void testGeneralResourceMove() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "move");
        r1.setContent("c");
        registry.put("/test/c1/move/move3", r1);

        Collection c2 = registry.newCollection();
        registry.put("/test/c2/move", c2);

        registry.move("/test/c1/move/move3", "/test/c2/move/move3");

        Resource newR1 = registry.get("/test/c2/move/move3");
        Assert.assertEquals(newR1.getProperty("test"), "move", "Moved resource should have a property named 'test' with value 'move'.");

        boolean failed = false;
        
        try {
            registry.get("/test/c1/move/move3");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Moved resource should not be accessible from the old path.");
    }

    @Test
    public void testGeneralCollectionMove() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "move");
        r1.setContent("c");
        registry.put("/test/c1/move5/move/dummy", r1);

        Collection c2 = registry.newCollection();
        registry.put("/test/c3", c2);

        registry.move("/test/c1/move5", "/test/c3/move5");

        Resource newR1 = registry.get("/test/c3/move5/move/dummy");
        Assert.assertEquals(newR1.getProperty("test"), "move", "Moved resource should have a property named 'test' with value 'move'.");

        boolean failed = false;
        
        try {
            registry.get("/test/c1/move5/move/dummy");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Moved resource should not be accessible from the old path.");
    }

    @Test
    public void testCollectionMoveWithChild() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("testX", "move");
        r1.setContent("c");
        
        registry.put("/testX/c1/move/move3", r1);
        registry.move("/testX/c1/", "/testX/c2/");

        Resource newR1 = registry.get("/testX/c2/move/move3");
        Assert.assertEquals(newR1.getProperty("testX"), "move", "Moved resource should have a property named 'testX' with value 'move'.");

        boolean failed = false;
        
        try {
            registry.get("/testX/c1/move/move3");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Moved resource should not be accessible from the old path.");
    }
    
    @Test
    public void testGeneralCacheCollectionMove() throws RepositoryException {
        Collection c1 = registry.newCollection();
        registry.put("/test/c1", c1);
        registry.move("/test/c1", "/test/c2");

        boolean failed = false;
        
        try {
            registry.get("/test/c1");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Moved collection should not be accessible from the old path.");
    }

    @Test
    public void testGeneralCacheResourceMove() throws RepositoryException {
        Resource r1 = registry.newResource();
        registry.put("/test/r1", r1);
        registry.move("/test/r1", "/test/r2");

        boolean failed = false;
        
        try {
            registry.get("/test/r1");
        } catch (RepositoryException e) {
            failed = true;
        }
        
        Assert.assertTrue(failed, "Moved resource should not be accessible from the old path.");
    }
}
