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

package org.wso2.carbon.repository.core.handlers;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Filter;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.api.utils.METHODS;
import org.wso2.carbon.repository.core.CurrentContext;

/**
 * This class is designed to manage user-defined handlers.
 */
public class UserDefinedHandlerManager extends HandlerManager {

    private Map<Integer, HandlerManager> userHandlerManagers = new HashMap<Integer, HandlerManager>();

    public HandlerManager getUserHandlerManager() {
        HandlerManager hm = userHandlerManagers.get(CurrentContext.getCallerTenantId());
        
        if (hm == null) {
            hm = new HandlerManager();
            userHandlerManagers.put(CurrentContext.getCallerTenantId(), hm);
        }
        
        return hm;
    }

    @Override
    public void addHandler(METHODS[] methods, Filter filter, Handler handler) {
        getUserHandlerManager().addHandler(methods, filter, handler);
    }

    @Override
    public void addHandler(METHODS[] methods, Filter filter, Handler handler, String lifecyclePhase) {
        getUserHandlerManager().addHandler(methods, filter, handler, lifecyclePhase);
    }

//    @Override
//    public void addHandlerWithPriority(String[] methods, Filter filter, Handler handler) {
//        getUserHandlerManager().addHandlerWithPriority(methods, filter, handler);
//    }
//
//    @Override
//    public void addHandlerWithPriority(String[] methods, Filter filter, Handler handler, String lifecyclePhase) {
//        getUserHandlerManager().addHandlerWithPriority(methods, filter, handler, lifecyclePhase);
//    }

    @Override
    public void removeHandler(Handler handler) {
        getUserHandlerManager().removeHandler(handler);
    }

    @Override
    public void removeHandler(Handler handler, String lifecyclePhase) {
        getUserHandlerManager().removeHandler(handler, lifecyclePhase);
    }

    @Override
    public void createVersion(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().createVersion(requestContext);
    }

    @Override
    public void restoreVersion(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().restoreVersion(requestContext);
    }

    @Override
    public String[] getVersions(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().getVersions(requestContext);
    }

    @Override
    public Collection executeQuery(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().executeQuery(requestContext);
    }

    @Override
    public Collection searchContent(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().searchContent(requestContext);
    }

    @Override
    public Resource get(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().get(requestContext);
    }

    @Override
    public String put(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().put(requestContext);
    }

    @Override
    public String importResource(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().importResource(requestContext);
    }

    @Override
    public void delete(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().delete(requestContext);
    }

    @Override
    public void putChild(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().putChild(requestContext);
    }

    @Override
    public void importChild(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().importChild(requestContext);
    }

    @Override
    public String copy(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().copy(requestContext);
    }

    @Override
    public String move(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().move(requestContext);
    }

    @Override
    public String rename(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().rename(requestContext);
    }

    @Override
    public void createLink(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().createLink(requestContext);
    }

    @Override
    public void removeLink(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().removeLink(requestContext);
    }

    @Override
    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().resourceExists(requestContext);
    }

    @Override
    public Element dump(HandlerContext requestContext) throws RepositoryException {
        return getUserHandlerManager().dump(requestContext);
    }

    @Override
    public void restore(HandlerContext requestContext) throws RepositoryException {
        getUserHandlerManager().restore(requestContext);
    }

    @Override
    public void setEvaluateAllHandlers(boolean evaluateAllHandlers) {
        getUserHandlerManager().setEvaluateAllHandlers(evaluateAllHandlers);
    }
}
