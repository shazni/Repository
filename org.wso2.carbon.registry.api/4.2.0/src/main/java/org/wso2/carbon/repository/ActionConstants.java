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

package org.wso2.carbon.repository;

/**
 * Action Constants, used to define actions associated with registry resources when defining
 * permissions.
 */
public class ActionConstants {
	
    /**
     * Action authorize
     */
    public static final String AUTHORIZE = "authorize";

    /**
     * Action of the getting resource from registry. (Registry.get action)
     */
    public static final String GET = "http://www.wso2.org/projects/registry/actions/get";

    /**
     * Action of the putting resource to registry. (Registry.put action)
     */
    public static final String PUT = "http://www.wso2.org/projects/registry/actions/add";

    /**
     * Action of deleting the resource from registry. (Registry.delete action)
     */
    public static final String DELETE = "http://www.wso2.org/projects/registry/actions/delete";
}
