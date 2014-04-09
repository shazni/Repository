/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.registry.core.test.utils.BaseTestCase;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.api.utils.Method;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.ResourceStorer;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.handlers.HandlerManager;
import org.wso2.carbon.repository.core.handlers.builtin.URLMatcher;
import org.wso2.carbon.repository.core.utils.InternalUtils;

public class SystemRegistryInHandlerTest  extends BaseTestCase {
//    RealmConfiguration realmConfig;

    @BeforeTest
    public void setUp() {
        super.setUp();

        try {
            RealmUnawareRegistryCoreServiceComponent comp = new RealmUnawareRegistryCoreServiceComponent();
            comp.registerBuiltInHandlers(embeddedRegistryService);
        } catch (RepositoryException e) {
            Assert.fail("Failed to initialize the registry. Caused by: " + e.getMessage());
        }
    }

    @Test
    public void testNestedRegistryOperations() throws RepositoryException {
    	Repository adminRegistry = embeddedRegistryService.getRepository("admin");
        
    	RepositoryContext registryContext = InternalUtils.getRepositoryContext(adminRegistry);
        MyPrivateHandler myPrivateHandler = new MyPrivateHandler();

        HandlerManager handlerManager = registryContext.getHandlerManager();

        URLMatcher myPrivateHandlerMatcher = new URLMatcher();
        myPrivateHandlerMatcher.setGetPattern(".*/to/my/private/handler");
        myPrivateHandlerMatcher.setPutPattern(".*/to/my/private/handler");
        handlerManager.addHandler(new Method[] {Method.GET, Method.PUT} , myPrivateHandlerMatcher, myPrivateHandler);

        Resource r = adminRegistry.newResource();
        String originalContent = "original content";
        r.setContent(originalContent.getBytes());

        adminRegistry.put("/to/my/private/handler", r);
        Resource rr = adminRegistry.get("/to/my/private/handler");

        byte[] newContent = (byte[])rr.getContent();
        String newContentString = RepositoryUtils.decodeBytes(newContent);

        String expectedString = "<adminRegistry-output><systemRegistry-output>" +
                            "<systemRegistry-input><adminRegistry-input>" +
                            originalContent +
                            "</adminRegistry-input></systemRegistry-input>" +
                            "</systemRegistry-output></adminRegistry-output>";

        Assert.assertEquals(expectedString, newContentString, "the returned content should be equal.");
    }

    private class MyPrivateHandler extends Handler {
    	
        public Resource get(HandlerContext requestContext) throws RepositoryException {
            String path = requestContext.getResourcePath().getPath();
            String currentUser = CurrentContext.getUser();
            
            if (currentUser.equals("admin")) {
                Repository systemRegistry = embeddedRegistryService.getSystemRepository();
                Resource r = systemRegistry.get(path);
                byte[] content = (byte[])r.getContent();
                String contentString = RepositoryUtils.decodeBytes(content);
                contentString = "<adminRegistry-output>" + contentString + "</adminRegistry-output>";
                r.setContent(contentString.getBytes());

                // check the current user again,
                String newCurrentUser = CurrentContext.getUser();
                Assert.assertEquals(newCurrentUser, currentUser, "The session user should be the same");

                return r;
            } else {            	            	
            	ResourceStorer repository = InternalUtils.getRepositoryContext(requestContext.getRepository()).getRepository();
                Resource r = repository.get(path);
            	
                byte[] content = (byte[])r.getContent();
                String contentString = RepositoryUtils.decodeBytes(content);
                contentString = "<systemRegistry-output>" + contentString + "</systemRegistry-output>";
                r.setContent(contentString.getBytes());

                String newCurrentUser = CurrentContext.getUser();
                Assert.assertEquals(newCurrentUser, currentUser, "The session user should be the same");
                
                return r;
            }
        }

        public void put(HandlerContext requestContext) throws RepositoryException {
            String path = requestContext.getResourcePath().getPath();
            Resource r = requestContext.getResource();
            String currentUser = CurrentContext.getUser();
            
            if (currentUser.equals("admin")) {
                Repository systemRegistry = embeddedRegistryService.getSystemRepository();
                byte[] content = (byte[])r.getContent();
                String contentString = RepositoryUtils.decodeBytes(content);
                contentString = "<adminRegistry-input>" + contentString + "</adminRegistry-input>";
                r.setContent(contentString.getBytes());
                systemRegistry.put(path, r);
            } else {
            	ResourceStorer repository = InternalUtils.getRepositoryContext(requestContext.getRepository()).getRepository();
                
            	byte[] content = (byte[])r.getContent();
                String contentString = RepositoryUtils.decodeBytes(content);
                contentString = "<systemRegistry-input>" + contentString + "</systemRegistry-input>";
                r.setContent(contentString.getBytes());                
                repository.put(path, r);
            }

            // check the current user again,
            String newCurrentUser = CurrentContext.getUser();
            Assert.assertEquals(newCurrentUser, currentUser, "The session user should be the same");
        }
    }
}
