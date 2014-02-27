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

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLStreamException;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.CollectionImpl;

public class DumpTest extends BaseTestCase {

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
    public void testResourceDump() throws RepositoryException, XMLStreamException {
        Resource r = registry.newResource();
        r.setProperty("key1", "value1");
        r.setProperty("key2", "value2");
        r.setContent("content 1");
        registry.put("/testDump", r);

        // doing the dump
        StringWriter writer = new StringWriter();
        registry.dump("/testDump", writer);
        Reader input = new StringReader(writer.toString());
        registry.restore("/testDumpDup", input);
        r = registry.get("/testDumpDup");

        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[])r.getContent()), "content 1");

        // checking the properties.
        Assert.assertEquals(r.getProperties().size(), 2);
        Assert.assertEquals(r.getProperty("key1"), "value1");
        Assert.assertEquals(r.getProperty("key2"), "value2");
    }

    @Test
    public void testCollectionDump() throws RepositoryException, XMLStreamException {
        Resource r = registry.newCollection();
        r.setProperty("key1", "value1");
        r.setProperty("key2", "value2");
        registry.put("/testDumpC", r);

        // adding children
        Resource r1 = registry.newCollection();
        r1.setProperty("key1", "value1C");
        r1.setProperty("key2", "value2C");
        registry.put("/testDumpC/child1C", r1);

        Resource r2 = registry.newResource();
        r2.setContent("content child2R");
        registry.put("/testDumpC/child2R", r2);

        // doing the dump
        StringWriter writer = new StringWriter();
        registry.dump("/testDumpC", writer);
        Reader input = new StringReader(writer.toString());

        // now restoring and retrieving the dumped element
        registry.restore("/testDumpDupC", input);
        r = registry.get("/testDumpDupC");

        // checking the properties.
        Assert.assertEquals(r.getProperties().size(), 2);
        Assert.assertEquals(r.getProperty("key1"), "value1");
        Assert.assertEquals(r.getProperty("key2"), "value2");

        // getting the children
        r1 = registry.get("/testDumpDupC/child1C");
        Assert.assertEquals(r1.getProperties().size(), 2);
        Assert.assertEquals(r1.getProperty("key1"), "value1C");
        Assert.assertEquals(r1.getProperty("key2"), "value2C");

        r2 = registry.get("/testDumpDupC/child2R");
        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[])r2.getContent()), "content child2R");
    }

    @Test
    public void testRootDump() throws RepositoryException, XMLStreamException {
        Resource r = registry.newCollection();
        r.setProperty("key1", "value1");
        r.setProperty("key2", "value2");
        registry.put("/", r);

        // adding children
        Resource r1 = registry.newCollection();
        r1.setProperty("key1", "value1C");
        r1.setProperty("key2", "value2C");
        registry.put("/child1CX", r1);

        Resource r2 = registry.newResource();
        r2.setContent("content child2R");
        registry.put("/child2RX", r2);

        // doing the dump
        StringWriter writer = new StringWriter();
        registry.dump("/", writer);
        Reader input = new StringReader(writer.toString());
        registry.restore("/testDumpDupR", input);
        r = registry.get("/testDumpDupR");

        // checking the properties.
        Assert.assertEquals(r.getProperties().size(), 2);
        Assert.assertEquals(r.getProperty("key1"), "value1");
        Assert.assertEquals(r.getProperty("key2"), "value2");
        
        // getting the children
        r1 = registry.get("/testDumpDupR/child1CX");
        Assert.assertEquals(r1.getProperties().size(), 2);
        Assert.assertEquals(r1.getProperty("key1"), "value1C");
        Assert.assertEquals(r1.getProperty("key2"), "value2C");

        r2 = registry.get("/testDumpDupR/child2RX");
        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[])r2.getContent()), "content child2R");
    }

    @Test
    public void testRootRestore() throws RepositoryException, XMLStreamException {
        Resource r = registry.newCollection();
        r.setProperty("key1", "value3");
        r.setProperty("key2", "value4");
        registry.put("/testSomewhereElse1", r);

        // adding children
        Resource r1 = registry.newCollection();
        r1.setProperty("key1", "value1C");
        r1.setProperty("key2", "value2C");
        registry.put("/testSomewhereElse1/child1CY", r1);

        Resource r2 = registry.newResource();
        r2.setContent("content child2R");
        registry.put("/testSomewhereElse1/child2RY", r2);

        Collection collection = registry.newCollection();
        registry.put("/anotherLocation", collection);

        // doing the dump
        StringWriter writer = new StringWriter();
        registry.dump("/testSomewhereElse1", writer);
        Reader input = new StringReader(writer.toString());
        registry.restore("/anotherLocation", input);
        r = registry.get("/anotherLocation");

        // checking the properties.
        Assert.assertEquals(r.getProperties().size(), 2);
        Assert.assertEquals(r.getProperty("key1"), "value3");
        Assert.assertEquals(r.getProperty("key2"), "value4");

        // getting the children
        r1 = registry.get("/anotherLocation/child1CY");
        Assert.assertEquals(r1.getProperties().size(), 2);
        Assert.assertEquals(r1.getProperty("key1"), "value1C");
        Assert.assertEquals(r1.getProperty("key2"), "value2C");

        r2 = registry.get("/anotherLocation/child2RY");
        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[])r2.getContent()), "content child2R");
    }

    @Test
    public void testSimpleNewRestore() throws RepositoryException, XMLStreamException {
        Resource r1 = registry.newCollection();
        r1.setProperty("key1", "value1C");
        r1.setProperty("key2", "value2C");
        registry.put("/testSomewhereElse2/child1CY/foo", r1);

        Collection collection = registry.newCollection();
        registry.put("/anotherLocation", collection);

        // doing the dump
        StringWriter writer = new StringWriter();
        registry.dump("/testSomewhereElse2", writer);
        Reader input = new StringReader(writer.toString());
        registry.restore("/anotherLocation", input);
        
        Resource r2 = registry.get("/anotherLocation/child1CY/foo");
        Assert.assertTrue((r2 instanceof CollectionImpl));
    }

    @Test
    public void testNewRestore() throws RepositoryException, XMLStreamException {
        Resource r = registry.newCollection();
        r.setProperty("key1", "value3");
        r.setProperty("key2", "value4");
        registry.put("/testSomewhereElse3", r);

        // adding children
        Resource r1 = registry.newCollection();
        r1.setProperty("key1", "value1C");
        r1.setProperty("key2", "value2C");
        registry.put("/testSomewhereElse3/child1CY", r1);
        
        r1 = registry.newCollection();
        r1.setProperty("key1", "value1C");
        r1.setProperty("key2", "value2C");
        registry.put("/testSomewhereElse3/child1CY/foo", r1);
        
        r1 = registry.newCollection();
        r1.setProperty("key1", "value1C");
        r1.setProperty("key2", "value2C");
        registry.put("/testSomewhereElse3/child1CY/bar", r1);

        Resource r2 = registry.newCollection();
        //r2.setContent("content child2R");
        registry.put("/testSomewhereElse3/newWWC1", r2);

        r2 = registry.newCollection();
        //r2.setContent("content child2R");
        registry.put("/testSomewhereElse3/newWWC2", r2);

        r2 = registry.newResource();
        r2.setContent("content child2R");
        registry.put("/testSomewhereElse3/child2RY", r2);

        Collection collection = registry.newCollection();
        registry.put("/anotherLocation", collection);

        // doing the dump
        StringWriter writer = new StringWriter();
        registry.dump("/testSomewhereElse3", writer);
        Reader input = new StringReader(writer.toString());
        registry.restore("/anotherLocation", input);
        r = registry.get("/anotherLocation");

        // checking the properties.
        Assert.assertEquals(r.getProperties().size(), 2);
        Assert.assertEquals(r.getProperty("key1"), "value3");
        Assert.assertEquals(r.getProperty("key2"), "value4");

        // getting the children
        r1 = registry.get("/anotherLocation/child1CY");
        Assert.assertEquals(r1.getProperties().size(), 2);
        Assert.assertEquals(r1.getProperty("key1"), "value1C");
        Assert.assertEquals(r1.getProperty("key2"), "value2C");

        r2 = registry.get("/anotherLocation/child1CY/foo");
        Assert.assertTrue((r2 instanceof CollectionImpl));
        r2 = registry.get("/anotherLocation/child1CY/bar");
        Assert.assertTrue((r2 instanceof CollectionImpl));

        r2 = registry.get("/anotherLocation/child2RY");
        Assert.assertEquals(RepositoryUtils.decodeBytes((byte[])r2.getContent()), "content child2R");
    }

    @Test
    public void testAbsoluteAssociationPath() throws Exception {
        Assert.assertEquals("/abc", RepositoryUtils.getAbsoluteAssociationPath("../abc", "/lm/pqr"));
        Assert.assertEquals("/abc/def", RepositoryUtils.getAbsoluteAssociationPath("../../../abc/def", "/lm/pqr/b/boo"));
        Assert.assertEquals("/abc/hag/def", RepositoryUtils.getAbsoluteAssociationPath("../hag/def", "/abc/boo/lm"));
        Assert.assertEquals("/abc", RepositoryUtils.getAbsoluteAssociationPath("abc", "/pqr"));
        Assert.assertEquals("/bloom/squid/abc", RepositoryUtils.getAbsoluteAssociationPath("squid/abc", "/bloom/squid2"));
        Assert.assertEquals("/abc", RepositoryUtils.getAbsoluteAssociationPath("abc", "/abc"));

        // go beyond cases
        Assert.assertEquals("//abc", RepositoryUtils.getAbsoluteAssociationPath("../../abc", "/lm/pqr"));
        Assert.assertEquals("///abc", RepositoryUtils.getAbsoluteAssociationPath("../../../abc", "/lm/pqr"));
        Assert.assertEquals("////abc", RepositoryUtils.getAbsoluteAssociationPath("../../../../abc", "/lm/pqr"));
    }

    @Test
    public void testRelativeAssociationPath() throws Exception {
        Assert.assertEquals("../abc", RepositoryUtils.getRelativeAssociationPath("/abc", "/lm/pqr"));
        Assert.assertEquals("../../../abc/def", RepositoryUtils.getRelativeAssociationPath("/abc/def", "/lm/pqr/b/boo"));
        Assert.assertEquals("../hag/def", RepositoryUtils.getRelativeAssociationPath("/abc/hag/def", "/abc/boo/lm"));
        Assert.assertEquals("abc", RepositoryUtils.getRelativeAssociationPath("/abc", "/pqr"));
        Assert.assertEquals("squid/abc", RepositoryUtils.getRelativeAssociationPath("/bloom/squid/abc", "/bloom/squid2"));
        Assert.assertEquals("abc", RepositoryUtils.getRelativeAssociationPath("/abc", "/abc"));

        Assert.assertEquals("../../abc", RepositoryUtils.getRelativeAssociationPath("//abc", "/lm/pqr"));
        Assert.assertEquals("../../../abc", RepositoryUtils.getRelativeAssociationPath("///abc", "/lm/pqr"));
        Assert.assertEquals("../../../../abc", RepositoryUtils.getRelativeAssociationPath("////abc", "/lm/pqr"));
    }

    @Test
    public void testDumpWithSymLink() throws Exception {
        Resource r = registry.newResource();
        r.setProperty("key1", "value3");
        r.setProperty("key2", "value4");
        registry.put("/my/original/link/resource", r);

        registry.createLink("/my/sym/link/resource", "/my/original/link/resource");

        // just check the sym
        Resource r2 = registry.get("/my/sym/link/resource");
        Assert.assertEquals("value3", r2.getProperty("key1"));
        Assert.assertEquals("value4", r2.getProperty("key2"));

        // now get a dump of /my
        StringWriter writer = new StringWriter();
        registry.dump("/my", writer);

        StringReader reader = new StringReader(writer.toString());
        // putting reader
        registry.restore("/restored", reader);

        Resource r3 =  registry.get("/restored/sym/link/resource");
        Assert.assertEquals("value3", r3.getProperty("key1"));
        Assert.assertEquals("value4", r3.getProperty("key2"));

        // do some changes to the original and check the sym link changing
        Resource r4 = registry.get("/restored/original/link/resource");
        r4.setProperty("key3", "value5");
        registry.put("/restored/original/link/resource", r4);

        Resource r5 =  registry.get("/restored/sym/link/resource");
        Assert.assertEquals("value5", r5.getProperty("key3"));
    }

    @Test
    public void testNewerVersionException() throws Exception {
        Resource r = registry.newResource();
        r.setContent("abc123");
        registry.put("/aaa3/bb/def", r);

        // now get a dump
        StringWriter writer = new StringWriter();
        registry.dump("/aaa3", writer);

        // now update the resource
        r.setContent("abc1234");
        registry.put("/aaa3/bb/def", r);

        String dumpStr = writer.toString();
        dumpStr = dumpStr.replaceAll("<resource", "<resource ignoreConflicts=\"false\"");
        StringReader reader = new StringReader(dumpStr);
        
        // putting reader
        try {
            registry.restore("/aaa3", reader);
            Assert.assertTrue(false);
        } catch (Exception e) {
        	Assert.assertTrue(true);
        }

        writer = new StringWriter();
        registry.dump("/aaa3", writer);
        reader = new StringReader(writer.toString());
        
        try {
            registry.restore("/aaa3", reader);
            Assert.assertTrue(true);
        } catch (Exception e) {
        	Assert.assertTrue(false);
        }
    }
}
