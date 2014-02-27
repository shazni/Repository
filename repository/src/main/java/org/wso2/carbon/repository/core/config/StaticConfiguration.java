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

package org.wso2.carbon.repository.core.config;

import org.wso2.carbon.repository.core.config.StaticConfiguration;

/**
 * Class containing static (one-time) configuration information, which can be done before first
 * boot.
 */
public class StaticConfiguration {

    private static boolean versioningProperties = false;
    
    private static String repositoryRoot ;

    /**
     * Method to obtain whether properties are versioned.
     *
     * @return whether properties are versioned.
     */
    public static boolean isVersioningProperties() {
        return versioningProperties;
    }

    /**
     * Method to set whether properties are versioned.
     *
     * @param versioningProperties whether properties are versioned.
     */
    public static void setVersioningProperties(boolean versioningProperties) {
        StaticConfiguration.versioningProperties = versioningProperties;
    }
    
    /**
     * Method to set the repositoryRoot
     * 
     * @param repositoryRootVal repository root value
     */
    public static void setRepositoryRoot(String repositoryRootVal) {
    	repositoryRoot = repositoryRootVal ;
    }
    
    /**
     * Method to get the repositoryRoot root
     * 
     * @return configured repositoryRoot root
     */
    public static String getRepositoryRoot() {
    	return repositoryRoot ;
    }
}
