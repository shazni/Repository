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

package org.wso2.carbon.repository.core.jdbc.dataaccess;

import org.wso2.carbon.repository.core.jdbc.dao.JDBCLogsDAO;
import org.wso2.carbon.repository.core.jdbc.dao.JDBCResourceDAO;
import org.wso2.carbon.repository.core.jdbc.dao.JDBCResourceVersionDAO;
import org.wso2.carbon.repository.spi.dao.LogsDAO;
import org.wso2.carbon.repository.spi.dao.ResourceDAO;
import org.wso2.carbon.repository.spi.dao.ResourceVersionDAO;
import org.wso2.carbon.repository.spi.dataaccess.DAOManager;

/**
 * An implementation of {@link DAOManager} to obtain access to the object representations of various
 * data stored on a back-end JDBC-based database.
 */
public class JDBCDAOManager implements DAOManager {

    private LogsDAO logsDAO;
    private ResourceDAO resourceDAO;
    private ResourceVersionDAO resourceVersionDAO;

    public JDBCDAOManager() {
        this.resourceDAO = new JDBCResourceDAO();
        this.logsDAO = new JDBCLogsDAO();
        this.resourceVersionDAO = new JDBCResourceVersionDAO(this);
    }

    public LogsDAO getLogsDAO() {
        return logsDAO;
    }

    public ResourceDAO getResourceDAO() {
        return resourceDAO;
    }

    public ResourceVersionDAO getResourceVersionDAO() {
        return resourceVersionDAO;
    }
}
