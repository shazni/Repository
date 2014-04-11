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
import org.wso2.carbon.repository.api.utils.Method;
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
    private Map<Method, Set<Handler>> handlerMap = new LinkedHashMap<Method, Set<Handler>>();

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
    public synchronized void addHandler(Method[] methods, Filter filter, Handler handler) {
        Set<Filter> filterSet = new LinkedHashSet<Filter>();
        filterSet.add(filter);
        handler.setFilters(filterSet);
        StringBuilder sb = new StringBuilder();
        if (methods != null) {
            for (Method method : methods) {
                Set<Handler> handlers = handlerMap.get(method);
                if (handlers == null) {
                    handlers = new LinkedHashSet<Handler>();
                }
                handlers.add(handler);
                handlerMap.put(method , handlers);
                if (log.isDebugEnabled()) {
                    sb.append(" ").append(method);
                }
            }
        } else {
            for (Method method : Method.values()) {
                Set<Handler> handlers = handlerMap.get(method);
                if (handlers == null) {
                    handlers = new LinkedHashSet<Handler>();
                }
                handlers.add(handler);
                handlerMap.put(method , handlers);
            }
            if (log.isDebugEnabled()) {
                sb.append(" all");
            }
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Registered the handler " + filter.getClass().getName() + " --> " + handler.getClass().getName() + " for" + sb.toString() + " methods.");
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
    public void addHandler(Method[] methods, Filter filter, Handler handler, String lifecyclePhase) {
        // We don't handle lifecycle phases in this Handler Manager. The Handler Lifecycle Manager
        // Does the required handling.
        addHandler(methods, filter, handler);
    }

    /**
     * remove a handler from all the filters, all the methods
     *
     * @param handler the handler to remove
     */
    public synchronized void removeHandler(Handler handler) {
        for (Method method : Method.values()) {
            Set<Handler> handlers = handlerMap.get(method);
            if (handlers != null) {
                handlers.remove(handler);
                handlerMap.put(method, handlers);
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
        Set<Handler> handlers =  handlerMap.get(Method.CREATE_VERSION);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.CREATE_VERSION)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.RESTORE_VERSION);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.RESTORE_VERSION)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.GET_VERSIONS);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.GET_VERSIONS)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.EXECUTE_QUERY);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.EXECUTE_QUERY)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.SEARCH_CONTENT);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.SEARCH_CONTENT)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.GET);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.GET)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.PUT);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.PUT)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.IMPORT);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.IMPORT)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.DELETE);

        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.DELETE)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.PUT_CHILD);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.PUT_CHILD)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.IMPORT_CHILD);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.IMPORT_CHILD)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.COPY);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.COPY)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.MOVE);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.MOVE)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.RENAME);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.RENAME)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.CREATE_LINK);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.CREATE_LINK)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.REMOVE_LINK);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.REMOVE_LINK)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.RESOURCE_EXISTS);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.RESOURCE_EXISTS)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.DUMP);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.DUMP)) {
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
        Set<Handler> handlers =  handlerMap.get(Method.RESTORE);
        if (handlers != null) {
            for (Handler handler : handlers) {
                if (handler.engageHandler(requestContext, Method.RESTORE)) {
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
        }
    }

    public void setEvaluateAllHandlers(boolean evaluateAllHandlers) {
        this.evaluateAllHandlers = evaluateAllHandlers;
    }
    
    public Set<Handler> getRegistryContextHandlerSet() {
    	return handlerMap.get(Method.GET_REGISTRY_CONTEXT);
    }
}
