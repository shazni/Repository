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

package org.wso2.carbon.repository.core.handlers;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.handlers.builtin.SimulationFilter;

/**
 * Manages the handlers and their invocations. Handlers, which are required to be invoked must be
 * registered with the class using the {@link #addHandler} method. Handlers are allowed for a subset
 * of Registry API methods (see Java doc of Handler class for more details). Handler authors can
 * further specify exactly for which methods their handlers should engage. For example, handler
 * author may specify his handler should be engaged only for GET and DELETE operations. This class
 * maintains separate maps for each supported operation and puts handlers to corresponding maps once
 * registered.
 * <p/>
 * Handlers are registered with a Filter instance. Filter decides (based on the request) whether or
 * not to invoke its associated handler.
 * <p/>
 * This class has a separate method for each supported operation. These methods are invoked by
 * BasicRegistry once such method is invoked in the Registry API. Then this class iterates through
 * the map corresponding to the invoked method and evaluates filters of registered handlers. For
 * each filter which evaluates to true, its associated handler will be invoked. This process
 * continues either till there is no more handlers or till the processingComplete parameter of the
 * RequestContext is set to true.
 * <p/>
 * There is only one instance of this class exists per registry instance.
 */
public class HandlerManager {

    private static final Log log = LogFactory.getLog(HandlerManager.class);

    private static final String AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN = "An exception occurred while executing handler chain. ";
    private static final String UNABLE_TO_PROCEED_WITH_SIMULATION = "Unable to proceed with simulation";

    private Map<Filter, Set<Handler>> getHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> putHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> deleteHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> importHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> putChildHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> importChildHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> invokeAspectHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> moveHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> copyHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> renameHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> createLinkHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> removeLinkHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> addAssociationHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> removeAssociationHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getAssociationsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getAllAssociationsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> applyTagHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getResourcePathsWithTagHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getTagsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> removeTagHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> addCommentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> editCommentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> removeCommentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getCommentsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> rateResourceHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getAverageRatingHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getRatingHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> createVersionHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getVersionsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> restoreVersionHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> executeQueryHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> searchContentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> resourceExistsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> getRegistryContextHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> dumpMap = new LinkedHashMap<Filter, Set<Handler>>();
    private Map<Filter, Set<Handler>> restoreMap = new LinkedHashMap<Filter, Set<Handler>>();

    private boolean evaluateAllHandlers = false;

    /**
     * Registers handlers with the handler manager. Each handler should be registered with a Filter.
     * If a handler should be engaged only to a subset of allowed methods, those methods can be
     * specified as a string array.
     *
     * @param methods Methods for which the registered handler should be engaged. Allowed values in
     *                the string array are "GET", "PUT", "IMPORT", "DELETE", "PUT_CHILD",
     *                "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK", "REMOVE_LINK",
     *                "ADD_ASSOCIATION", "RESOURCE_EXISTS", "REMOVE_ASSOCIATION",
     *                "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS", "APPLY_TAG",
     *                "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG", "ADD_COMMENT",
     *                "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE", "GET_AVERAGE_RATING",
     *                "GET_RATING", "CREATE_VERSION", "GET_VERSIONS", "RESTORE_VERSION",
     *                "EXECUTE_QUERY", "SEARCH_CONTENT", and "INVOKE_ASPECT". If null is given,
     *                handler will be engaged to all methods.
     * @param filter  Filter instance associated with the handler.
     * @param handler Handler instance to be registered.
     */
    public synchronized void addHandler(String[] methods, Filter filter, Handler handler) {
        if (methods == null || RepositoryUtils.containsString(Filter.GET, methods)) {
            Set<Handler> handlers = getHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.PUT, methods)) {
            Set<Handler> handlers = putHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            putHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.DELETE, methods)) {
            Set<Handler> handlers = deleteHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            deleteHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.IMPORT, methods)) {
            Set<Handler> handlers = importHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            importHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.PUT_CHILD, methods)) {
            Set<Handler> handlers = putChildHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            putChildHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.IMPORT_CHILD, methods)) {
            Set<Handler> handlers = importChildHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            importChildHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.INVOKE_ASPECT, methods)) {
            Set<Handler> handlers = invokeAspectHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            invokeAspectHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.MOVE, methods)) {
            Set<Handler> handlers = moveHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            moveHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.COPY, methods)) {
            Set<Handler> handlers = copyHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            copyHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RENAME, methods)) {
            Set<Handler> handlers = renameHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            renameHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.CREATE_LINK, methods)) {
            Set<Handler> handlers = createLinkHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            createLinkHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_LINK, methods)) {
            Set<Handler> handlers = removeLinkHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            removeLinkHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.ADD_ASSOCIATION, methods)) {
            Set<Handler> handlers = addAssociationHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            addAssociationHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_ASSOCIATION, methods)) {
            Set<Handler> handlers = removeAssociationHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            removeAssociationHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_ASSOCIATIONS, methods)) {
            Set<Handler> handlers = getAssociationsHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getAssociationsHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_ALL_ASSOCIATIONS, methods)) {
            Set<Handler> handlers = getAllAssociationsHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getAllAssociationsHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.APPLY_TAG, methods)) {
            Set<Handler> handlers = applyTagHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            applyTagHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_RESOURCE_PATHS_WITH_TAG, methods)) {
            Set<Handler> handlers = getResourcePathsWithTagHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getResourcePathsWithTagHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_TAGS, methods)) {
            Set<Handler> handlers = getTagsHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getTagsHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_TAG, methods)) {
            Set<Handler> handlers = removeTagHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            removeTagHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.ADD_COMMENT, methods)) {
            Set<Handler> handlers = addCommentHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            addCommentHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.EDIT_COMMENT, methods)) {
            Set<Handler> handlers = editCommentHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            editCommentHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_COMMENT, methods)) {
            Set<Handler> handlers = removeCommentHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            removeCommentHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_COMMENTS, methods)) {
            Set<Handler> handlers = getCommentsHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getCommentsHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RATE_RESOURCE, methods)) {
            Set<Handler> handlers = rateResourceHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            rateResourceHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_AVERAGE_RATING, methods)) {
            Set<Handler> handlers = getAverageRatingHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getAverageRatingHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_RATING, methods)) {
            Set<Handler> handlers = getRatingHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getRatingHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.CREATE_VERSION, methods)) {
            Set<Handler> handlers = createVersionHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            createVersionHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_VERSIONS, methods)) {
            Set<Handler> handlers = getVersionsHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getVersionsHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RESTORE_VERSION, methods)) {
            Set<Handler> handlers = restoreVersionHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            restoreVersionHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.EXECUTE_QUERY, methods)) {
            Set<Handler> handlers = executeQueryHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            executeQueryHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.SEARCH_CONTENT, methods)) {
            Set<Handler> handlers = searchContentHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            searchContentHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RESOURCE_EXISTS, methods)) {
            Set<Handler> handlers = resourceExistsHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            resourceExistsHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_REGISTRY_CONTEXT, methods)) {
            Set<Handler> handlers = getRegistryContextHandlerMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            getRegistryContextHandlerMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.DUMP, methods)) {
            Set<Handler> handlers = dumpMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            dumpMap.put(filter, handlers);
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RESTORE, methods)) {
            Set<Handler> handlers = restoreMap.get(filter);
            
            if (handlers == null) {
                handlers = new LinkedHashSet<Handler>();
            }
            
            handlers.add(handler);
            restoreMap.put(filter, handlers);
        }

        String methodInfo;
        if (methods == null) {
            methodInfo = " all";
        } else {
            StringBuilder sb = new StringBuilder();
            
            for (String method : methods) {
                sb.append(" ").append(method);
            }
            
            methodInfo = sb.toString();
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Registered the handler " + filter.getClass().getName() + " --> " + handler.getClass().getName() + " for" + methodInfo + " methods.");
        }
    }

    /**
     * Registers handlers belonging to the given lifecycle phase with the handler manager. Each
     * handler should be registered with a Filter. If a handler should be engaged only to a subset
     * of allowed methods, those methods can be specified as a string array.
     *
     * @param methods        Methods for which the registered handler should be engaged. Allowed
     *                       values in the string array are "GET", "PUT", "IMPORT", "DELETE",
     *                       "PUT_CHILD", "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK",
     *                       "REMOVE_LINK", "ADD_ASSOCIATION", "RESOURCE_EXISTS",
     *                       "REMOVE_ASSOCIATION", "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS",
     *                       "APPLY_TAG", "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG",
     *                       "ADD_COMMENT", "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE",
     *                       "GET_AVERAGE_RATING", "GET_RATING", "CREATE_VERSION", "GET_VERSIONS",
     *                       "RESTORE_VERSION", "EXECUTE_QUERY", "SEARCH_CONTENT", and
     *                       "INVOKE_ASPECT". If null is given, handler will be engaged to all
     *                       methods.
     * @param filter         Filter instance associated with the handler.
     * @param lifecyclePhase The name of the lifecycle phase.
     * @param handler        Handler instance to be registered.
     */
    public void addHandler(String[] methods, Filter filter, Handler handler, String lifecyclePhase) {
        // We don't handle lifecycle phases in this Handler Manager. The Handler Lifecycle Manager
        // Does the required handling.
        addHandler(methods, filter, handler);
    }

    /**
     * This is to add a handler that invokes with the high priority, it becomes the first in the
     * list until another handler with this method is added.
     *
     * @param methods Methods for which the registered handler should be engaged. Allowed values in
     *                the string array are "GET", "PUT", "IMPORT", "DELETE", "PUT_CHILD",
     *                "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK", "REMOVE_LINK",
     *                "ADD_ASSOCIATION", "RESOURCE_EXISTS", "REMOVE_ASSOCIATION",
     *                "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS", "APPLY_TAG",
     *                "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG", "ADD_COMMENT",
     *                "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE", "GET_AVERAGE_RATING",
     *                "GET_RATING", "CREATE_VERSION", "GET_VERSIONS", "RESTORE_VERSION",
     *                "EXECUTE_QUERY", "SEARCH_CONTENT", and "INVOKE_ASPECT". If null is given,
     *                handler will be engaged to all methods.
     * @param filter  Filter instance associated with the handler.
     * @param handler Handler instance to be registered.
     */
    public synchronized void addHandlerWithPriority(String[] methods, Filter filter, Handler handler) {
        // creating temporarily references to the old handler
        Map<Filter, Set<Handler>> getHandlerMapTempRef = getHandlerMap;
        Map<Filter, Set<Handler>> putHandlerMapTempRef = putHandlerMap;
        Map<Filter, Set<Handler>> deleteHandlerMapTempRef = deleteHandlerMap;
        Map<Filter, Set<Handler>> importHandlerMapTempRef = importHandlerMap;
        Map<Filter, Set<Handler>> putChildHandlerMapTempRef = putChildHandlerMap;
        Map<Filter, Set<Handler>> importChildHandlerMapTempRef = importChildHandlerMap;
        Map<Filter, Set<Handler>> invokeAspectHandlerMapTempRef = invokeAspectHandlerMap;
        Map<Filter, Set<Handler>> moveHandlerMapTempRef = moveHandlerMap;
        Map<Filter, Set<Handler>> copyHandlerMapTempRef = copyHandlerMap;
        Map<Filter, Set<Handler>> renameHandlerMapTempRef = renameHandlerMap;
        Map<Filter, Set<Handler>> createLinkHandlerMapTempRef = createLinkHandlerMap;
        Map<Filter, Set<Handler>> removeLinkHandlerMapTempRef = removeLinkHandlerMap;
        Map<Filter, Set<Handler>> addAssociationHandlerMapTempRef = addAssociationHandlerMap;
        Map<Filter, Set<Handler>> removeAssociationHandlerMapTempRef = removeAssociationHandlerMap;
        Map<Filter, Set<Handler>> getAssociationsHandlerMapTempRef = getAssociationsHandlerMap;
        Map<Filter, Set<Handler>> getAllAssociationsHandlerMapTempRef = getAllAssociationsHandlerMap;
        Map<Filter, Set<Handler>> applyTagHandlerMapTempRef = applyTagHandlerMap;
        Map<Filter, Set<Handler>> getResourcePathsWithTagHandlerMapTempRef = getResourcePathsWithTagHandlerMap;
        Map<Filter, Set<Handler>> getTagsHandlerMapTempRef = getTagsHandlerMap;
        Map<Filter, Set<Handler>> removeTagHandlerMapTempRef = removeTagHandlerMap;
        Map<Filter, Set<Handler>> addCommentHandlerMapTempRef = addCommentHandlerMap;
        Map<Filter, Set<Handler>> editCommentHandlerMapTempRef = editCommentHandlerMap;
        Map<Filter, Set<Handler>> removeCommentHandlerMapTempRef = removeCommentHandlerMap;
        Map<Filter, Set<Handler>> getCommentsHandlerMapTempRef = getCommentsHandlerMap;
        Map<Filter, Set<Handler>> rateResourceHandlerMapTempRef = rateResourceHandlerMap;
        Map<Filter, Set<Handler>> getAverageRatingHandlerMapTempRef = getAverageRatingHandlerMap;
        Map<Filter, Set<Handler>> getRatingHandlerMapTempRef = getRatingHandlerMap;
        Map<Filter, Set<Handler>> createVersionHandlerMapTempRef = createVersionHandlerMap;
        Map<Filter, Set<Handler>> getVersionsHandlerMapTempRef = getVersionsHandlerMap;
        Map<Filter, Set<Handler>> restoreVersionHandlerMapTempRef = restoreVersionHandlerMap;
        Map<Filter, Set<Handler>> executeQueryHandlerMapTempRef = executeQueryHandlerMap;
        Map<Filter, Set<Handler>> searchContentHandlerMapTempRef = searchContentHandlerMap;
        Map<Filter, Set<Handler>> resourceExistsHandlerMapTempRef = resourceExistsHandlerMap;
        Map<Filter, Set<Handler>> getRegistryContextHandlerMapTempRef = getRegistryContextHandlerMap;
        Map<Filter, Set<Handler>> dumpMapTempRef = dumpMap;
        Map<Filter, Set<Handler>> restoreMapTempRef = restoreMap;

        // updating the class references with the new objects
        getHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        putHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        deleteHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        importHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        putChildHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        importChildHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        invokeAspectHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        moveHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        copyHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        renameHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        createLinkHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        removeLinkHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        addAssociationHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        removeAssociationHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getAssociationsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getAllAssociationsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        applyTagHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getResourcePathsWithTagHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getTagsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        removeTagHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        addCommentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        editCommentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        removeCommentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getCommentsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        rateResourceHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getAverageRatingHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getRatingHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        createVersionHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getVersionsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        restoreVersionHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        executeQueryHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        searchContentHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        resourceExistsHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        getRegistryContextHandlerMap = new LinkedHashMap<Filter, Set<Handler>>();
        dumpMap = new LinkedHashMap<Filter, Set<Handler>>();
        restoreMap = new LinkedHashMap<Filter, Set<Handler>>();

        // add the handler using the default method
        addHandler(methods, filter, handler);

        // copy old the handlers in old hashes to the new one
        appendHandlerMap(getHandlerMap, getHandlerMapTempRef);
        appendHandlerMap(putHandlerMap, putHandlerMapTempRef);
        appendHandlerMap(deleteHandlerMap, deleteHandlerMapTempRef);
        appendHandlerMap(importHandlerMap, importHandlerMapTempRef);
        appendHandlerMap(putChildHandlerMap, putChildHandlerMapTempRef);
        appendHandlerMap(importChildHandlerMap, importChildHandlerMapTempRef);
        appendHandlerMap(invokeAspectHandlerMap, invokeAspectHandlerMapTempRef);
        appendHandlerMap(moveHandlerMap, moveHandlerMapTempRef);
        appendHandlerMap(copyHandlerMap, copyHandlerMapTempRef);
        appendHandlerMap(renameHandlerMap, renameHandlerMapTempRef);
        appendHandlerMap(createLinkHandlerMap, createLinkHandlerMapTempRef);
        appendHandlerMap(removeLinkHandlerMap, removeLinkHandlerMapTempRef);
        appendHandlerMap(addAssociationHandlerMap, addAssociationHandlerMapTempRef);
        appendHandlerMap(removeAssociationHandlerMap, removeAssociationHandlerMapTempRef);
        appendHandlerMap(getAssociationsHandlerMap, getAssociationsHandlerMapTempRef);
        appendHandlerMap(getAllAssociationsHandlerMap, getAllAssociationsHandlerMapTempRef);
        appendHandlerMap(applyTagHandlerMap, applyTagHandlerMapTempRef);
        appendHandlerMap(getResourcePathsWithTagHandlerMap, getResourcePathsWithTagHandlerMapTempRef);
        appendHandlerMap(getTagsHandlerMap, getTagsHandlerMapTempRef);
        appendHandlerMap(removeTagHandlerMap, removeTagHandlerMapTempRef);
        appendHandlerMap(addCommentHandlerMap, addCommentHandlerMapTempRef);
        appendHandlerMap(editCommentHandlerMap, editCommentHandlerMapTempRef);
        appendHandlerMap(removeCommentHandlerMap, removeCommentHandlerMapTempRef);
        appendHandlerMap(getCommentsHandlerMap, getCommentsHandlerMapTempRef);
        appendHandlerMap(rateResourceHandlerMap, rateResourceHandlerMapTempRef);
        appendHandlerMap(getAverageRatingHandlerMap, getAverageRatingHandlerMapTempRef);
        appendHandlerMap(getRatingHandlerMap, getRatingHandlerMapTempRef);
        appendHandlerMap(createVersionHandlerMap, createVersionHandlerMapTempRef);
        appendHandlerMap(getVersionsHandlerMap, getVersionsHandlerMapTempRef);
        appendHandlerMap(restoreVersionHandlerMap, restoreVersionHandlerMapTempRef);
        appendHandlerMap(executeQueryHandlerMap, executeQueryHandlerMapTempRef);
        appendHandlerMap(searchContentHandlerMap, searchContentHandlerMapTempRef);
        appendHandlerMap(resourceExistsHandlerMap, resourceExistsHandlerMapTempRef);
        appendHandlerMap(getRegistryContextHandlerMap, getRegistryContextHandlerMapTempRef);
        appendHandlerMap(dumpMap, dumpMapTempRef);
        appendHandlerMap(restoreMap, restoreMapTempRef);
    }

    /**
     * This is to add a handler belonging to the given lifecycle phase that invokes with the high
     * priority, it becomes the first in the list until another handler with this method is added.
     *
     * @param methods        Methods for which the registered handler should be engaged. Allowed
     *                       values in the string array are "GET", "PUT", "IMPORT", "DELETE",
     *                       "PUT_CHILD", "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK",
     *                       "REMOVE_LINK", "ADD_ASSOCIATION", "RESOURCE_EXISTS",
     *                       "REMOVE_ASSOCIATION", "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS",
     *                       "APPLY_TAG", "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG",
     *                       "ADD_COMMENT", "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE",
     *                       "GET_AVERAGE_RATING", "GET_RATING", "CREATE_VERSION", "GET_VERSIONS",
     *                       "RESTORE_VERSION", "EXECUTE_QUERY", "SEARCH_CONTENT", and
     *                       "INVOKE_ASPECT". If null is given, handler will be engaged to all
     *                       methods.
     * @param filter         Filter instance associated with the handler.
     * @param lifecyclePhase The name of the lifecycle phase.
     * @param handler        Handler instance to be registered.
     */
    public void addHandlerWithPriority(String[] methods, Filter filter, Handler handler, String lifecyclePhase) {
        // We don't handle lifecycle phases in this Handler Manager. The Handler Lifecycle Manager
        // Does the required handling.
        addHandlerWithPriority(methods, filter, handler);
    }

    // append the entries in the handler map2 to handler map1 => map1 += map2;
    private void appendHandlerMap(Map<Filter, Set<Handler>> map1, Map<Filter, Set<Handler>> map2) {
        for (Map.Entry<Filter, Set<Handler>> entry : map2.entrySet()) {
            Filter f = entry.getKey();
            Set<Handler> sourceHandlerSet = entry.getValue();
            Set<Handler> targetHandlerSet = map1.get(f);
            
            if (targetHandlerSet == null) {
                // even the filter is not added, so add the full entry to the map1
                map1.put(f, sourceHandlerSet);
            } else {
                // the filter is already there, so we are just adding the entries in the
                // sourceHandlerSet to targetHandlerSet
                targetHandlerSet.addAll(sourceHandlerSet);
            }
        }
    }

    /**
     * remove a handler from all the filters, all the methods
     *
     * @param handler the handler to remove
     */
    public synchronized void removeHandler(Handler handler) {
        for (Map.Entry<Filter, Set<Handler>> entry : getHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : putHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : deleteHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : importHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : putChildHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : importChildHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : invokeAspectHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : moveHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : copyHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : renameHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : createLinkHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : createLinkHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : removeLinkHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }


        for (Map.Entry<Filter, Set<Handler>> entry : addAssociationHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : removeAssociationHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : applyTagHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : removeTagHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : rateResourceHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : restoreVersionHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : createVersionHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : editCommentHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : removeCommentHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getAssociationsHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getAllAssociationsHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getResourcePathsWithTagHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getTagsHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : addCommentHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getCommentsHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getRatingHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getAverageRatingHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getVersionsHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : executeQueryHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : searchContentHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : resourceExistsHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        for (Map.Entry<Filter, Set<Handler>> entry : getRegistryContextHandlerMap.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().remove(handler);
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Removed the handler " + handler.getClass().getName() + " for all methods.");
        }
    }

    /**
     * remove a handler belonging to the given lifecycle phase from all the filters, all the
     * methods
     *
     * @param handler        the handler to remove.
     * @param lifecyclePhase The name of the lifecycle phase.
     */
    public void removeHandler(Handler handler, String lifecyclePhase) {
        // We don't handle lifecycle phases in this Handler Manager. The Handler Lifecycle Manager
        // Does the required handling.
        removeHandler(handler);
    }

    /**
     * Removes handlers with the handler manager. Each handler should be registered with a Filter.
     * If a handler should be disengaged only for a subset of allowed methods, those methods can be
     * specified as a string array.
     *
     * @param methods Methods for which the registered handler should be disengaged. Allowed values
     *                in the string array are "GET", "PUT", "IMPORT", "DELETE", "PUT_CHILD",
     *                "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK", "REMOVE_LINK",
     *                "ADD_ASSOCIATION", "RESOURCE_EXISTS", "REMOVE_ASSOCIATION",
     *                "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS", "APPLY_TAG",
     *                "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG", "ADD_COMMENT",
     *                "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE", "GET_AVERAGE_RATING",
     *                "GET_RATING", "CREATE_VERSION", "GET_VERSIONS", "RESTORE_VERSION",
     *                "EXECUTE_QUERY", "SEARCH_CONTENT", and "INVOKE_ASPECT". If null is given,
     *                handler will be disengaged to all methods.
     * @param filter  Filter instance associated with the handler. Each filter that you pass in must
     *                have the associated handler set to it.
     * @param handler Handler instance to be unregistered.
     */
    public synchronized void removeHandler(String[] methods, Filter filter, Handler handler) {
        if (methods == null || RepositoryUtils.containsString(Filter.GET, methods)) {
            Set<Handler> handlers = getHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.PUT, methods)) {
            Set<Handler> handlers = putHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.DELETE, methods)) {
            Set<Handler> handlers = deleteHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.IMPORT, methods)) {
            Set<Handler> handlers = importHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.PUT_CHILD, methods)) {
            Set<Handler> handlers = putChildHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.IMPORT_CHILD, methods)) {
            Set<Handler> handlers = importChildHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.INVOKE_ASPECT, methods)) {
            Set<Handler> handlers = invokeAspectHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.MOVE, methods)) {
            Set<Handler> handlers = moveHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.COPY, methods)) {
            Set<Handler> handlers = copyHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RENAME, methods)) {
            Set<Handler> handlers = renameHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.CREATE_LINK, methods)) {
            Set<Handler> handlers = createLinkHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_LINK, methods)) {
            Set<Handler> handlers = removeLinkHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.ADD_ASSOCIATION, methods)) {
            Set<Handler> handlers = addAssociationHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_ASSOCIATION, methods)) {
            Set<Handler> handlers = removeAssociationHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.APPLY_TAG, methods)) {
            Set<Handler> handlers = applyTagHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_TAG, methods)) {
            Set<Handler> handlers = removeTagHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RATE_RESOURCE, methods)) {
            Set<Handler> handlers = rateResourceHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RESTORE_VERSION, methods)) {
            Set<Handler> handlers = restoreVersionHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.CREATE_VERSION, methods)) {
            Set<Handler> handlers = createVersionHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.EDIT_COMMENT, methods)) {
            Set<Handler> handlers = editCommentHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.REMOVE_COMMENT, methods)) {
            Set<Handler> handlers = removeCommentHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_ASSOCIATIONS, methods)) {
            Set<Handler> handlers = getAssociationsHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_ALL_ASSOCIATIONS, methods)) {
            Set<Handler> handlers = getAllAssociationsHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null ||
                RepositoryUtils.containsString(Filter.GET_RESOURCE_PATHS_WITH_TAG, methods)) {
            Set<Handler> handlers = getResourcePathsWithTagHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_TAGS, methods)) {
            Set<Handler> handlers = getTagsHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.ADD_COMMENT, methods)) {
            Set<Handler> handlers = addCommentHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_COMMENTS, methods)) {
            Set<Handler> handlers = getCommentsHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_RATING, methods)) {
            Set<Handler> handlers = getRatingHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_AVERAGE_RATING, methods)) {
            Set<Handler> handlers = getAverageRatingHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_VERSIONS, methods)) {
            Set<Handler> handlers = getVersionsHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.EXECUTE_QUERY, methods)) {
            Set<Handler> handlers = executeQueryHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.SEARCH_CONTENT, methods)) {
            Set<Handler> handlers = searchContentHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.RESOURCE_EXISTS, methods)) {
            Set<Handler> handlers = resourceExistsHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        if (methods == null || RepositoryUtils.containsString(Filter.GET_REGISTRY_CONTEXT, methods)) {
            Set<Handler> handlers = getRegistryContextHandlerMap.get(filter);
            if (handlers != null) {
                handlers.remove(handler);
            }
        }

        String methodInfo;
        if (methods == null) {
            methodInfo = " all";
        } else {
            StringBuilder sb = new StringBuilder();
            for (String method : methods) {
                sb.append(" ").append(method);
            }
            methodInfo = sb.toString();
        }
        if (log.isDebugEnabled()) {
            log.debug("Removed the handler " + filter.getClass().getName() + " --> " + handler.getClass().getName() + " for" + methodInfo + " methods.");
        }
    }

    /**
     * Removes handlers belonging to the given lifecycle phase with the handler manager. Each
     * handler should be registered with a Filter. If a handler should be disengaged only for a
     * subset of allowed methods, those methods can be specified as a string array.
     *
     * @param methods        Methods for which the registered handler should be disengaged. Allowed
     *                       values in the string array are "GET", "PUT", "IMPORT", "DELETE",
     *                       "PUT_CHILD", "IMPORT_CHILD", "MOVE", "COPY", "RENAME", "CREATE_LINK",
     *                       "REMOVE_LINK", "ADD_ASSOCIATION", "RESOURCE_EXISTS",
     *                       "REMOVE_ASSOCIATION", "GET_ASSOCIATIONS", "GET_ALL_ASSOCIATIONS",
     *                       "APPLY_TAG", "GET_RESOURCE_PATHS_WITH_TAG", "GET_TAGS", "REMOVE_TAG",
     *                       "ADD_COMMENT", "EDIT_COMMENT", "GET_COMMENTS", "RATE_RESOURCE",
     *                       "GET_AVERAGE_RATING", "GET_RATING", "CREATE_VERSION", "GET_VERSIONS",
     *                       "RESTORE_VERSION", "EXECUTE_QUERY", "SEARCH_CONTENT", and
     *                       "INVOKE_ASPECT". If null is given, handler will be disengaged to all
     *                       methods.
     * @param filter         Filter instance associated with the handler. Each filter that you pass
     *                       in must have the associated handler set to it.
     * @param handler        Handler instance to be unregistered.
     * @param lifecyclePhase The name of the lifecycle phase.
     */
    public void removeHandler(String[] methods, Filter filter, Handler handler, String lifecyclePhase) {
        // We don't handle lifecycle phases in this Handler Manager. The Handler Lifecycle Manager
        // Does the required handling.
        removeHandler(methods, filter, handler);
    }

    /**
     * Determines whether the processing of the request is completed or not.
     *
     * @param requestContext Details of the request.
     *
     * @return True if processing is complete and in simulation mode, or if processing is complete
     *         and if all handlers belonging to this handler manager needs not be evaluated.
     */
    public boolean isProcessingComplete(HandlerContext requestContext) {
        return requestContext.isProcessingComplete() && (SimulationFilter.isSimulation() || !evaluateAllHandlers);
    }

    /**
     * Manages the handler invocations of CREATE_VERSION method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void createVersion(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = createVersionHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleCreateVersion(requestContext)) {
                Set<Handler> handlerSet = createVersionHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.createVersion(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    /**
     * Manages the handler invocations of RESTORE_VERSION method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void restoreVersion(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = restoreVersionHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleRestoreVersion(requestContext)) {
                Set<Handler> handlerSet = restoreVersionHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.restoreVersion(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    /**
     * Manages the handler invocations of GET_VERSIONS method.
     *
     * @param requestContext Details of the request.
     *
     * @return Versions.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public String[] getVersions(HandlerContext requestContext) throws RepositoryException {
        String[] versions = null;
        Set<Filter> filters = getVersionsHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleGetVersions(requestContext)) {
                Set<Handler> handlerSet = getVersionsHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        versions = handler.getVersions(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }

        return versions;
    }

    /**
     * Manages the handler invocations of EXECUTE_QUERY method.
     *
     * @param requestContext Details of the request.
     *
     * @return Collection of results.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public Collection executeQuery(HandlerContext requestContext) throws RepositoryException {
        Collection collection = null;
        List<String> results = new LinkedList<String>();
        Set<Filter> filters = executeQueryHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleExecuteQuery(requestContext)) {
                Set<Handler> handlerSet = executeQueryHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        collection = handler.executeQuery(requestContext);
                        if (collection != null) {
                            String[] children = collection.getChildPaths();
                            if (children != null) {
                                for (String child : children) {
                                    if (child != null) {
                                        results.add(child);
                                    }
                                }
                            }
                        }
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
        
        if (collection != null) {
            collection.setContent(results.toArray(new String[results.size()]));
        } else if (results.size() > 0) {
        	collection = requestContext.getRepository().getRepositoryService().newCollection(results.toArray(new String[results.size()]));
        }
        
        return collection;
    }

    /**
     * Manages the handler invocations of SEARCH_CONTENT method.
     *
     * @param requestContext Details of the request.
     *
     * @return Collection of content.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public Collection searchContent(HandlerContext requestContext) throws RepositoryException {
        Collection collection = null;
        Set<Filter> filters = searchContentHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleSearchContent(requestContext)) {
                Set<Handler> handlerSet = searchContentHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        collection = handler.searchContent(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }

        return collection;
    }

    /**
     * Manages the handler invocations of GET method.
     *
     * @param requestContext Details of the request.
     *
     * @return Resource requested from the GET operation.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public Resource get(HandlerContext requestContext) throws RepositoryException {
        Resource resource = null;
        Set<Filter> filters = getHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleGet(requestContext)) {
                Set<Handler> handlerSet = getHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        resource = handler.get(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }

        return resource;
    }

    /**
     * Manages the handler invocations of PUT method.
     *
     * @param requestContext Details of the request.
     *
     * @return Path where the resource is actually stored. Subsequent accesses to the resource
     *         should use this path.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public String put(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = putHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handlePut(requestContext)) {
                Set<Handler> handlerSet = putHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.put(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }

        if (isProcessingComplete(requestContext)) {
            return requestContext.getActualPath();
        } else {
            return null;
        }
    }

    /**
     * Manages the handler invocations of IMPORT method.
     *
     * @param requestContext Details of the request.
     *
     * @return Path where the resource is actually stored. Subsequent accesses to the resource
     *         should use this path.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public String importResource(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = importHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleImportResource(requestContext)) {
                Set<Handler> handlerSet = importHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.importResource(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }

        if (isProcessingComplete(requestContext)) {
            return requestContext.getActualPath();
        } else {
            return null;
        }
    }

    /**
     * Manages the handler invocations of DELETE method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void delete(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = deleteHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleDelete(requestContext)) {
                Set<Handler> handlerSet = deleteHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.delete(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    /**
     * Manages the handler invocations of PUT_CHILD method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void putChild(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = putChildHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handlePutChild(requestContext)) {
                Set<Handler> handlerSet = putChildHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.putChild(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    /**
     * Manages the handler invocations of IMPORT_CHILD method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void importChild(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = importChildHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleImportChild(requestContext)) {
                Set<Handler> handlerSet = importChildHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.importChild(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    /**
     * Manages the handler invocations of COPY method.
     *
     * @param requestContext Details of the request.
     *
     * @return the actual path for the new resource.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public String copy(HandlerContext requestContext) throws RepositoryException {
        String copiedPath = null;
        Set<Filter> filters = copyHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleCopy(requestContext)) {
                Set<Handler> handlerSet = copyHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        copiedPath = handler.copy(requestContext);
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
        
        return copiedPath;
    }

    /**
     * Manages the handler invocations of MOVE method.
     *
     * @param requestContext Details of the request.
     *
     * @return the actual path for the new resource.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public String move(HandlerContext requestContext) throws RepositoryException {
        String movedPath = null;
        Set<Filter> filters = moveHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleMove(requestContext)) {
                Set<Handler> handlerSet = moveHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        movedPath = handler.move(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
        
        return movedPath;
    }

    /**
     * Manages the handler invocations of RENAME method.
     *
     * @param requestContext Details of the request.
     *
     * @return the actual path for the new resource.
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public String rename(HandlerContext requestContext) throws RepositoryException {
        String renamedPath = null;
        Set<Filter> filters = renameHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleRename(requestContext)) {
                Set<Handler> handlerSet = renameHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        renamedPath = handler.rename(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
        
        return renamedPath;
    }

    /**
     * Manages the handler invocations of CREATE_LINK method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void createLink(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = createLinkHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleCreateLink(requestContext)) {
                Set<Handler> handlerSet = createLinkHandlerMap.get(filter);
                
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.createLink(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    /**
     * Manages the handler invocations of REMOVE_LINK method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void removeLink(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = removeLinkHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleRemoveLink(requestContext)) {
                Set<Handler> handlerSet = removeLinkHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.removeLink(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    /**
     * Manages the handler invocations of RESOURCE_EXISTS method.
     *
     * @param requestContext Details of the request.
     *
     * @return whether the resource exists
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
        boolean resourceExist = false;
        Set<Filter> filters = resourceExistsHandlerMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleResourceExists(requestContext)) {
                Set<Handler> handlerSet = resourceExistsHandlerMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        resourceExist = handler.resourceExists(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return false;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return false;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }

        return resourceExist;
    }

    /**
     * Manages the handler invocations of DUMP method.
     *
     * @param requestContext Details of the request.
     *
     * @return the dumped element
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public Element dump(HandlerContext requestContext) throws RepositoryException {
    	Element dumpedElement = null;
        Set<Filter> filters = dumpMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleDump(requestContext)) {
                Set<Handler> handlerSet = dumpMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.dump(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return null;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
        
        return dumpedElement;
    }

    /**
     * Manages the handler invocations of RESTORE method.
     *
     * @param requestContext Details of the request.
     *
     * @throws RepositoryException This exception is thrown for all exceptions occurred inside
     *                           handlers or filters.
     */
    public void restore(HandlerContext requestContext) throws RepositoryException {
        Set<Filter> filters = restoreMap.keySet();
        
        for (Filter filter : filters) {
            if (filter != null && filter.handleRestore(requestContext)) {
                Set<Handler> handlerSet = restoreMap.get(filter);
                Handler[] handlers = handlerSet.toArray(new Handler[handlerSet.size()]);
                
                for (Handler handler : handlers) {
                    try {
                        handler.restore(requestContext);
                        
                        if (!requestContext.isExecutionStatusSet(handler)) {
                            requestContext.setExecutionStatus(handler, true);
                        }
                    } catch (RepositoryException e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        throw e;
                    } catch (VirtualMachineError e) {
                        log.fatal(UNABLE_TO_PROCEED_WITH_SIMULATION, e);
                        throw e;
                    } catch (Throwable e) {
                        requestContext.setExecutionStatus(handler, e);
                        
                        // Don't throw exceptions in simulation mode, but exit lifecycle phase
                        if (SimulationFilter.isSimulation()) {
                            return;
                        }
                        
                        // We will be concatenating the incoming exception's message so that it will
                        // be carried forward, and displayed at the client-side.
                        throw new RepositoryException(AN_EXCEPTION_OCCURRED_WHILE_EXECUTING_HANDLER_CHAIN + e.getMessage(), e);
                    }
                    
                    if (isProcessingComplete(requestContext)) {
                        break;
                    }
                }
            }

            if (isProcessingComplete(requestContext)) {
                break;
            }
        }
    }

    public void setEvaluateAllHandlers(boolean evaluateAllHandlers) {
        this.evaluateAllHandlers = evaluateAllHandlers;
    }
    
    public Map<Filter, Set<Handler>> getRegistryContextMap() {
    	return getRegistryContextHandlerMap ;
    }
}
