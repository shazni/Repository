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

public class SymlinkTest extends BaseTestCase {
    protected static Repository registry = null;

    @BeforeTest
    public void setUp() {
        super.setUp();

        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getSystemRepository();
        } catch (RepositoryException e) {
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testSymbolicLinks() throws RepositoryException {
        Collection testCollection = registry.newCollection();
        Resource testResource = registry.newResource();
        testCollection.setProperty("name", "valueC");
        registry.put("/testCollection", testCollection);
        testResource.setProperty("name", "value1");
        registry.put("/testCollection/testResource", testResource);
        
        registry.createLink("/mountCollection", "/testCollection");
        Resource mountCollection = registry.get("/mountCollection");
        Assert.assertEquals(mountCollection.getPropertyValue("name"), "valueC");
        String [] children = (String []) mountCollection.getContent();
        Assert.assertEquals(children[0], "/mountCollection/testResource");
        Resource mountedResource = registry.get("/mountCollection/testResource");
        Assert.assertEquals(mountedResource.getPropertyValue("name"), "value1");

        registry.createLink("/testCollection", "/");
        boolean exceptionOccurred = false;
        
        try {
            registry.createLink("/testCollection", "/testCollection");
        } catch (Exception e) {
            exceptionOccurred = true;
        }
        
        Assert.assertTrue(exceptionOccurred, "Symlink link to itself is not valid");
    }

    @Test
    public void testSymbolicLinksRoots() throws RepositoryException {
        Collection testCollection = registry.newCollection();
        Resource testResource = registry.newResource();
        testCollection.setProperty("name", "valueC");
        registry.put("/", testCollection);
        testResource.setProperty("name", "value1");
        registry.put("/testCollection2", testResource);

        registry.createLink("/mountCollection2", "/");
        Resource mountCollection = registry.get("/mountCollection2");
        Assert.assertEquals(mountCollection.getPropertyValue("name"), "valueC");
        String [] children = (String []) mountCollection.getContent();
        Assert.assertEquals(children[0], "/mountCollection2/testCollection2");
        Resource mountedResource = registry.get("/mountCollection2/testCollection2");
        Assert.assertEquals(mountedResource.getPropertyValue("name"), "value1");
    }    

    @Test
    public void testCopySourceSymLinkRoot() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a/b/c", testResource);
        registry.createLink("/p", "/a/b"); // now p is a link to b

        registry.copy("/p", "/q");

        Assert.assertTrue(registry.resourceExists("/q/c"), "q should have resource c");
    }

    @Test
    public void testCopySourceFromSymlink() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a1/b1/c1/d1", testResource);
        registry.createLink("/p1", "/a1/b1"); // now p is a link to b

        registry.copy("/p1/c1", "/q1");

        Assert.assertTrue(registry.resourceExists("/q1/d1"), "q1 should have resource d1");  
    }

    @Test
    public void testCopyTargetToSymLink() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a2/b2/", testResource);
        registry.put("/c2/d2/", testResource);
        registry.createLink("/p2", "/a2/b2"); // now p is a link to b

        registry.copy("/c2", "/p2/c2");

        Assert.assertTrue(registry.resourceExists("/p2/c2/d2"), "q2 should have resource c2");
        Assert.assertTrue(registry.resourceExists("/a2/b2/c2/d2"), "q2 should have resource c2");
    }

    @Test
    public void testCopyBothSymLink() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a3/b3/", testResource);
        registry.put("/c3/d3/e3", testResource);
        registry.createLink("/p3", "/a3/b3");   
        registry.createLink("/q3", "/c3/d3");

        registry.copy("/q3/e3", "/p3/e3");

        Assert.assertTrue(registry.resourceExists("/p3/e3"), "p3 should have resource e3");
        Assert.assertTrue(registry.resourceExists("/a3/b3/e3"), "a3/b3/ should have resource e3");
    }

    @Test
    public void testMoveSourceSymLinkRoot() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a4/b4/c4", testResource);
        registry.createLink("/p4", "/a4/b4"); // now p is a link to b

        registry.move("/p4", "/q4");

        Assert.assertTrue(registry.resourceExists("/q4/c4"), "q4 should have resource c4");
    }

    @Test
    public void testMoveSourceFromSymlink() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a11/b11/c11/d11", testResource);
        registry.createLink("/p11", "/a11/b11"); // now p is a link to b

        registry.move("/p11/c11", "/q11");

        Assert.assertTrue(registry.resourceExists("/q11/d11"), "q11 should have resource d11");
    }

    @Test
    public void testMoveTargetToSymLink() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a21/b21/", testResource);
        registry.put("/c21/d21/", testResource);
        registry.createLink("/p21", "/a21/b21"); // now p is a link to b

        registry.move("/c21", "/p21/c21");

        Assert.assertTrue(registry.resourceExists("/p21/c21/d21"), "q21 should have resource c21");
        Assert.assertTrue(registry.resourceExists("/a21/b21/c21/d21"), "q21 should have resource c21");
    }

    @Test
    public void testMoveBothSymLink() throws RepositoryException {
        Collection testResource = registry.newCollection();
        registry.put("/a31/b31/", testResource);
        registry.put("/c31/d31/e31", testResource);
        registry.createLink("/p31", "/a31/b31");
        registry.createLink("/q31", "/c31/d31");

        registry.move("/q31/e31", "/p31/e31");

        Assert.assertTrue(registry.resourceExists("/p31/e31"), "p31 should have resource e31");
        Assert.assertTrue(registry.resourceExists("/a31/b31/e31"), "a31/b31/ should have resource e31");
    }

    @Test
    public void testMoveSourceSymLinkRootCollection() throws RepositoryException {
        Resource testResource = registry.newResource();
        registry.put("/a6/b6/c6", testResource);
        registry.createLink("/p6", "/a6/b6"); // now p is a link to b
        registry.createLink("/x6", "/a6/b6/c6");

        registry.move("/p6", "/q6");
        registry.move("/x6", "/y6");

        Assert.assertTrue(registry.get("/q6") instanceof Collection, "q6 should be a collection");
        Assert.assertTrue(!(registry.get("/y6") instanceof Collection), "y6 should be a non-collection");
    }

    @Test
    public void testCopyParentWithSymlink() throws RepositoryException {
        // adding basic resources.
        Resource testR = registry.newResource();
        testR.setContent("test R content");
        registry.put("/target/originalR", testR);
        Resource testR2 = registry.newResource();
        testR2.setContent("test R2 content");
        registry.put("/target/originalC/r2", testR2);

        Collection col = registry.newCollection();
        registry.put("/source", col);

        // creating the sym link
        registry.createLink("/source/symR", "/target/originalR");
        registry.createLink("/source/symC", "/target/originalC");

        // now just check the link is created
        Resource testRSym1 = registry.get("/source/symR");
        byte[] testRSym1Bytes = (byte[])testRSym1.getContent();
        Assert.assertEquals("test R content", RepositoryUtils.decodeBytes(testRSym1Bytes));
        
        Resource testR2Sym1 = registry.get("/source/symC/r2");
        byte[] testR2Sym1Bytes = (byte[])testR2Sym1.getContent();
        Assert.assertEquals("test R2 content", RepositoryUtils.decodeBytes(testR2Sym1Bytes));

        // now copy the source
        registry.copy("/source", "/source-copy");

        // now check the copied symbolic links
        Resource testRSym2 = registry.get("/source-copy/symR");
        byte[] testRSym2Bytes = (byte[])testRSym2.getContent();
        Assert.assertEquals("test R content", RepositoryUtils.decodeBytes(testRSym2Bytes));

        Resource testR2Sym2 = registry.get("/source-copy/symC/r2");
        byte[] testR2Sym2Bytes = (byte[])testR2Sym2.getContent();
        Assert.assertEquals("test R2 content", RepositoryUtils.decodeBytes(testR2Sym2Bytes));

        // change the symbolic links and whether the original is updated
        testRSym2.setContent("test R updated content");
        registry.put("/source-copy/symR", testRSym2);
        testR2Sym2.setContent("test R2 updated content");
        registry.put("/source-copy/symC/r2", testR2Sym2);

        // and check whether the original is updated.
        testR = registry.get("/target/originalR");
        byte[] testRBytes = (byte[])testR.getContent();
        Assert.assertEquals("test R updated content", RepositoryUtils.decodeBytes(testRBytes));

        testR2 = registry.get("/target/originalC/r2");
        byte[] testR2Bytes = (byte[])testR2.getContent();
        Assert.assertEquals("test R2 updated content", RepositoryUtils.decodeBytes(testR2Bytes));

        // cleaning up for the next test case
        registry.delete("/source");
        registry.delete("/source-copy");
        registry.delete("/target");
    }

    @Test
    public void testMoveParentWithSymlink() throws RepositoryException {
        // adding basic resources.
        Resource testR = registry.newResource();
        testR.setContent("test R content");
        registry.put("/target/originalR", testR);
        Resource testR2 = registry.newResource();
        testR2.setContent("test R2 content");
        registry.put("/target/originalC/r2", testR2);

        Collection col = registry.newCollection();
        registry.put("/source", col);

        // creating the sym link
        registry.createLink("/source/symR", "/target/originalR");
        registry.createLink("/source/symC", "/target/originalC");

        // now just check the link is created
        Resource testRSym1 = registry.get("/source/symR");
        byte[] testRSym1Bytes = (byte[])testRSym1.getContent();
        Assert.assertEquals("test R content", RepositoryUtils.decodeBytes(testRSym1Bytes));

        Resource testR2Sym1 = registry.get("/source/symC/r2");
        byte[] testR2Sym1Bytes = (byte[])testR2Sym1.getContent();
        Assert.assertEquals("test R2 content", RepositoryUtils.decodeBytes(testR2Sym1Bytes));

        // now copy the source
        registry.move("/source", "/source-copy");

        // now check the copied symbolic links
        Resource testRSym2 = registry.get("/source-copy/symR");
        byte[] testRSym2Bytes = (byte[])testRSym2.getContent();
        Assert.assertEquals("test R content", RepositoryUtils.decodeBytes(testRSym2Bytes));

        Resource testR2Sym2 = registry.get("/source-copy/symC/r2");
        byte[] testR2Sym2Bytes = (byte[])testR2Sym2.getContent();
        Assert.assertEquals("test R2 content", RepositoryUtils.decodeBytes(testR2Sym2Bytes));

        // change the symbolic links and whether the original is updated
        testRSym2.setContent("test R updated content");
        registry.put("/source-copy/symR", testRSym2);
        testR2Sym2.setContent("test R2 updated content");
        registry.put("/source-copy/symC/r2", testR2Sym2);

        // and check whether the original is updated.
        testR = registry.get("/target/originalR");
        byte[] testRBytes = (byte[])testR.getContent();
        Assert.assertEquals("test R updated content", RepositoryUtils.decodeBytes(testRBytes));

        testR2 = registry.get("/target/originalC/r2");
        byte[] testR2Bytes = (byte[])testR2.getContent();
        Assert.assertEquals("test R2 updated content", RepositoryUtils.decodeBytes(testR2Bytes));

        // cleaning up for the next test case
        registry.delete("/source-copy");
        registry.delete("/target");
    }

    @Test
    public void testRenameParentWithSymlink() throws RepositoryException {
        // adding basic resources.
        Resource testR = registry.newResource();
        testR.setContent("test R content");
        registry.put("/target/originalR", testR);
        
        Resource testR2 = registry.newResource();
        testR2.setContent("test R2 content");
        registry.put("/target/originalC/r2", testR2);

        Collection col = registry.newCollection();
        registry.put("/source", col);

        // creating the sym link
        registry.createLink("/source/symR", "/target/originalR");
        registry.createLink("/source/symC", "/target/originalC");

        // now just check the link is created
        Resource testRSym1 = registry.get("/source/symR");
        byte[] testRSym1Bytes = (byte[])testRSym1.getContent();
        Assert.assertEquals("test R content", RepositoryUtils.decodeBytes(testRSym1Bytes));

        Resource testR2Sym1 = registry.get("/source/symC/r2");
        byte[] testR2Sym1Bytes = (byte[])testR2Sym1.getContent();
        Assert.assertEquals("test R2 content", RepositoryUtils.decodeBytes(testR2Sym1Bytes));

        // now copy the source
        registry.rename("/source", "source-copy");

        // now check the copied symbolic links
        Resource testRSym2 = registry.get("/source-copy/symR");
        byte[] testRSym2Bytes = (byte[])testRSym2.getContent();
        Assert.assertEquals("test R content", RepositoryUtils.decodeBytes(testRSym2Bytes));

        Resource testR2Sym2 = registry.get("/source-copy/symC/r2");
        byte[] testR2Sym2Bytes = (byte[])testR2Sym2.getContent();
        Assert.assertEquals("test R2 content", RepositoryUtils.decodeBytes(testR2Sym2Bytes));

        // change the symbolic links and whether the original is updated
        testRSym2.setContent("test R updated content");
        registry.put("/source-copy/symR", testRSym2);
        testR2Sym2.setContent("test R2 updated content");
        registry.put("/source-copy/symC/r2", testR2Sym2);

        // and check whether the original is updated.
        testR = registry.get("/target/originalR");
        byte[] testRBytes = (byte[])testR.getContent();
        Assert.assertEquals("test R updated content", RepositoryUtils.decodeBytes(testRBytes));

        testR2 = registry.get("/target/originalC/r2");
        byte[] testR2Bytes = (byte[])testR2.getContent();
        Assert.assertEquals("test R2 updated content", RepositoryUtils.decodeBytes(testR2Bytes));

        // cleaning up for the next test case
        registry.delete("/source-copy");
        registry.delete("/target");
    }

    @Test
    public void testSymlinkOrder() throws RepositoryException {
        // here is the plan: we are creating following 2 circular symbolic links in reverse order
        // and get it working somehow (the following should be considered as
        // sym-link => real-path
        // 1. /Root_fake => /Root_real
        // 2. /Root_real/insiderA/resource_fake => /Root_fake/insiderB/insiderC/resource_real
        // so we will start with putting the following resources
        // 1. /Root_real/insiderA (collection)
        // 2. /Root_real/insiderB/insiderC/resource_real (resource).

        Collection c1 = registry.newCollection();
        registry.put("/Root_real/insiderA", c1);

        Resource r1 = registry.newResource();
        r1.setContent("guess me if you can");
        r1.setProperty("key1", "cycle-value1");
        r1.setProperty("key2", "cycle-value2");
        registry.put("/Root_real/insiderB/insiderC/resource_real", r1);

        // bang... now create the symbolic links in the opposite order

        registry.createLink("/Root_real/insiderA/resource_fake", "/Root_fake/insiderB/insiderC/resource_real");

        registry.createLink("/Root_fake", "/Root_real");

        // so just checking our circular symlink work just checking whether our resource can be
        // accessed through the first symlink.

        Resource r2 = registry.get("/Root_real/insiderA/resource_fake");
        Assert.assertTrue(!(r2 instanceof Collection));

        byte[] content = (byte[])r2.getContent();
        String contentStr = RepositoryUtils.decodeBytes(content);
        Assert.assertEquals("guess me if you can", contentStr);
        Assert.assertEquals("cycle-value1", r2.getPropertyValue("key1"));
        Assert.assertEquals("cycle-value2", r2.getPropertyValue("key2"));
    }

    @Test
    public void testSymlinkOrder2() throws RepositoryException {
        // here is the plan: we are creating following 2 circular symbolic links in reverse order
        // and get it working somehow (the following should be considered as
        // sym-link => real-path
        // 1. /Root_fake => /Root_real
        // 2. /Root_real/insiderA/resource_fake => /Root_fake/insiderB/insiderC/resource_real
        // so we will start with putting the following resources
        // 1. /Root_real/insiderA (collection)
        // 2. /Root_real/insiderB/insiderC/resource_real (resource).

        Collection c1 = registry.newCollection();
        registry.put("/myRoot/test", c1);

        registry.createLink("/mySymbolicLink", "/myRoot");

        Resource r1 = registry.newResource();
        r1.setContent("guess me if you can");
        r1.setProperty("key1", "cycle-value1");
        r1.setProperty("key2", "cycle-value2");
        registry.put("/mySymbolicLink/test", r1);

        Collection c2 = registry.newCollection();
        registry.put("/myRoot/aaa", c2);

        // bang... now create the symbolic links in the opposite order

        registry.createLink("/myRoot/aaa/symlink2",
                "/mySymbolicLink/test");
        
        Resource r2 = registry.get("/myRoot/aaa/symlink2");
        Assert.assertTrue(!(r2 instanceof Collection));

        byte[] content = (byte[])r2.getContent();
        String contentStr = RepositoryUtils.decodeBytes(content);
        Assert.assertEquals("guess me if you can", contentStr);
        Assert.assertEquals("cycle-value1", r2.getPropertyValue("key1"));
        Assert.assertEquals("cycle-value2", r2.getPropertyValue("key2"));
    }

    @Test
    public void testTransitiveSymLinks() throws RepositoryException {
        // create set of collections
        Resource r = registry.newResource();
        r.setContent("01");
        registry.put("/Root_01/r1", r);

        r.setContent("02");
        registry.put("/Root_02/r1", r);

        r.setContent("03");
        registry.put("/Root_03/r1", r);

        registry.createLink("/Root", "/Root_01");
        r = registry.get("/Root/r1");
        Assert.assertEquals("01", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        // dynamically change /Root to each of the above collections
        registry.createLink("/Root", "/Root_02");
        r = registry.get("/Root/r1");
        Assert.assertEquals("02", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        registry.createLink("/Root", "/Root_03");
        r = registry.get("/Root/r1");
        Assert.assertEquals("03", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        // create the transitive super link and check the behaviour by changing /Root symlink target
        registry.createLink("/super-link", "/Root/r1");

        registry.createLink("/Root", "/Root_03");
        r = registry.get("/super-link");
        Assert.assertEquals("03", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        registry.createLink("/Root", "/Root_02");
        r = registry.get("/super-link");
        Assert.assertEquals("02", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        registry.createLink("/Root", "/Root_01");
        r = registry.get("/super-link");
        Assert.assertEquals("01", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        // create a transitive and cyclic symlink at the same time and check the behaviour by
        // changing /Root
        registry.createLink("/Root_01/bang", "/Root/r1");

        registry.createLink("/Root", "/Root_03");
        r = registry.get("/Root_01/bang");
        Assert.assertEquals("03", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        registry.createLink("/Root", "/Root_02");
        r = registry.get("/Root_01/bang");
        Assert.assertEquals("02", RepositoryUtils.decodeBytes((byte[])r.getContent()));

        registry.createLink("/Root", "/Root_01");
        r = registry.get("/Root_01/bang");
        Assert.assertEquals("01", RepositoryUtils.decodeBytes((byte[])r.getContent()));
    }
}
