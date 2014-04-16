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

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;

public class PropertiesTest extends BaseTestCase {

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
    public void testRootLevelProperties() throws RepositoryException {
        Resource root = registry.get("/");
        root.addProperty("p1", "v1");
        registry.put("/", root);

        Resource rootB = registry.get("/");
        Assert.assertEquals(rootB.getPropertyValue("p1"), "v1", "Root should have a property named p1 with value v1");
    }

    @Test
    public void testSingleValuedProperties() throws RepositoryException {
        Resource r2 = registry.newResource();
        r2.setContent("Some content for r2");
        r2.addProperty("p1", "p1v1");
        registry.put("/propTest/r2", r2);

        Resource r2b = registry.get("/propTest/r2");
        String p1Value = r2b.getPropertyValue("p1");

        Assert.assertEquals(p1Value, "p1v1", "Property p1 of /propTest/r2 should contain the value p1v1");
    }

    @Test
    public void testMultiValuedProperties() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("Some content for r1");
        r1.addProperty("p1", "p1v1");
        r1.addProperty("p1", "p1v2");
        registry.put("/propTest/r1", r1);

        Resource r1b = registry.get("/propTest/r1");
        List<String> propValues = r1b.getPropertyValues("p1");

        Assert.assertTrue(propValues.contains("p1v1"), "Property p1 of /propTest/r1 should contain the value p1v1");

        Assert.assertTrue(propValues.contains("p1v2"), "Property p1 of /propTest/r1 should contain the value p1v2");
    }

    @Test
    public void testNullValuedProperties() throws RepositoryException {
        Resource r2 = registry.newResource();
        r2.setContent("Some content for r2");
        r2.addProperty("p1", null);
        registry.put("/propTest3/r2", r2);

        Resource r2b = registry.get("/propTest3/r2");
        String p1Value = r2b.getPropertyValue("p1");

        Assert.assertEquals(p1Value, null, "Property p1 of /propTest3/r2 should contain the value null");
    }

    @Test
    public void testNullMultiValuedProperties() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("Some content for r1");
        r1.addProperty("p1", null);
        r1.addProperty("p1", null);
        registry.put("/propTest4/r1", r1);

        Resource r1b = registry.get("/propTest4/r1");
        List<String> propValues = r1b.getPropertyValues("p1");

        Assert.assertEquals(propValues.get(0), null, "Property p1 of /propTest4/r1 should contain the value null");

        Assert.assertEquals(propValues.get(1), null, "Property p1 of /propTest4/r1 should contain the value null");
    }

    @Test
    public void testRemovingProperties() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("r1 content");
        r1.setProperty("p1", "v1");
        r1.setProperty("p2", "v2");
        registry.put("/props/t1/r1", r1);

        Resource r1e1 = registry.get("/props/t1/r1");
        r1e1.removeProperty("p1");
        registry.put("/props/t1/r1", r1e1);

        Resource r1e2 = registry.get("/props/t1/r1");

        Assert.assertEquals(r1e2.getPropertyValue("p1"), null, "Property is not removed.");
        Assert.assertNotNull(r1e2.getPropertyValue("p2"), "Wrong property is removed.");
    }

    @Test
    public void testRemovingMultivaluedProperties() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("r1 content");
        r1.addProperty("p1", "v1");
        r1.addProperty("p1", "v2");
        registry.put("/props/t2/r1", r1);

        Resource r1e1 = registry.get("/props/t2/r1");
        List<String> propertyValues = r1e1.getPropertyValues("p1");
        propertyValues.remove("v1");
        r1e1.setProperty("p1", propertyValues);
        registry.put("/props/t2/r1", r1e1);

        Resource r1e2 = registry.get("/props/t2/r1");

        Assert.assertFalse(r1e2.getPropertyValues("p1").contains("v1"), "Property is not removed.");
        Assert.assertTrue(r1e2.getPropertyValues("p1").contains("v2"), "Wrong property is removed.");
    }

    @Test
    public void testEditingMultivaluedProperties() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("r1 content");
        r1.addProperty("p1", "v1");
        r1.addProperty("p1", "v2");
        registry.put("/props/t3/r1", r1);

        Resource r1e1 = registry.get("/props/t3/r1");
        List<String> propertyValues = r1e1.getPropertyValues("p1");
        propertyValues.remove("v1");
        propertyValues.add("v3");
        r1e1.setProperty("p1", propertyValues);
        registry.put("/props/t3/r1", r1e1);

        Resource r1e2 = registry.get("/props/t3/r1");

        Assert.assertFalse(r1e2.getPropertyValues("p1").contains("v1"), "Property is not edited.");
        Assert.assertTrue(r1e2.getPropertyValues("p1").contains("v3"), "Property is not edited.");
        Assert.assertTrue(r1e2.getPropertyValues("p1").contains("v2"), "Wrong property is removed.");
    }
}
