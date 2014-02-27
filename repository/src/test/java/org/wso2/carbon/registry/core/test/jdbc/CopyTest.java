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
import org.wso2.carbon.repository.api.utils.RepositoryUtils;

public class CopyTest extends BaseTestCase {

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
    public void testResourceCopy() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "copy");
        r1.setContent("c");
        registry.put("/test/copy/c1/copy1", r1);

        Collection c1 = registry.newCollection();
        registry.put("/test/move", c1);

        registry.copy("/test/copy/c1/copy1", "/test/copy/c2/copy1");

        Resource newR1 = registry.get("/test/copy/c2/copy1");
        Assert.assertEquals(newR1.getProperty("test"), "copy", "Copied resource should have a property named 'test' with value 'copy'.");

        Resource oldR1 = registry.get("/test/copy/c1/copy1");
        Assert.assertEquals(oldR1.getProperty("test"), "copy", "Original resource should have a property named 'test' with value 'copy'.");

        String newContent = RepositoryUtils.decodeBytes((byte[]) newR1.getContent());
        String oldContent = RepositoryUtils.decodeBytes((byte[]) oldR1.getContent());
        Assert.assertEquals(newContent, oldContent, "Contents are not equal in copied resources");
    }

    @Test
    public void testCollectionCopy() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setProperty("test", "copy");
        r1.setContent("c");
        registry.put("/test/copy/copy3/c3/resource1", r1);

        Collection c1 = registry.newCollection();
        registry.put("/test/move", c1);

        registry.copy("/test/copy/copy3", "/test/newCol/copy3");

        Resource newR1 = registry.get("/test/newCol/copy3/c3/resource1");
        Assert.assertEquals(newR1.getProperty("test"), "copy", "Copied resource should have a property named 'test' with value 'copy'.");

        Resource oldR1 = registry.get("/test/copy/copy3/c3/resource1");
        Assert.assertEquals(oldR1.getProperty("test"), "copy", "Original resource should have a property named 'test' with value 'copy'.");
    }
}
