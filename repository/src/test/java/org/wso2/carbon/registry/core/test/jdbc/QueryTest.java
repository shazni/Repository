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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;


public class QueryTest extends BaseTestCase {
    protected static Repository registry = null;
    protected static Repository systemRegistry = null;

    @BeforeTest
    public void setUp() {
        super.setUp();

        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getConfigUserRepository("admin");
            systemRegistry = embeddedRegistryService.getConfigSystemRepository();
        } catch (RepositoryException e) {
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testDefaultQuery() throws Exception {
        Resource r1 = registry.newResource();
        String r1Content = "this is r1 content";
        r1.setContent(r1Content.getBytes());
        r1.setDescription("production ready.");
        String r1Path = "/c3/r1";
        registry.put(r1Path, r1);

        Resource r2 = registry.newResource();
        String r2Content = "content for r2 :)";
        r2.setContent(r2Content);
        r2.setDescription("ready for production use.");
        String r2Path = "/c3/r2";
        registry.put(r2Path, r2);

        Resource r3 = registry.newResource();
        String r3Content = "content for r3 :)";
        r3.setContent(r3Content);
        r3.setDescription("only for government use.");
        String r3Path = "/c3/r3";
        registry.put(r3Path, r3);

        String sql1 = "SELECT RT.REG_TAG_ID FROM REG_RESOURCE_TAG RT, REG_RESOURCE R " +
                "WHERE (R.REG_VERSION=RT.REG_VERSION OR " +
                "(R.REG_PATH_ID=RT.REG_PATH_ID AND R.REG_NAME=RT.REG_RESOURCE_NAME)) " +
                "AND R.REG_DESCRIPTION LIKE ? ORDER BY RT.REG_TAG_ID";

        Resource q1 = systemRegistry.newResource();
        q1.setContent(sql1);
        q1.setMediaType(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);
        q1.addProperty(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME,
                RepositoryConstants.TAGS_RESULT_TYPE);
        systemRegistry.put("/qs/q3", q1);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("1", "%production%");
        Collection result = registry.executeQuery("/qs/q3", parameters);
    }

    @Test
    public void testWithSpecialCharactersQuery() throws Exception {
        Resource r1 = registry.newResource();
        String r1Content = "this is r1 content";
        r1.setContent(r1Content.getBytes());
        r1.setDescription("production ready.");
        String r1Path = "/c3/r1";
        registry.put(r1Path, r1);

        Resource r2 = registry.newResource();
        String r2Content = "content for r2 :)";
        r2.setContent(r2Content);
        r2.setDescription("ready for production use.");
        String r2Path = "/c3/r2";
        registry.put(r2Path, r2);

        Resource r3 = registry.newResource();
        String r3Content = "content for r3 :)";
        r3.setContent(r3Content);
        r3.setDescription("only for government use.");
        String r3Path = "/c3/r3";
        registry.put(r3Path, r3);

        String sql1 = "SELECT\nRT.REG_TAG_ID\nFROM REG_RESOURCE_TAG\nRT,\nREG_RESOURCE\nR\n" +
                "WHERE\n(R.REG_VERSION=RT.REG_VERSION\nOR\n" +
                "(R.REG_PATH_ID=RT.REG_PATH_ID\nAND\nR.REG_NAME=RT.REG_RESOURCE_NAME))\n" +
                "AND R.REG_DESCRIPTION\nLIKE\n?\nORDER BY\nRT.REG_TAG_ID";

        Resource q1 = systemRegistry.newResource();
        
        q1.setContent(sql1);
        q1.setMediaType(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);
        q1.addProperty(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME, RepositoryConstants.TAGS_RESULT_TYPE);
        
        systemRegistry.put("/qs/q3", q1);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("1", "%production%");
        Collection result = registry.executeQuery("/qs/q3", parameters);
    }

    @Test
    public void testWithoutTableParamsQuery() throws Exception {
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
    public void testWithoutWhereQuery() throws Exception {
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

        String sql1 = "SELECT REG_PATH_ID, REG_NAME FROM REG_RESOURCE";
        
        Resource q1 = systemRegistry.newResource();
        
        q1.setContent(sql1);
        q1.setMediaType(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);
        q1.addProperty(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME, RepositoryConstants.RESOURCES_RESULT_TYPE);
        
        systemRegistry.put("/qs/q1", q1);

        Map parameters = new HashMap();
        Resource result = registry.executeQuery("/qs/q1", parameters);

        Assert.assertTrue(result instanceof org.wso2.carbon.repository.api.Collection, "Search with result type Resource should return a directory.");

        String[] paths = (String[])result.getContent();
        Assert.assertTrue(paths.length >=3, "Should return all the resources");
    }
}
