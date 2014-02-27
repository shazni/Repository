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
import org.wso2.carbon.user.api.RealmConfiguration;

public class StaticConfigurationFalseTest extends BaseTestCase {
    protected static Repository registry = null;
    
    @BeforeTest
    public void setUp() {
        setupCarbonHome();

        StaticConfiguration.setVersioningProperties(false);

        setupContext();
        
        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getRepository("admin");
        } catch (RepositoryException e) {
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testVersioningProperties() throws RepositoryException {
        Assert.assertEquals(StaticConfiguration.isVersioningProperties(), false);
        Resource r = registry.newResource();
        r.setProperty("key1", "value1");
        registry.put("/testProperties", r);

        r = registry.get("/testProperties");

        r.setProperty("key2", "value2");
        registry.put("/testProperties", r);
        // to create the version
        registry.put("/testProperties", r);

        r = registry.get("/testProperties");
        Assert.assertEquals(r.getProperties().size(), 2);
        // retrieve versions
        String []versionPaths = registry.getVersions("/testProperties");

        Assert.assertEquals(versionPaths.length, 2);
        r = registry.get(versionPaths[1]);
        // still there should be a resource
        Assert.assertEquals(r.getProperties().size(), 2);
        Assert.assertEquals(r.getProperty("key2"), "value2");

        // same should be done to collections as well
        Resource c = registry.newCollection();
        c.setProperty("key1", "value1");
        registry.put("/testPropertiesC", c);
        registry.createVersion("/testPropertiesC");

        c.setProperty("key2", "value2");
        registry.put("/testPropertiesC", c);
        // to create the version
        registry.createVersion("/testPropertiesC");

        // retrieve versions
        versionPaths = registry.getVersions("/testPropertiesC");

        Assert.assertEquals(versionPaths.length, 2);
        c = registry.get(versionPaths[1]);
        // still there should be a resource
        Assert.assertEquals(c.getProperties().size(), 2);
        Assert.assertEquals(c.getProperty("key2"), "value2");
    }
}
