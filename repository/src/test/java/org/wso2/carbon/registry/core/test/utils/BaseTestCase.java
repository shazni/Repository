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

import java.io.File;
import java.io.InputStream;

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.core.EmbeddedRepositoryService;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.internal.RepositoryServiceComponent;

public class BaseTestCase {

    protected RepositoryContext ctx = null;
    protected InputStream is;
    
    protected static EmbeddedRepositoryService embeddedRegistryService = new EmbeddedRepositoryService();

    public void setUp() {
        setupCarbonHome();
        setupContext();
    }

    protected void setupContext() {
        try {
            String dbDirectory = "target/databasetest";
            
            if ((new File(dbDirectory)).exists()) {
                deleteDBDir(new File(dbDirectory));
            }
            
            is = this.getClass().getClassLoader().getResourceAsStream(System.getProperty("registry.config"));
            ctx = RepositoryContext.getBaseInstance(is, embeddedRegistryService);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
        ctx.setSetup(true);
        
        if(ctx.selectDBConfig("h2-db") == null) {
        	System.err.println("\nDatabase configuration is null");
        }
        
        try {
        	if(ctx != null) {
        		embeddedRegistryService.init(ctx);
        	} else {
        		System.err.print("Registry context is null. Therfore exiting");
        		System.exit(0);
        	}
		} catch (RepositoryException e) {
			e.printStackTrace();
		}	
    }

    protected void setupCarbonHome() {
        if (System.getProperty("carbon.home") == null) {
            File file = new File("../carbon-home/");
            if (file.exists()) {
                System.setProperty("carbon.home", file.getAbsolutePath());
            }
            file = new File("../carbon-home/");
            if (file.exists()) {
                System.setProperty("carbon.home", file.getAbsolutePath());
            }
        }
        
        // The line below is responsible for initializing the cache.
        CarbonContext.getThreadLocalCarbonContext();
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain("foo.com");
        PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(1);
    }
    
    /**
     * Delete the temporary database directory.
     *
     * @param dir database directory.
     *
     * @return true if the database directory was deleted
     */
    private static boolean deleteDBDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                if (!deleteDBDir(new File(dir, child))) {
                    return false;
                }
            }
        }
        
        return dir.delete();
    }

    public class RealmUnawareRegistryCoreServiceComponent extends RepositoryServiceComponent {

    }
}
