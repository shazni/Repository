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

public class TransactionsTest extends BaseTestCase {
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
    public void testSuccessfulSimpleTransaction() throws RepositoryException {
        registry.beginTransaction();

        Resource r1 = registry.newResource();
        r1.setProperty("test", "t1");
        r1.setContent("some content");
        registry.put("/t1/r1", r1);

        registry.commitTransaction();

        Resource r1b = registry.get("/t1/r1");
        Assert.assertEquals(r1b.getPropertyValue("test"), "t1", "Completed transaction resource should have a property named 'test' with value 't1'");
    }

    @Test
    public void testFailedSimpleTransaction() throws RepositoryException {
        registry.beginTransaction();

        Resource r2 = registry.newResource();
        r2.setProperty("test", "t2");
        r2.setContent("some content");
        registry.put("/t1/r2", r2);

        registry.rollbackTransaction();

        try {
            registry.get("/t1/r2");
            Assert.fail("Resource added by the incomplete transaction should be deleted.");
        } catch (RepositoryException e) {}
    }

    @Test
    public void testSuccessfulMultiOperationTransaction() throws RepositoryException {
        registry.beginTransaction();

        Resource r1 = registry.newResource();
        r1.setProperty("test", "t2");
        r1.setContent("some content");
        registry.put("/t2/r1", r1);

        registry.commitTransaction();

        Resource r1b = registry.get("/t2/r1");
        Assert.assertEquals(r1b.getPropertyValue("test"), "t2", "Completed transaction resource should have a property named 'test' with value 't2'");
    }

    @Test
    public void testFailedMultiOperationTransaction() throws RepositoryException {
        registry.beginTransaction();

        Resource r1 = registry.newResource();
        r1.setProperty("test", "t2");
        r1.setContent("some content");
        registry.put("/t3/r1", r1);

        registry.rollbackTransaction();

        try {
            registry.get("/t3/r1");
            Assert.fail("Resource /t3/r1 should be deleted after transaction is rolled back.");
        } catch (RepositoryException e) {
        }
    }

    @Test
    public void testNestedSuccessfulMultiOperationTransaction() throws RepositoryException {
        registry.beginTransaction();

        Resource r1 = registry.newResource();
        r1.setProperty("test", "t2");
        r1.setContent("some content");
        registry.put("t2/r1", r1);

        registry.beginTransaction();

        registry.commitTransaction();

        registry.commitTransaction();

        Resource r1b = registry.get("/t2/r1");
        Assert.assertEquals(r1b.getPropertyValue("test"), "t2", "Completed transaction resource should have a property named 'test' with value 't2'");
    }

    @Test
    public void testNestedFailedMultiOperationTransaction() throws RepositoryException {
        registry.beginTransaction();

        Resource r1 = registry.newResource();
        r1.setProperty("test", "t2");
        r1.setContent("some content");
        registry.put("/t32/r1", r1);
        registry.beginTransaction();

        registry.commitTransaction();

        registry.rollbackTransaction();

        try {
            registry.get("/t32/r1");
            Assert.fail("Resource /t32/r1 should be deleted after transaction is rolled back.");
        } catch (RepositoryException e) {
        }
    }
}
