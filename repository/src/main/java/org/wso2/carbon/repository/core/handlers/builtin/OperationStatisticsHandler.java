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

package org.wso2.carbon.repository.core.handlers.builtin;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.api.utils.Method;
import org.wso2.carbon.repository.core.statistics.StatisticsLog;

/**
 * This handler is used to record operation-level statistics related to a given server instance.
 */
public class OperationStatisticsHandler extends Handler {

    private static Log log = LogFactory.getLog(OperationStatisticsHandler.class);

    // The instance of the logger to be used to log statistics.
    private static Log statsLog = StatisticsLog.getLog();

    // Map of statistic records.
    private static Map<Method, Long> records = null;

    // The executor service used to create threads to record operation statistics.
    private static ExecutorService executor = null;

    static {
        if (statsLog.isDebugEnabled()) {
            initializeStatisticsLogging();
        }
    }

    private static synchronized void initializeStatisticsLogging() {
        if (executor != null) {
            return;
        }
        records = new HashMap<Method, Long>();
        executor = Executors.newCachedThreadPool();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                executor.shutdownNow();
            }
        });
        
        for (Method method : new Method[]{Method.GET, Method.PUT, Method.IMPORT, Method.MOVE,
                Method.COPY, Method.RENAME, Method.DELETE, Method.ADD_ASSOCIATION,
                Method.REMOVE_ASSOCIATION, Method.GET_ASSOCIATIONS, Method.GET_ALL_ASSOCIATIONS,
                Method.EXECUTE_QUERY, Method.RESOURCE_EXISTS, Method.DUMP, Method.RESTORE}) {
            records.put(method, 0l);
        }
        
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                scheduler.shutdownNow();
            }
        });
        
        Runnable runnable = new Runnable() {
            public void run() {
                if (records == null) {
                    log.error("Unable to store operation statistics.");
                } else {
                    synchronized (this) {
                        statsLog.debug("Total Number of get calls                : " + records.get(Method.GET));
                        statsLog.debug("Total Number of put calls                : " + records.get(Method.PUT));
                        statsLog.debug("Total Number of import calls             : " + records.get(Method.IMPORT));
                        statsLog.debug("Total Number of move calls               : " + records.get(Method.MOVE));
                        statsLog.debug("Total Number of copy calls               : " + records.get(Method.COPY));
                        statsLog.debug("Total Number of rename calls             : " + records.get(Method.RENAME));
                        statsLog.debug("Total Number of delete calls             : " + records.get(Method.DELETE));
                        statsLog.debug("Total Number of addAssociation calls     : " + records.get(Method.ADD_ASSOCIATION));
                        statsLog.debug("Total Number of removeAssociation calls  : " + records.get(Method.REMOVE_ASSOCIATION));
                        statsLog.debug("Total Number of getAssociations calls    : " + records.get(Method.GET_ASSOCIATIONS));
                        statsLog.debug("Total Number of getAllAssociations calls : " + records.get(Method.GET_ALL_ASSOCIATIONS));
                        statsLog.debug("Total Number of executeQuery calls       : " + records.get(Method.EXECUTE_QUERY));
                        statsLog.debug("Total Number of resourceExists calls     : " + records.get(Method.RESOURCE_EXISTS));
                        statsLog.debug("Total Number of dump calls               : " + records.get(Method.DUMP));
                        statsLog.debug("Total Number of restore calls            : " + records.get(Method.RESTORE));
                    }
                }
            }
        };
        
        scheduler.scheduleAtFixedRate(runnable, 60, 60, TimeUnit.SECONDS);
    }

    private void incrementRecord(final Method operation) {
        Runnable runnable = new Runnable() {
            public void run() {
                if (records == null) {
                    log.error("Unable to store operation statistics.");
                } else {
                    synchronized (this) {
                        records.put(operation, records.get(operation) + 1);
                    }
                }
            }
        };
        
        if (executor != null) {
            executor.execute(runnable);
        } else {
            initializeStatisticsLogging();
            executor.execute(runnable);
        }
    }

    public Resource get(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.GET);
        }
        
        return super.get(requestContext);
    }

    public void put(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.PUT);
        }
        
        super.put(requestContext);
    }

    public void importResource(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.IMPORT);
        }
        
        super.importResource(requestContext);
    }

    public String move(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.MOVE);
        }
        
        return super.move(requestContext);
    }

    public String copy(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.COPY);
        }
        
        return super.copy(requestContext);
    }

    public String rename(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.RENAME);
        }
        
        return super.rename(requestContext);
    }

    public void delete(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.DELETE);
        }
        
        super.delete(requestContext);
    }

    public Collection executeQuery(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.EXECUTE_QUERY);
        }
        
        return super.executeQuery(requestContext);
    }

    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.RESOURCE_EXISTS);
        }
        
        return super.resourceExists(requestContext);
    }

    public void dump(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.DUMP);
        }
        
        super.dump(requestContext);
    }

    public void restore(HandlerContext requestContext) throws RepositoryException {
        if (statsLog.isDebugEnabled()) {
            incrementRecord(Method.RESTORE);
        }
        
        super.restore(requestContext);
    }
}
