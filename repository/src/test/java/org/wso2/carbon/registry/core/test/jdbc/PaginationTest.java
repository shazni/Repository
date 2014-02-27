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

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;

public class PaginationTest extends BaseTestCase {
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
    public void testCollectionPagination() throws RepositoryException {
        Collection c1 = registry.newCollection();
        registry.put("/test/c1", c1);
        
        for (int j = 0; j < 50; j++) {
            Collection ci = registry.newCollection();
            registry.put(String.format("/test/c1/c_%02d", j), ci);
        }
        
        Resource collection = registry.get("/test/c1");
        String childNodes[] = (String[])collection.getContent();
        Assert.assertEquals(50, childNodes.length);
        Collection coll = registry.get("/test/c1", 0, 20);
        Assert.assertEquals(20, coll.getChildCount());
        childNodes = coll.getChildPaths();
        Assert.assertEquals(20, childNodes.length);
        coll = (Collection)registry.get("/test/c1");
        Assert.assertEquals(50, coll.getChildCount());

        coll = registry.get("/test/c1", 20, 5);
        childNodes = coll.getChildPaths();
        Assert.assertEquals(5, childNodes.length);
        Assert.assertEquals(childNodes[0], "/test/c1/c_20");
        Assert.assertEquals(childNodes[4], "/test/c1/c_24");
    }
}
