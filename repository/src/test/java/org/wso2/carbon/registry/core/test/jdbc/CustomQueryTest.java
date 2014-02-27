/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.registry.core.test.jdbc;

import java.util.HashMap;
import java.util.LinkedList;
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

public class CustomQueryTest  extends BaseTestCase {

    protected static Repository registry = null;
    protected static Repository configSystemRegistry = null;

    @BeforeTest
    public void setUp() {
        super.setUp();

        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
            registry = embeddedRegistryService.getGovernanceUserRepository("admin");
            configSystemRegistry = embeddedRegistryService.getConfigSystemRepository();
        } catch (RepositoryException e){
        	Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testCustomQueryResultsOrderForResources() throws RepositoryException {
        Repository rootSystemRegistry = embeddedRegistryService.getSystemRepository();

        Resource r1 = rootSystemRegistry.newResource();
        r1.setDescription("r1 content");
        r1.setContent("r1 content");
        rootSystemRegistry.put("/test/resources/rx1", r1);
        Resource r2 = rootSystemRegistry.newResource();
        r2.setDescription("rq content");
        r2.setContent("r2 content");
        rootSystemRegistry.put("/test/resources/rx2", r2);
        Resource r0 = rootSystemRegistry.newResource();
        r0.setDescription("r0 content");
        r0.setContent("r0 content");
        rootSystemRegistry.put("/test/resources/rx0", r0);

        Resource comQuery = rootSystemRegistry.newResource();
        String sql = "SELECT REG_PATH_ID, REG_NAME FROM REG_RESOURCE R WHERE R.REG_DESCRIPTION LIKE ? ORDER BY R.REG_CREATED_TIME DESC";

        comQuery.setContent(sql);

        comQuery.setMediaType(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);

        rootSystemRegistry.put("/test/resources/q1", comQuery);

        Map<String, String> params = new HashMap<String, String>();
        params.put("1", "%content");
        Collection qResults = rootSystemRegistry.executeQuery("/test/resources/q1", params);

        String[] qPaths = (String[]) qResults.getContent();

        Assert.assertEquals(qPaths.length, 3, "Query result count should be 3");
        Assert.assertEquals(qPaths[0], "/test/resources/rx0", "Comment query result is invalid");
        Assert.assertEquals(qPaths[1], "/test/resources/rx2", "Comment query result is invalid");
        Assert.assertEquals(qPaths[2], "/test/resources/rx1", "Comment query result is invalid");

        comQuery = rootSystemRegistry.newResource();
        sql = "SELECT REG_PATH_ID, REG_NAME FROM REG_RESOURCE R ORDER BY R.REG_CREATED_TIME DESC";

        comQuery.setContent(sql);

        comQuery.setMediaType(RepositoryConstants.SQL_QUERY_MEDIA_TYPE);

        rootSystemRegistry.put("/test/resources/q1", comQuery);

        params = new HashMap<String, String>();

        qResults = rootSystemRegistry.executeQuery("/test/resources/q1", params);

        qPaths = (String[]) qResults.getContent();

        List<String> paths = new LinkedList<String>();
        
        for (String temp : qPaths) {
            if (temp.startsWith("/test/resources/rx")) {
                paths.add(temp);
            }
        }
        
        qPaths = paths.toArray(new String[paths.size()]);

        Assert.assertEquals(qPaths[0], "/test/resources/rx0", "Comment query result is invalid");
        Assert.assertEquals(qPaths[1], "/test/resources/rx2", "Comment query result is invalid");
        Assert.assertEquals(qPaths[2], "/test/resources/rx1", "Comment query result is invalid");
    }
}
