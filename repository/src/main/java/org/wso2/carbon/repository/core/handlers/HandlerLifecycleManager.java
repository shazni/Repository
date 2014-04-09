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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Filter;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.api.utils.Method;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.handlers.builtin.MountHandler;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * This class is used to manage handlers belonging to a particular lifecycle phase. There are three
 * defined phases, <b>default</b>, <b>reporting</b>, and <b>user</b>.
 */
public class HandlerLifecycleManager extends HandlerManager {

    private Dictionary<String, HandlerManager> handlerManagers;

    /**
     * This phase contains the default system-level handlers.
     */
    public static final String DEFAULT_SYSTEM_HANDLER_PHASE = "default";
    
    /**
     * This phase contains the tenant-specific system-level handlers.
     */
    public static final String TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE = "tenant";

    /**
     * This phase contains the tenant-specific system-level handlers.
     */
    public static final String USER_DEFINED_SYSTEM_HANDLER_PHASE = "system";

    /**
     * This phase contains all the reporting handlers (such as WS-Eventing & Notification
     * handlers).
     */
    public static final String DEFAULT_REPORTING_HANDLER_PHASE = "reporting";

    /**
     * This phase contains user-defined handlers.
     */
    public static final String USER_DEFINED_HANDLER_PHASE = "user";

    /**
     * This phase contains the handlers that will get executed after a successful commit.
     */
    public static final String COMMIT_HANDLER_PHASE = "commit";

    /**
     * This phase contains the handlers that will get executed after the rollback of a failed
     * commit.
     */
    public static final String ROLLBACK_HANDLER_PHASE = "rollback";

    private static final Log log = LogFactory.getLog(HandlerLifecycleManager.class);

    /**
     * Creates a Handler Manager for a given handler lifecycle phase.
     */
    public HandlerLifecycleManager() {
        handlerManagers = new Hashtable<String, HandlerManager>();
        
        HandlerManager defaultHandlerManager = new HandlerManager();
        defaultHandlerManager.setEvaluateAllHandlers(false);
        handlerManagers.put(DEFAULT_SYSTEM_HANDLER_PHASE, defaultHandlerManager);
        
        HandlerManager defaultTenantSpecificHandlerManager = new UserDefinedHandlerManager();
        defaultTenantSpecificHandlerManager.setEvaluateAllHandlers(false);
        handlerManagers.put(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE, defaultTenantSpecificHandlerManager);
        
        HandlerManager userDefinedSystemHandlerManager = new HandlerManager();
        userDefinedSystemHandlerManager.setEvaluateAllHandlers(false);
        handlerManagers.put(USER_DEFINED_SYSTEM_HANDLER_PHASE, userDefinedSystemHandlerManager);
        
        HandlerManager userDefinedHandlerManager = new UserDefinedHandlerManager();
        userDefinedHandlerManager.setEvaluateAllHandlers(false);
        handlerManagers.put(USER_DEFINED_HANDLER_PHASE, userDefinedHandlerManager);
        
        HandlerManager reportingHandlerManager = new HandlerManager();
        reportingHandlerManager.setEvaluateAllHandlers(true);
        handlerManagers.put(DEFAULT_REPORTING_HANDLER_PHASE, reportingHandlerManager);

        HandlerManager commitHandlerManager = new UserDefinedHandlerManager();
        commitHandlerManager.setEvaluateAllHandlers(true);
        handlerManagers.put(COMMIT_HANDLER_PHASE, commitHandlerManager);
        
        HandlerManager rollbackHandlerManager = new UserDefinedHandlerManager();
        rollbackHandlerManager.setEvaluateAllHandlers(true);
        handlerManagers.put(ROLLBACK_HANDLER_PHASE, rollbackHandlerManager);

        init(MultitenantConstants.SUPER_TENANT_ID);
    }

    /**
     * An initialization method to be called in a multi-tenant environment.
     *
     * @param tenantId the identifier of the tenant.
     */
    public synchronized void init(int tenantId) {
        CurrentContext.setCallerTenantId(tenantId);
        
        try {
            UserDefinedHandlerManager userDefinedHandlerManager = (UserDefinedHandlerManager) handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
            userDefinedHandlerManager.getUserHandlerManager();
            userDefinedHandlerManager = (UserDefinedHandlerManager) handlerManagers.get(USER_DEFINED_HANDLER_PHASE);
            userDefinedHandlerManager.getUserHandlerManager();
        } finally {
            CurrentContext.removeCallerTenantId();
        }
    }

    /**
     * Method to obtain the handler manager for the specified phase.
     *
     * @param lifecyclePhase The name of the lifecycle phase.
     *
     * @return the handler manager corresponding to the specified phase.
     */
    public HandlerManager getHandlerManagerForPhase(String lifecyclePhase) {
        if (lifecyclePhase.equals(COMMIT_HANDLER_PHASE) || lifecyclePhase.equals(ROLLBACK_HANDLER_PHASE)) {
            return handlerManagers.get(lifecyclePhase);
        } else {
            String msg = "Unable to provide handler manager for internally managed or invalid phase" + lifecyclePhase;
            log.error(msg);
            throw new SecurityException(msg);
        }
    }

    @Override
    public void addHandler(Method[] methods, Filter filter, Handler handler) {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).addHandler(methods, filter, handler);
    }
//
//    @Override
//    public void addHandlerWithPriority(String[] methods, Filter filter, Handler handler) {
//        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).addHandlerWithPriority(methods, filter, handler);
//    }

    @Override
    public void removeHandler(Handler handler) {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).removeHandler(handler);
    }

    @Override
    public void addHandler(Method[] methods, Filter filter, Handler handler, String lifecyclePhase) {
        if (lifecyclePhase == null) {
            addHandler(methods, filter, handler);
            return;
        }
        
        HandlerManager hm = handlerManagers.get(lifecyclePhase);
        
        if (hm == null) {
            log.warn("Invalid handler lifecycle phase: " + lifecyclePhase + ". Adding handler to the default phase.");
            addHandler(methods, filter, handler);
        } else {
            hm.addHandler(methods, filter, handler);
        }
    }

//    @Override
//    public void addHandlerWithPriority(String[] methods, Filter filter, Handler handler, String lifecyclePhase) {
//        if (lifecyclePhase == null) {
//            addHandlerWithPriority(methods, filter, handler);
//            return;
//        }
//
//        HandlerManager hm = handlerManagers.get(lifecyclePhase);
//
//        if (hm == null) {
//            log.warn("Invalid handler lifecycle phase: " + lifecyclePhase + ". Adding handler to the default phase.");
//            addHandlerWithPriority(methods, filter, handler);
//        } else {
//            hm.addHandlerWithPriority(methods, filter, handler, lifecyclePhase);
//        }
//    }

    @Override
    public void removeHandler(Handler handler, String lifecyclePhase) {
        if (lifecyclePhase == null) {
            removeHandler(handler);
            return;
        }
        
        HandlerManager hm = handlerManagers.get(lifecyclePhase);
        
        if (hm == null) {
            log.warn("Invalid handler lifecycle phase: " + lifecyclePhase + ". Removing handler from the default phase.");
            removeHandler(handler);
        } else {
            hm.removeHandler(handler, lifecyclePhase);
        }
    }

    @Override
    public void createVersion(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).createVersion(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).createVersion(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).createVersion(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).createVersion(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        // The reporting handler phase needs to know about the state of processing
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).createVersion(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public void restoreVersion(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).restoreVersion(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).restoreVersion(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).restoreVersion(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).restoreVersion(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        // The reporting handler phase needs to know about the state of processing
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).restoreVersion(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public void delete(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).delete(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).delete(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).delete(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).delete(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).delete(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public void putChild(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).putChild(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).putChild(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).putChild(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).putChild(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).putChild(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public void importChild(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).importChild(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).importChild(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).importChild(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).importChild(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).importChild(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public void createLink(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).createLink(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).createLink(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).createLink(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).createLink(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).createLink(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public void removeLink(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).removeLink(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).removeLink(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).removeLink(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).removeLink(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).removeLink(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public void restore(HandlerContext requestContext) throws RepositoryException {
        handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).restore(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).restore(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).restore(requestContext);
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        handlerManagers.get(USER_DEFINED_HANDLER_PHASE).restore(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).restore(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
    }

    @Override
    public String[] getVersions(HandlerContext requestContext) throws RepositoryException {
        String[] defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).getVersions(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            String[] tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).getVersions(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            String[] systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).getVersions(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        String[] userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).getVersions(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).getVersions(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public Collection executeQuery(HandlerContext requestContext) throws RepositoryException {
        Collection defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).executeQuery(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            Collection tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).executeQuery(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            Collection systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).executeQuery(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        Collection userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).executeQuery(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).executeQuery(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public Collection searchContent(HandlerContext requestContext) throws RepositoryException {
        Collection defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).searchContent(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            Collection tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).searchContent(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            Collection systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).searchContent(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        Collection userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).searchContent(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).searchContent(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public Resource get(HandlerContext requestContext) throws RepositoryException {
        Resource defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).get(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            Resource tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).get(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            Resource systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).get(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        Resource userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).get(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        Resource resourceOnContext = requestContext.getResource();
        Resource output = (userDefinedValue != null) ? userDefinedValue : defaultValue; requestContext.setResource(output);
        
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).get(requestContext);
        requestContext.setResource(resourceOnContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        return output;
    }

    @Override
    public String put(HandlerContext requestContext) throws RepositoryException {
        String defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).put(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            String tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).put(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            String systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).put(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        String userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).put(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        // The reporting handler phase needs to know about the state of processing
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).put(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public String importResource(HandlerContext requestContext) throws RepositoryException {
        String defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).importResource(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            String tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).importResource(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            String systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).importResource(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        String userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).importResource(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        requestContext.setProcessingComplete(isProcessingComplete);
        
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).importResource(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public String copy(HandlerContext requestContext) throws RepositoryException {
        String defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).copy(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            String tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).copy(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            String systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).copy(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        String userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).copy(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).copy(requestContext);
        // The reporting handlers may change the state of processing
        
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public String move(HandlerContext requestContext) throws RepositoryException {
        String defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).move(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            String tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).move(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            String systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).move(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        String userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).move(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).move(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public String rename(HandlerContext requestContext) throws RepositoryException {
        String defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).rename(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            String tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).rename(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            String systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).rename(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        String userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).rename(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).rename(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public Element dump(HandlerContext requestContext) throws RepositoryException {
    	Element defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).dump(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
        	Element tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).dump(requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
        	Element systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).dump(requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        Element userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).dump(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).dump(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }

    @Override
    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
        boolean defaultValue = handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE).resourceExists(requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            boolean tenantSpecificValue = handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE).resourceExists(requestContext);
            defaultValue = defaultValue || tenantSpecificValue;
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            boolean systemSpecificValue = handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE).resourceExists(requestContext);
            defaultValue = defaultValue || systemSpecificValue;
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        boolean userDefinedValue = handlerManagers.get(USER_DEFINED_HANDLER_PHASE).resourceExists(requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        // The reporting handler phase needs to know about the state of processing
        
        requestContext.setProcessingComplete(isProcessingComplete);
        handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE).resourceExists(requestContext);
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        requestContext.setProcessingComplete(isProcessingComplete);
        
        return userDefinedValue || defaultValue;
    }

    public RepositoryContext getRegistryContext(HandlerContext requestContext) throws RepositoryException {
        RepositoryContext defaultValue = getRegistryContext(handlerManagers.get(DEFAULT_SYSTEM_HANDLER_PHASE), requestContext);
        boolean isProcessingComplete = requestContext.isProcessingComplete();
        
        if (!isProcessingComplete) {
            RepositoryContext tenantSpecificValue = getRegistryContext(handlerManagers.get(TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE), requestContext);
            
            if (tenantSpecificValue != null) {
                defaultValue = tenantSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        if (!isProcessingComplete) {
            RepositoryContext systemSpecificValue = getRegistryContext(handlerManagers.get(USER_DEFINED_SYSTEM_HANDLER_PHASE), requestContext);
            
            if (systemSpecificValue != null) {
                defaultValue = systemSpecificValue;
            }
            
            isProcessingComplete = requestContext.isProcessingComplete();
        }
        
        requestContext.setProcessingComplete(false);
        RepositoryContext userDefinedValue = getRegistryContext(handlerManagers.get(USER_DEFINED_HANDLER_PHASE), requestContext);
        isProcessingComplete |= requestContext.isProcessingComplete();
        
        // The reporting handler phase needs to know about the state of processing
        requestContext.setProcessingComplete(isProcessingComplete);
        getRegistryContext(handlerManagers.get(DEFAULT_REPORTING_HANDLER_PHASE), requestContext);
        
        // The reporting handlers may change the state of processing
        isProcessingComplete |= requestContext.isProcessingComplete();
        requestContext.setProcessingComplete(isProcessingComplete);
        
        if (userDefinedValue != null) {
            return userDefinedValue;
        }
        
        return defaultValue;
    }
    
    /**
     * Manages the handler invocations of GET_REGISTRY_CONTEXT method.
     *
     * @param requestContext Details of the request.
     *
     * @return whether the resource exists
     */
    public RepositoryContext getRegistryContext(HandlerManager handlerManager,
                                                HandlerContext requestContext) throws RepositoryException {
        RepositoryContext registryContext = null;
        Set<Handler> handlerSet = handlerManager.getRegistryContextHandlerSet();
        if (handlerSet != null) {
            Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.RESTORE)) {
                    if(handler instanceof MountHandler) {
                        registryContext = ((MountHandler) handler).getRegistryContext(requestContext);
                    }
                    if (!requestContext.isExecutionStatusSet(handler)) {
                        requestContext.setExecutionStatus(handler, true);
                    }
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }
        }
        return registryContext;
    }
}