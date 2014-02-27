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
import org.wso2.carbon.repository.core.caching.PathCache;

public class PathCacheTest extends BaseTestCase {
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
    public void testPathCache() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("some content");
        registry.put("/test/paths/r1", r1);

        double rate1 = PathCache.getPathCache().hitRate();
        registry.put("/test/paths/r2", r1);
        double rate2 = PathCache.getPathCache().hitRate();
        Assert.assertTrue(rate2 >= rate1, "Rate2 >= Rate1");

        registry.get("/test");
        double rate3 = PathCache.getPathCache().hitRate();
        Assert.assertTrue(rate3 >= rate2, "Rate3 >= Rate2");

        registry.get("/test/");
        double rate4 = PathCache.getPathCache().hitRate();
        Assert.assertTrue(rate4 >= rate3, "Rate4 >= Rate3");

        registry.get("/test");
        double rate5 = PathCache.getPathCache().hitRate();
        Assert.assertTrue(rate5 >= rate4, "Rate5 >= Rate4");

        registry.get("/test");
        double rate6 = PathCache.getPathCache().hitRate();
        Assert.assertTrue(rate6 >= rate5, "Rate6 >= Rate5");

        registry.get("/test");
        double rate7 = PathCache.getPathCache().hitRate();
        Assert.assertTrue(rate7 >= rate6, "Rate7 >= Rate6");
    }
}
