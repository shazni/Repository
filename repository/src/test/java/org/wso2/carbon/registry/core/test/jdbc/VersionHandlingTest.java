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

public class VersionHandlingTest extends BaseTestCase {
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
    public void testCreateVersions() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("some content");
        registry.put("/version/r1", r1);
        registry.put("/version/r1", r1);

        String[] r1Versions = registry.getVersions("/version/r1");

        Assert.assertEquals(r1Versions.length, 1, "/version/r1 should have 1 version.");

        Resource r1v2 = registry.get("/version/r1");
        r1v2.setContent("another content");
        registry.put("/version/r1", r1v2);

        r1Versions = registry.getVersions("/version/r1");
        Assert.assertEquals(r1Versions.length, 2, "/version/r1 should have 2 version.");
    }

    @Test
    public void testResourceContentVersioning() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("content 1");
        registry.put("/v2/r1", r1);

        Resource r12 = registry.get("/v2/r1");
        r12.setContent("content 2");
        registry.put("/v2/r1", r12);
        registry.put("/v2/r1", r12);

        String[] r1Versions = registry.getVersions("/v2/r1");

        Resource r1vv1 = registry.get(r1Versions[1]);

        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[]) r1vv1.getContent()), "content 1", "r1's first version's content should be 'content 1'");

        Resource r1vv2 = registry.get(r1Versions[0]);

        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[]) r1vv2.getContent()), "content 2", "r1's second version's content should be 'content 2'");
    }

    @Test
    public void testResourcePropertyVersioning() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("content 1");
        r1.addProperty("p1", "v1");
        registry.put("/v4/r1", r1);

        Resource r1v2 = registry.get("/v4/r1");
        r1v2.addProperty("p2", "v2");
        registry.put("/v4/r1", r1v2);
        registry.put("/v4/r1", r1v2);

        String[] r1Versions = registry.getVersions("/v4/r1");

        Resource r1vv1 = registry.get(r1Versions[1]);

        Assert.assertEquals(r1vv1.getPropertyValue("p1"), "v1", "r1's first version should contain a property p1 with value v1");

        Resource r1vv2 = registry.get(r1Versions[0]);

        Assert.assertEquals(r1vv2.getPropertyValue("p1"), "v1", "r1's second version should contain a property p1 with value v1");

        Assert.assertEquals(r1vv2.getPropertyValue("p2"), "v2", "r1's second version should contain a property p2 with value v2");
    }

    @Test
    public void testSimpleCollectionVersioning() throws RepositoryException {
        Collection c1 = registry.newCollection();
        registry.put("/v3/c1", c1);

        registry.createVersion("/v3/c1");

        Collection c2 = registry.newCollection();
        registry.put("/v3/c1/c2", c2);

        registry.createVersion("/v3/c1");

        Collection c3 = registry.newCollection();
        registry.put("/v3/c1/c3", c3);

        registry.createVersion("/v3/c1");

        Collection c4 = registry.newCollection();
        registry.put("/v3/c1/c2/c4", c4);

        registry.createVersion("/v3/c1");

        Collection c5 = registry.newCollection();
        registry.put("/v3/c1/c2/c5", c5);

        registry.createVersion("/v3/c1");

        String[] c1Versions = registry.getVersions("/v3/c1");

        registry.get(c1Versions[0]);
        registry.get(c1Versions[1]);
        registry.get(c1Versions[2]);
    }

    @Test
    public void testResourceRestore() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("content 1");
        registry.put("/test/v10/r1", r1);

        Resource r1e1 = registry.get("/test/v10/r1");
        r1e1.setContent("content 2");
        registry.put("/test/v10/r1", r1e1);
        registry.put("/test/v10/r1", r1e1);

        String[] r1Versions = registry.getVersions("/test/v10/r1");
        registry.restoreVersion(r1Versions[1]);

        Resource r1r1 = registry.get("/test/v10/r1");

        Assert.assertEquals("content 1", RepositoryUtils.decodeBytes((byte[]) r1r1.getContent()), "Restored resource should have content 'content 1'");
    }

    @Test
    public void testSimpleCollectionRestore() throws RepositoryException {
        Collection c1 = registry.newCollection();
        registry.put("/test/v11/c1", c1);

        registry.createVersion("/test/v11/c1");

        Resource r1 = registry.newResource();
        r1.setContent("r1c1");
        registry.put("/test/v11/c1/r1", r1);

        registry.createVersion("/test/v11/c1");

        Resource r2 = registry.newResource();
        r2.setContent("r1c1");
        registry.put("/test/v11/c1/r2", r2);

        registry.createVersion("/test/v11/c1");

        String[] c1Versions = registry.getVersions("/test/v11/c1");
        Assert.assertEquals(c1Versions.length, 3, "/test/v11/c1 should have 3 versions.");
        
        Collection c1r0 = (Collection) registry.get("/test/v11/c1");
        Resource[] resources = c1r0.getChildren();
        
        Assert.assertEquals(resources.length, 2, "There needs to be 2 resources in Collection");

        registry.restoreVersion(c1Versions[2]);
        Collection c1r1 = (Collection) registry.get("/test/v11/c1");
        Assert.assertEquals(0, c1r1.getChildPaths().length, "version 1 of c1 should not have any children");

        try {
            registry.get("/test/v11/c1/r1");
            Assert.fail("Version 1 of c1 should not have child r1");
        } catch (RepositoryException e) {}
        
        try {
            registry.get("/test/v11/c1/r2");
            Assert.fail("Version 1 of c1 should not have child r2");
        } catch (RepositoryException e) {
        }

        registry.restoreVersion(c1Versions[1]);
        Collection c1r2 = (Collection) registry.get("/test/v11/c1");
        Assert.assertEquals(1, c1r2.getChildPaths().length, "version 2 of c1 should have 1 child");

        try {
            registry.get("/test/v11/c1/r1");
        } catch (RepositoryException e) {
            Assert.fail("Version 2 of c1 should have child r1");
        }

        try {
            registry.get("/test/v11/c1/r2");
            Assert.fail("Version 2 of c1 should not have child r2");
        } catch (RepositoryException e) {

        }

        registry.restoreVersion(c1Versions[0]);
        Collection c1r3 = (Collection) registry.get("/test/v11/c1");
        Assert.assertEquals(2, c1r3.getChildPaths().length, "version 3 of c1 should have 2 children");

        try {
            registry.get("/test/v11/c1/r1");
        } catch (RepositoryException e) {
            Assert.fail("Version 3 of c1 should have child r1");
        }

        try {
            registry.get("/test/v11/c1/r2");
        } catch (RepositoryException e) {
            Assert.fail("Version 3 of c1 should have child r2");
        }
    }

    @Test
    public void testAdvancedCollectionRestore() throws RepositoryException {
        Collection c1 = registry.newCollection();
        registry.put("/test/v12/c1", c1);

        registry.createVersion("/test/v12/c1");

        Resource r1 = registry.newResource();
        r1.setContent("r1c1");
        registry.put("/test/v12/c1/c11/r1", r1);

        registry.createVersion("/test/v12/c1");

        Collection c2 = registry.newCollection();
        registry.put("/test/v12/c1/c11/c2", c2);

        registry.createVersion("/test/v12/c1");

        Resource r1e1 = registry.get("/test/v12/c1/c11/r1");
        r1e1.setContent("r1c2");
        registry.put("/test/v12/c1/c11/r1", r1e1);

        registry.createVersion("/test/v12/c1");

        String[] c1Versions = registry.getVersions("/test/v12/c1");
        Assert.assertEquals(c1Versions.length, 4, "c1 should have 4 versions");

        registry.restoreVersion(c1Versions[3]);

        try {
            registry.get("/test/v12/c1/c11");
            Assert.fail("Version 1 of c1 should not have child c11");
        } catch (RepositoryException e) {
        }

        registry.restoreVersion(c1Versions[2]);

        try {
            registry.get("/test/v12/c1/c11");
        } catch (RepositoryException e) {
            Assert.fail("Version 2 of c1 should have child c11");
        }

        try {
            registry.get("/test/v12/c1/c11/r1");
        } catch (RepositoryException e) {
            Assert.fail("Version 2 of c1 should have child c11/r1");
        }

        registry.restoreVersion(c1Versions[1]);

        Resource r1e2 = null;
        
        try {
            r1e2 = registry.get("/test/v12/c1/c11/r1");
        } catch (RepositoryException e) {
            Assert.fail("Version 2 of c1 should have child c11/r1");
        }

        try {
            registry.get("/test/v12/c1/c11/c2");
        } catch (RepositoryException e) {
            Assert.fail("Version 2 of c1 should have child c11/c2");
        }

        String r1e2Content = RepositoryUtils.decodeBytes((byte[]) r1e2.getContent());
        Assert.assertEquals(r1e2Content, "r1c1", "c11/r1 content should be 'r1c1");

        registry.restoreVersion(c1Versions[0]);

        Resource r1e3 = registry.get("/test/v12/c1/c11/r1");
        String r1e3Content = RepositoryUtils.decodeBytes((byte[]) r1e3.getContent());
        Assert.assertEquals(r1e3Content, "r1c2", "c11/r1 content should be 'r1c2");
    }

    @Test
    public void testPermalinksForResources() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("r1c1");
        registry.put("/test/v13/r1", r1);
        registry.put("/test/v13/r1", r1);

        String[] r1Versions = registry.getVersions("/test/v13/r1");

        Resource r1e1 = registry.get(r1Versions[0]);
        Assert.assertEquals(r1e1.getPermanentPath(), r1Versions[0], "Permalink incorrect");

        r1e1.setContent("r1c2");
        registry.put("/test/v13/r1", r1e1);

        r1Versions = registry.getVersions("/test/v13/r1");

        Resource r1e2 = registry.get(r1Versions[0]);
        Assert.assertEquals(r1e2.getPermanentPath(), r1Versions[0], "Permalink incorrect");

        registry.restoreVersion(r1Versions[1]);

        Resource r1e3 = registry.get(r1Versions[1]);
        Assert.assertEquals(r1e3.getPermanentPath(), r1Versions[1], "Permalink incorrect");
    }

    @Test
    public void testPermalinksForCollections() throws RepositoryException {
        Collection c1 = registry.newCollection();
        registry.put("/test/v14/c1", c1);

        registry.createVersion("/test/v14/c1");

        String[] c1Versions = registry.getVersions("/test/v14/c1");
        Resource c1e1 = registry.get(c1Versions[0]);
        Assert.assertEquals(c1e1.getPermanentPath(), c1Versions[0], "Permalink incorrect");

        Resource r1 = registry.newResource();
        r1.setContent("r1c1");
        registry.put("/test/v14/c1/r1", r1);

        registry.createVersion("/test/v14/c1");
        
        c1Versions = registry.getVersions("/test/v14/c1");
        Resource c1e2 = registry.get(c1Versions[0]);
        Assert.assertEquals(c1e2.getPermanentPath(), c1Versions[0], "Permalink incorrect");

        registry.restoreVersion(c1Versions[1]);

        Resource c1e3 = registry.get(c1Versions[1]);
        Assert.assertEquals(c1e3.getPermanentPath(), c1Versions[1], "Permalink incorrect");
    }

    @Test
    public void testRootLevelVersioning() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("r1c1");
        registry.put("/vtr1", r1);

        registry.createVersion("/");

        Collection c2 = registry.newCollection();
        registry.put("/vtc2", c2);

        registry.createVersion("/");

        String[] rootVersions = registry.getVersions("/");

        Collection rootV0 = (Collection) registry.get(rootVersions[0]);
        String[] rootV0Children = (String[]) rootV0.getContent();
        Assert.assertTrue(RepositoryUtils.containsAsSubString("/vtr1", rootV0Children), "Root should have child vtr1");
        Assert.assertTrue(RepositoryUtils.containsAsSubString("/vtc2", rootV0Children), "Root should have child vtc2");

        Collection rootV1 = (Collection) registry.get(rootVersions[1]);
        String[] rootV1Children = (String[]) rootV1.getContent();
        Assert.assertTrue(RepositoryUtils.containsAsSubString("/vtr1", rootV1Children), "Root should have child vtr1");
        Assert.assertFalse(RepositoryUtils.containsAsSubString("/vtc2", rootV1Children), "Root should not have child vtc2");
    }
}
