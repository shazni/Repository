/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.repository.core.utils;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.utils.Actions;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.spi.dao.LogsDAO;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;

/**
 * Writes log records to the database on a separate thread
 */
public class LogWriter extends Thread {

    private static final Log log = LogFactory.getLog(LogWriter.class);
    private static final int DEFAULT_SLEEP_TIME = 10 * 1000;

    private LogQueue logQueue = null;
    private DataAccessManager dataAccessManager = null;
    private boolean canWriteLogs = true;

    public boolean isCanWriteLogs() {
        return canWriteLogs;
    }
    
    public void setCanWriteLogs(boolean canWriteLogs) {
        this.canWriteLogs = canWriteLogs;
    }
    /**
     * Constructor accepting a queue of logs.
     *
     * @param logQueue          the queue of logs.
     * @param dataAccessManager the manager class that can be used to obtain access to the back-end
     *                          database.
     */
    public LogWriter(LogQueue logQueue, DataAccessManager dataAccessManager) {
        this.logQueue = logQueue;
        this.dataAccessManager = dataAccessManager;
        this.setName("RegistryLogWritter");
    }

    /**
     * Sets the queue for holding and retrieving logs.
     *
     * @param logQueue the queue for holding and retrieving logs.
     */
    public void setLogQueue(LogQueue logQueue) {
        this.logQueue = logQueue;
    }

    /**
     * Obtains the queue for holding and retrieving logs.
     *
     * @return queue for holding and retrieving logs.
     */
    public LogQueue getLogQueue() {
        return logQueue;
    }

    /**
     * Starts writing logs.
     */
    public synchronized void start() {
        super.start();
    }

    /**
     * The main business logic.
     */
    public void run() {
    	logWrite();
    }

    public void logWrite() {
        while (true) {
            if (logQueue != null && !logQueue.isEmpty() && dataAccessManager != null) {
                int queueLength = logQueue.size();
                LogRecord[] logRecords = new LogRecord[queueLength];
                
                for (int a = 0; a < queueLength; a++) {
                    LogRecord logRecord = (LogRecord) logQueue.poll();
                    logRecords[a] = logRecord;
                }
                
                LogsDAO logsDAO = dataAccessManager.getDAOManager().getLogsDAO();
                
                try {
                    logsDAO.saveLogBatch(logRecords);
                } catch (RepositoryException e) {
                    log.error("Unable to save log records", e);
                }
            } else if (isInterrupted()) {
                break;
            }

            try {
                if (!isInterrupted()) {
                    sleep(DEFAULT_SLEEP_TIME);
                }
            } catch (InterruptedException e) {
                if (logQueue == null || logQueue.isEmpty()) {
                    break;
                }
            }
        }
    }


    /**
     * Adds log record to queue
     *
     * @param resourcePath the resource path.
     * @param userName     the name of the user who performed the action on the resource.
     * @param action       an identifier of the action that happened.
     * @param actionData   the data for further information.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void addLog(String resourcePath, String userName, Actions action, String actionData) throws RepositoryException {
        if (logQueue != null && isCanWriteLogs()) {
            LogRecord logRecord = new LogRecord();
            
            if (CurrentContext.getLocalPathMap() != null) {
                String temp = CurrentContext.getLocalPathMap().get(resourcePath);
                if (temp != null) {
                    resourcePath = temp;
                }
            }
            
            logRecord.setResourcePath(resourcePath);
            logRecord.setUserName(userName);
            logRecord.setTimestamp(new Date(System.currentTimeMillis()));
            logRecord.setAction(action);
            logRecord.setActionData(actionData);
            logRecord.setTenantId(CurrentContext.getTenantId());

            logQueue.add(logRecord);
        }
	}
}
