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

import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Filter;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.api.utils.METHODS;

/**
 * This is a built-in Filter that is used for simulation operations. The handler simulator uses an
 * instance of this filter. When in simulation mode, this filter will run the associated simulation
 * handler, and when not, the handler methods won't get called. The simulation mode can be set by
 * accessing the instance of this Filter.
 */
public class SimulationFilter extends Filter {

    /**
     * This field stores whether the filter is in simulation mode or not.
     */
    private static ThreadLocal<Boolean> simulation =
            new ThreadLocal<Boolean>() {
                protected Boolean initialValue() {
                    return false;
                }
            };

    /**
     * Method to obtain whether in simulation mode or not.
     *
     * @return whether in simulation mode or not.
     */
    public static Boolean isSimulation() {
        return simulation.get();
    }

    /**
     * Method to set whether in simulation mode or not.
     *
     * @param input whether in simulation mode or not.
     */
    public static void setSimulation(Boolean input) {
        simulation.set(input);
    }

//    /**
//     * Handles the get operation
//     */
//    @Override
//    public boolean handleGet(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the put operation
//     */
//    @Override
//    public boolean handlePut(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the import resource operation
//     */
//    @Override
//    public boolean handleImportResource(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the delete operation
//     */
//    @Override
//    public boolean handleDelete(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the put child operation
//     */
//    @Override
//    public boolean handlePutChild(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the import child operation
//     */
//    @Override
//    public boolean handleImportChild(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the move operation
//     */
//    @Override
//    public boolean handleMove(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the copy operation
//     */
//    @Override
//    public boolean handleCopy(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the rename operation
//     */
//    @Override
//    public boolean handleRename(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the create link operation
//     */
//    @Override
//    public boolean handleCreateLink(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the remove link operation
//     */
//    @Override
//    public boolean handleRemoveLink(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the restore version operation
//     */
//    @Override
//    public boolean handleRestoreVersion(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the create version operation
//     */
//    @Override
//    public boolean handleCreateVersion(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the get versions operation
//     */
//    @Override
//    public boolean handleGetVersions(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the execute query operation
//     */
//    @Override
//    public boolean handleExecuteQuery(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the search operation
//     */
//    @Override
//    public boolean handleSearchContent(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the resource exists operation
//     */
//    @Override
//    public boolean handleResourceExists(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the dump operation
//     */
//    @Override
//    public boolean handleDump(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }
//
//    /**
//     * Handles the resource operation
//     */
//    @Override
//    public boolean handleRestore(HandlerContext requestContext) throws RepositoryException {
//        return isSimulation();
//    }

    @Override
    public boolean filter(HandlerContext handlerContext, METHODS method) throws RepositoryException {
        return isSimulation();
    }
}
