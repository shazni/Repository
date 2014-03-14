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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.utils.InternalUtils;
import org.wso2.carbon.repository.core.utils.MediaTypesUtils;

public class JDBCRegistryTest extends BaseTestCase {

    /**
     * Registry instance for use in tests. Note that there should be only one Registry instance in a
     * JVM.
     */
    protected static Repository registry = null;
    protected static Repository systemRegistry = null;

    @BeforeTest
    public void setUp() {
        super.setUp();
        
        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getRepository("admin");
            systemRegistry = embeddedRegistryService.getSystemRepository();
        } catch (RepositoryException e) {
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testIllegalCharacters() throws Exception {
        Resource r1 = registry.newResource();
        String str = "My Content";
        r1.setContentStream(new ByteArrayInputStream(str.getBytes()));

        String illegal = "~!@#%^*+={}|\\<>\"\',";
                
        char[] illegalChars = illegal.toCharArray();
        
        for (char character : illegalChars) {
            try {
                registry.put("/a" + character + "b", r1);
                Assert.fail("Should not be able to add resource with path containing '" + character + "'");
                break;
            } catch (RepositoryException e) {}
        }
    }

    @Test
    public void testMediaTypesInCaps() throws Exception {
        MediaTypesUtils.getResourceMediaTypeMappings(embeddedRegistryService.getConfigSystemRepository());
        Resource r1 = registry.newResource();
        String str = "My Content";
        r1.setContentStream(new ByteArrayInputStream(str.getBytes()));

        registry.put("/abc.JPG", r1);
        Assert.assertEquals("image/jpeg", registry.get("/abc.JPG").getMediaType());
    }

    @Test
    public void testCollectionDetails() throws Exception {
        Resource r1 = registry.newResource();
        String str = "My Content";
        r1.setContentStream(new ByteArrayInputStream(str.getBytes()));
        registry.put("/c1/c2/c3/c4/r1", r1);
        r1 = registry.newCollection();
        r1.setDescription("This is test description");
        r1.addProperty("p1", "value1");
        registry.put("/c1/c2/c3", r1);

        r1 = registry.get("/c1/c2/c3/c4/r1");
        InputStream inContent = r1.getContentStream();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        
        int c;
        
        while ((c = inContent.read()) != -1) {
            outStream.write(c);
        }
        
        inContent.close();
        Assert.assertEquals(str, RepositoryUtils.decodeBytes(outStream.toByteArray()));
    }

    @Test
    public void testFlatResourceHandling() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setDescription("This is a test resource used for registry testing.");
        String r1Content = "<c>This is r1 content</c>";
        r1.setContent(r1Content.getBytes());
        r1.addProperty("p1", "v1");
        registry.put("/r1", r1);

        Resource r1f = registry.get("/r1");

        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[])r1.getContent()),
                RepositoryUtils.decodeBytes((byte[])r1f.getContent()), "Content is not equal.");

        Assert.assertEquals(r1.getDescription(), r1f.getDescription(), "Description is not equal.");

        Assert.assertEquals(r1f.getPropertyValue("p1"), "v1", "Property p1 should contain the value v1");

        registry.delete("/r1");

        boolean failed = false;
        
        try {
            registry.get("/r1");
        } catch (RepositoryException e) {
            failed = true;
        }

        Assert.assertTrue(failed, "Deleted resource /r1 is returned on get.");

    }

    @Test
    public void testHierarchicalResourceHandling() throws Exception {
        Resource r1 = registry.newResource();
        String r1content = "R1 content";
        r1.setContent(r1content.getBytes());

        registry.put("/d1/r1", r1);

        Resource d1 = registry.get("/d1");

        Assert.assertTrue(d1 instanceof org.wso2.carbon.repository.api.Collection, "/d1 should be a collection.");
        Assert.assertTrue(d1.getContent() instanceof String[], "Content of /d1 should be a String[]");

        String[] children = (String[])d1.getContent();
        
        boolean found = false;
        
        for (String aChildren : children) {
            if (aChildren.startsWith("/d1/r1")) {
                found = true;
                break;
            }
        }
        
        Assert.assertTrue(found, "/d1/r1 should be a child of /d1");

        Resource r1f = registry.get("/d1/r1");

        Assert.assertEquals(r1content, RepositoryUtils.decodeBytes((byte[])r1f.getContent()), "Resource content is not stored correctly.");

        registry.delete("/d1");

        boolean f1 = false;
        
        try {
            registry.get("/d1");
        } catch (RepositoryException e) {
            f1 = true;
        }
        
        Assert.assertTrue(f1, "Deleted collection /d1 is not marked as deleted.");

        boolean f2 = false;
        
        try {
            registry.get("/d1/r1");
        } catch (RepositoryException e) {
            f2 = true;
        }
        
        Assert.assertTrue(f2, "Deleted collection /d1/r1 is not marked as deleted.");
    }

    @Test
    public void testResourceVersioning() throws Exception {
    	RepositoryContext registryContext = InternalUtils.getRepositoryContext(registry) ;
        boolean isVersionOnChange = registryContext.isVersionOnChange();
    	
        Resource r1 = registry.newResource();
        byte[] r1Content = "R1 content".getBytes();
        r1.setContent(r1Content);

        registry.put("/r5", r1);

        // first update
        Resource readIt1 = registry.get("/r5");
        byte[] newR1Content = "New content".getBytes();
        readIt1.setContent(newR1Content);

        if (!isVersionOnChange) {
            registry.createVersion("/r5");
        }
        
        registry.put("/r5", readIt1);

        // second update
        Resource readIt2 = registry.get("/r5");
        byte[] newR1Content2 = "New content2".getBytes();
        readIt2.setContent(newR1Content2);

        if (!isVersionOnChange) {
            registry.createVersion("/r5");
        }
        
        registry.put("/r5", readIt2);

        // after the redesigning of the database, we need to put another do
        // set the database
        if (!isVersionOnChange) {
            registry.createVersion("/r5");
        }
        
        registry.put("/r5", readIt2);

        String[] versionPaths = registry.getVersions("/r5");

        Resource v1 = registry.get(versionPaths[2]);
        Resource v2 = registry.get(versionPaths[1]);
        Resource v3 = registry.get(versionPaths[0]);

        String content1 = RepositoryUtils.decodeBytes((byte[])v1.getContent());
        String content2 = RepositoryUtils.decodeBytes((byte[])v2.getContent());
        String content3 = RepositoryUtils.decodeBytes((byte[])v3.getContent());

        Assert.assertEquals(content1, "R1 content", "Content is not versioned properly.");
        Assert.assertEquals(content2, "New content", "Content is not versioned properly.");
        Assert.assertEquals(content3, "New content2", "Content is not versioned properly.");

        try {
            registry.restoreVersion(versionPaths[2]);
        } catch (RepositoryException e) {
            Assert.fail("Valid restore version failed.");
        }

        Resource r5restored = registry.get("/r5");

        String restoredContent = RepositoryUtils.decodeBytes((byte[])r5restored.getContent());
        Assert.assertEquals("R1 content", restoredContent, "Content is not restored properly.");
    }

    @Test
    public void testPutOnSamePath() {
        try {
            Resource userProfile = registry.newResource();
            userProfile.setContent("test".getBytes());

            registry.put("/foo/bar", userProfile);

            Resource userProfile2 = registry.newResource();
            userProfile2.setContent("test".getBytes());
            registry.put("/foo/bar", userProfile2);

            Resource myUserProfile = registry.get("/foo/bar");
            myUserProfile.getContent();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCollectionVersioning() throws Exception {
        String r1Content = "r1 content1";
        Resource r1 = registry.newResource();
        r1.setContent(r1Content.getBytes());

        registry.put("/c10/r1", r1);

        registry.createVersion("/c10");

        String r2Content = "r2 content2";
        Resource r2 = registry.newResource();
        r2.setContent(r2Content.getBytes());

        registry.put("/c10/r2", r2);

        registry.createVersion("/c10");

        String[] versionPaths = registry.getVersions("/c10");

        Resource c10v1 = registry.get(versionPaths[1]);
        Resource c10v2 = registry.get(versionPaths[0]);

        String[] childrenOfv1 = (String[])c10v1.getContent();
        Assert.assertTrue(containsString(childrenOfv1, "/c10/r1"), "collection content is not versioned properly.");

        String[] childrenOfv2 = (String[])c10v2.getContent();
        Assert.assertTrue(containsString(childrenOfv2, "/c10/r1"), "collection content is not versioned properly.");
        Assert.assertTrue(containsString(childrenOfv2, "/c10/r2"), "collection content is not versioned properly.");

        registry.restoreVersion(versionPaths[1]);

        Resource restoredC10 = registry.get("/c10");

        String[] restoredC10Children = (String[])restoredC10.getContent();
        Assert.assertTrue(containsString(restoredC10Children, "/c10/r1"), "Collection children are not restored properly.");
        Assert.assertTrue(!containsString(restoredC10Children, "/c10/r2"), "Collection children are not restored properly.");
    }

    @Test
    public void testValueChange() throws Exception {
        Resource r1 = registry.newResource();
        String content1 = "Content1";
        r1.setContent(content1.getBytes());
        registry.put("/abc/foo", r1);
        String content2 = "Content2";
        r1.setContent(content2.getBytes());
        registry.put("/abc/foo", r1);
        r1 = registry.get("/abc/foo");
        Object resourceContent = r1.getContent();

        boolean value = Arrays.equals(content2.getBytes(), (byte[])resourceContent);
        Assert.assertTrue(value);
    }

    @Test
    public void testUserDefinedResourceQuery() throws Exception {
        Resource r1 = registry.newResource();
        String r1Content = "this is r1 content";
        r1.setContent(r1Content.getBytes());
        r1.setDescription("production ready.");
        String r1Path = "/c1/r1";
        registry.put(r1Path, r1);

        Resource r2 = registry.newResource();
        String r2Content = "content for r2 :)";
        r2.setContent(r2Content);
        r2.setDescription("ready for production use.");
        String r2Path = "/c2/r2";
        registry.put(r2Path, r2);

        Resource r3 = registry.newResource();
        String r3Content = "content for r3 :)";
        r3.setContent(r3Content);
        r3.setDescription("only for government use.");
        String r3Path = "/c2/r3";
        registry.put(r3Path, r3);

        String sql1 = "SELECT REG_PATH_ID, REG_NAME FROM REG_RESOURCE WHERE REG_DESCRIPTION LIKE ?";
        Resource q1 = systemRegistry.newResource();
        q1.setContent(sql1);
        q1.setMediaType(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);
        q1.addProperty(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME, RepositoryConstants.RESOURCES_RESULT_TYPE);
        
        systemRegistry.put("/qs/q1", q1);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("1", "%production%");
        Resource result = registry.executeQuery("/qs/q1", parameters);

        Assert.assertTrue(result instanceof org.wso2.carbon.repository.api.Collection, "Search with result type Resource should return a directory.");

        List<String> matchingPaths = new ArrayList<String>();
        String[] paths = (String[])result.getContent();
        matchingPaths.addAll(Arrays.asList(paths));

        Assert.assertTrue(matchingPaths.contains("/c1/r1"), "Path /c1/r1 should be in the results.");
        Assert.assertTrue(matchingPaths.contains("/c2/r2"), "Path /c2/r2 should be in the results.");
    }

    @Test
    public void testLogs() throws RepositoryException {
        String r1Content = "this is the r200 content.";
        Resource r1 = registry.newResource();
        r1.setContent(r1Content.getBytes());

        try {
            registry.put("/r200", r1);
        } catch (RepositoryException e) {
            Assert.fail("Couldn't put a content resource in to path /r200");
        }
    }

    @Test
    public void testResourceDelete() throws RepositoryException {
        String content1 = "Content1";
        Resource r1 = registry.newResource();
        r1.setContent(content1);
        
        try {
            registry.put("/wso2/wsas/v1/r1", r1);
        } catch (RepositoryException e) {
        	Assert.fail("Couldn't put a content resource in to path /wso2/wsas/v1/r1");
        }

        // Adding a dummy resource
        Resource v2 = registry.newResource();
        registry.put("/wso2/wsas/v2", v2);

        //getting the resource
        Resource r2 = registry.get("/wso2/wsas/v1");

        //check whether the content is correct
        Assert.assertEquals("/wso2/wsas/v1/r1", ((String[])r2.getContent())[0]);
        Resource wsas = registry.get("/wso2/wsas");
        String[] wsasContent = (String[])wsas.getContent();
        Assert.assertNotNull(wsasContent);
        Assert.assertEquals(2, wsasContent.length);

        registry.delete("/wso2/wsas/v1");

        String content2 = "Content2";
        Resource resourceContent2 = registry.newResource();
        resourceContent2.setContent(content2);
        registry.put("/wso2/wsas/v1/r2", resourceContent2);

        wsas = registry.get("/wso2/wsas");
        wsasContent = (String[])wsas.getContent();
        Assert.assertNotNull(wsasContent);
        Assert.assertEquals(2, wsasContent.length);

        r2 = registry.get("/wso2/wsas/v1");
        //check whether the content is correct
        Assert.assertEquals("/wso2/wsas/v1/r2", ((String[])r2.getContent())[0]);
    }

    @Test
    public void testCombinedScenario() throws RepositoryException {
        // put a content resource in to the root
        String r1Content = "this is the r1 content.";
        Resource r1 = registry.newResource();
        r1.setContent(r1Content.getBytes());
        registry.put("/r11", r1);

        Resource r1New = registry.newResource();
        r1New.setContent("New r1");
        registry.put("/r1", r1New);

        // put a collection in to the root
        Collection c1 = registry.newCollection();
        registry.put("/c1", c1);

        // put a content artifact in to /c1/r2
        String r2Content = "this is r2 content";
        Resource r2 = registry.newResource();
        r2.setContent(r2Content.getBytes());

        registry.put("/c1/r2", r2);
        // put a content artifact in to non-existing collection

        String r3Content = "this is r3 content";
        Resource r3 = registry.newResource();

        r3.addProperty("Reviewer", "Foo");
        r3.addProperty("TestDone", "Axis2");
        r3.setContent(r3Content.getBytes());

        registry.put("/c2/r3", r3);

        // put c2/r4
        String r4Content = "this is r4 content";
        Resource r4 = registry.newResource();
        r4.setContent(r4Content.getBytes());

        registry.put("/c2/r4", r4);

        registry.delete("/r11");
        registry.delete("/c1");
        registry.delete("/c2");
    }

    @Test
    public void testGetMetaData() throws RepositoryException {
        String r1Content = "this is the rgm content.";
        Resource r = registry.newResource();
        r.setContent(r1Content.getBytes());
        registry.put("/rgm", r);

        Resource rm = registry.getMetaData("/rgm");
        Assert.assertNull(rm.getContent());
        
        Resource rr = registry.get("/rgm");
        Assert.assertNotNull(rr.getContent());      
    }

    @Test
    public void testResourceCollectionMix() throws RepositoryException {
        Resource defaultGadgetCollection = registry.newResource();
        registry.put("/system/gadgets", defaultGadgetCollection);

        defaultGadgetCollection = registry.newCollection();
        registry.put("/system/gadgets", defaultGadgetCollection);

        Resource r = registry.get("/system/gadgets");

        Assert.assertTrue(r instanceof Collection, "R should be a collection");
        Assert.assertTrue(registry.resourceExists("/system/gadgets"), "R should exist");

        registry.delete("/system/gadgets");

        defaultGadgetCollection = registry.newResource();
        registry.put("/system/gadgets", defaultGadgetCollection);

        r = registry.get("/system/gadgets");

        Assert.assertFalse(r instanceof Collection, "R should not be a collection");
        Assert.assertTrue(registry.resourceExists("/system/gadgets"), "R should exist");
        defaultGadgetCollection = registry.newCollection();
        registry.put("/system/gadgets", defaultGadgetCollection);

        r = registry.get("/system/gadgets");

        Assert.assertTrue(r instanceof Collection, "R should be a collection");
        Assert.assertTrue(registry.resourceExists("/system/gadgets"), "R should exist");
    }

    @Test
    public void testLastUpdateWithGet() throws RepositoryException {
        Resource r1 = registry.newResource();
        r1.setContent("test");
        registry.put("/pqr/xyz", r1);

        Resource r2 = registry.get("/pqr/xyz");
        Date date2 = r2.getLastModified();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            
        }

        Resource r3 = registry.get("/pqr/xyz");
        Date date3 = r3.getLastModified();

        Assert.assertEquals(date2, date3, "update time should be equal");
    }

    @Test
    public void testLastUpdateWithPut() throws RepositoryException {
        Resource r1 = registry.newResource();
        registry.put("/pqr/xyz", r1);

        Resource r2 = registry.get("/pqr/xyz");
        Date date2 = r2.getLastModified();

        registry.put("/pqr/xyz", r1);

        Resource r3 = registry.get("/pqr/xyz");
        Date date3 = r3.getLastModified();

        Assert.assertNotSame(date2, date3, "update time should be different");
    }

    private boolean containsString(String[] array, String value) {
        boolean found = false;
        
        for (String anArray : array) {
            if (anArray.startsWith(value)) {
                found = true;
                break;
            }
        }

        return found;
    }
}
