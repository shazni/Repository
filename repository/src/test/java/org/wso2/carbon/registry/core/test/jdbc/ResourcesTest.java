/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.carbon.repository.api.utils.RepositoryUtils;

public class ResourcesTest extends BaseTestCase {
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
    public void testResourceUpdate() throws RepositoryException {
        String r1Path = "/rTest/r1";
        Resource r1 = registry.newResource();
        r1.setContent("c1");
        r1.setProperty("p1", "v1");
        registry.put(r1Path, r1);

        Resource r1e1 = registry.get(r1Path);
        r1e1.setProperty("p1", "v2");
        registry.put(r1Path, r1e1);

        Resource r1e2 = registry.get(r1Path);
        Assert.assertNotNull(r1e2.getContent(), "r1 content should not be null");

        String r1e2Content = RepositoryUtils.decodeBytes((byte[]) r1e2.getContent());
        Assert.assertEquals(r1e2Content, "c1", "r1 content should be c1");
    }
}
