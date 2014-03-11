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
import org.wso2.carbon.repository.core.config.StaticConfiguration;

public class StaticConfigurationTrueTest extends BaseTestCase {
    protected static Repository registry = null;

    @BeforeTest
    public void setUp() {
        setupCarbonHome();

        StaticConfiguration.setVersioningProperties(true);

        setupContext();

        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getRepository("admin");
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testVersioningProperties() throws RepositoryException {
        Assert.assertEquals(StaticConfiguration.isVersioningProperties(), true);
        Resource r = registry.newResource();
        r.setProperty("key1", "value1");
        r.setProperty("key2", "value2");
        registry.put("/testProperties", r);

        r = registry.get("/testProperties");

        r.setProperty("key3", "value3");
        r.setProperty("key1", "value1dup");
        registry.put("/testProperties", r);
        // to create the version
        registry.put("/testProperties", r);

        r = registry.get("/testProperties");
        Assert.assertEquals(r.getPropertyKeys().size(), 3);

        // retrieve versions
        String []versionPaths = registry.getVersions("/testProperties");

        Assert.assertEquals(versionPaths.length, 2);

        registry.restoreVersion(versionPaths[1]);

        r = registry.get("/testProperties");
        // still there should be a resource
        Assert.assertEquals(r.getPropertyKeys().size(), 2);
        Assert.assertEquals(r.getPropertyValue("key1"), "value1");

        // again getting the latest version
        r = registry.get(versionPaths[0]);
        // still there should be a resource
        Assert.assertEquals(r.getPropertyKeys().size(), 3);
        Assert.assertEquals(r.getPropertyValue("key1"), "value1dup");

        // same should be done to collections as well
        Resource c = registry.newCollection();
        c.setProperty("key1", "value1");
        c.setProperty("key2", "value2");
        registry.put("/testPropertiesC", c);
        registry.createVersion("/testPropertiesC");

        c = registry.get("/testPropertiesC");

        c.setProperty("key3", "value3");
        c.setProperty("key1", "value1dup");
        registry.put("/testPropertiesC", c);

        // to create the version
        registry.createVersion("/testPropertiesC");
        c = registry.get("/testPropertiesC");
        Assert.assertEquals(c.getPropertyKeys().size(), 3);

        // retrieve versions
        versionPaths = registry.getVersions("/testPropertiesC");

        Assert.assertEquals(versionPaths.length, 2);

        registry.restoreVersion(versionPaths[1]);

        c = registry.get("/testPropertiesC");
        // still there should be a resource
        Assert.assertEquals(c.getPropertyKeys().size(), 2);
        Assert.assertEquals(c.getPropertyValue("key1"), "value1");

        c = registry.get(versionPaths[0]);
        // still there should be a resource
        Assert.assertEquals(c.getPropertyKeys().size(), 3);
        Assert.assertEquals(c.getPropertyValue("key1"), "value1dup");
    }
}
