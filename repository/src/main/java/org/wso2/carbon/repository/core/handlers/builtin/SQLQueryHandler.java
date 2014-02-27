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

package org.wso2.carbon.repository.core.handlers.builtin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;

/**
 * Handler managing adding and fetching SQL Queries.
 */
public class SQLQueryHandler extends Handler {

    private static final Log log = LogFactory.getLog(SQLQueryHandler.class);
    private ThreadLocal<Boolean> inExecution =
            new ThreadLocal<Boolean>() {
                protected Boolean initialValue() {
                    return false;
                }
            };

    private Boolean isInExecution() {
        return inExecution.get();
    }

    private void setInExecution(Boolean input) {
        inExecution.set(input);
    }

    public Resource get(HandlerContext requestContext) throws RepositoryException {
        if(isInExecution()){
            return super.get(requestContext);
        } else {
            setInExecution(true);
        }

        try {
            String queryPath = requestContext.getResourcePath().getPath();
            Resource resource = requestContext.getRepository().get(queryPath);
            Object content = resource.getContent();
            if (content instanceof byte[]) {
                resource.setContent(RepositoryUtils.decodeBytes((byte[]) content));
            }
            // else:
            // This case should not happen. If execution comes here
            // there must be something wrong when retrieving the content.
            // Note that this is tested for in SQLQueryProcessor

            requestContext.setProcessingComplete(true);
            
            return resource;
        }finally {
            setInExecution(false);
        }
    }

    public void put(HandlerContext requestContext) throws RepositoryException {
        if(isInExecution()){
            super.put(requestContext);
            return;
        } else {
            setInExecution(true);
        }

        try{
            Resource resource = requestContext.getResource();

            Object content = resource.getContent();
            
            if (content instanceof String) {
                String textContent = (String) content;
                resource.setContent(RepositoryUtils.encodeString(textContent));
            }

            String queryPath = requestContext.getResourcePath().getPath();
            requestContext.getRepository().put(queryPath, resource);
            requestContext.setProcessingComplete(true);
        } finally {
            setInExecution(false);
        }
    }
}
