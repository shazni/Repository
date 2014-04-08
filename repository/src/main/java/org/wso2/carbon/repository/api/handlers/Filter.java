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

package org.wso2.carbon.repository.api.handlers;

import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.utils.METHODS;

/**
 * Base class of all filter implementations. All handlers have to be registered in the JDBC registry
 * with a filter implementation. Filter implementations determine the conditions to invoke the
 * associating handler.
 */
public abstract class Filter {

    /**
     * Whether to invert the result of the evaluated filter condition or not.
     */
    protected boolean invert = false;

    public abstract boolean filter (HandlerContext handlerContext, METHODS method) throws RepositoryException;

}
