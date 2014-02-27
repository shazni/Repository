/*
 * Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;

/**
 * Testing import from a file and export to a file
 */
public class ResourceImportExportTest extends JDBCRegistryTest{
    protected static Repository originalRegistry = null;

    @BeforeTest
    public void setUp() {
        super.setUp();
        
        try {
        	originalRegistry = embeddedRegistryService.getRepository("admin");
        } catch (RepositoryException e) {
            e.printStackTrace();
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testImportResourceFromFile() throws RepositoryException {
    	RepositoryUtils.importToRegistry(new File("src/test/resources/import-resources/a.txt"), "/_system/governance/", originalRegistry);
        Resource resource = originalRegistry.get("/_system/governance/a.txt");
        String content = new String((byte[])resource.getContent());
        
        Assert.assertTrue("This is a sample content.".equals(content), "Resource content is wrong for the imported resource");

        RepositoryUtils.importToRegistry(new File("src/test/resources/import-resources/dir1"), "/_system/governance/", originalRegistry);
        resource = originalRegistry.get("/_system/governance/dir1/b.txt");
        content = new String((byte[])resource.getContent());
        Assert.assertTrue("This is a sample directory import.".equals(content), "Resource content is wrong for the imported resource");
    }

    @Test
    public void testExportResourceToFile() throws RepositoryException {
        Resource resource = originalRegistry.newResource();
        resource.setContent("This is a test content.");
        originalRegistry.put("/_system/governance/abc.txt", resource);

        File file = new File("abc.txt");
        RepositoryUtils.exportFromRegistry(file, "/_system/governance/abc.txt", originalRegistry);
        
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            char[] c = new char[30];
            reader.read(c);
            reader.close();
            Assert.assertTrue("This is a test content.".equals(new String(c).trim()), "Content different from actual and the exported");
        } catch (Exception e) {
            Assert.fail("File not exported properly:" + e.getMessage());
        }

        file.delete();
        
        resource = originalRegistry.newResource();
        resource.setContent("This is a test content.");
        originalRegistry.put("/_system/governance/test/abc.txt", resource);

        file = new File("test");
        file.mkdir();
        RepositoryUtils.exportFromRegistry(file, "/_system/governance/test", originalRegistry);

        try {
            file = new File("test/abc.txt");
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
            char[] c = new char[30];
            reader.read(c);
            file.delete();
            file = new File("test");
            file.delete();
            reader.close();
            Assert.assertTrue("This is a test content.".equals(new String(c).trim()), "Content different from actual and the exported");
        } catch (Exception e) {
            Assert.fail("File not exported properly:" + e.getMessage());
        }
    }
}
