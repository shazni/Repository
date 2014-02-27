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

package org.wso2.carbon.registry.core.test.utils;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;

public class RegistryUtilsTest extends BaseTestCase {

	@Test
    public void testGetResourceName() {
    	Assert.assertEquals("/", RepositoryUtils.getResourceName("/"), "Resource name incorrect.");
    	Assert.assertEquals("a", RepositoryUtils.getResourceName("/a"), "Resource name incorrect.");
    	Assert.assertEquals("b", RepositoryUtils.getResourceName("/a/b"), "Resource name incorrect.");
    	Assert.assertEquals("a.txt", RepositoryUtils.getResourceName("/a/a.txt"), "Resource name incorrect.");
    	Assert.assertEquals("a", RepositoryUtils.getResourceName("/a/"), "Resource name incorrect.");
    	Assert.assertEquals("b", RepositoryUtils.getResourceName("/a/b/"), "Resource name incorrect.");
    }
}
