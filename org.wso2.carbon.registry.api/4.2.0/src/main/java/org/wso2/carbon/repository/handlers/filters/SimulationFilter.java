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
package org.wso2.carbon.repository.handlers.filters;

import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.RequestContext;

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

    /**
     * Handles the get operation
     */
    @Override
    public boolean handleGet(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the put operation
     */
    @Override
    public boolean handlePut(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the import resource operation
     */
    @Override
    public boolean handleImportResource(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the delete operation
     */
    @Override
    public boolean handleDelete(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the put child operation
     */
    @Override
    public boolean handlePutChild(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the import child operation
     */
    @Override
    public boolean handleImportChild(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the move operation
     */
    @Override
    public boolean handleMove(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the copy operation
     */
    @Override
    public boolean handleCopy(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the rename operation
     */
    @Override
    public boolean handleRename(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the create link operation
     */
    @Override
    public boolean handleCreateLink(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the remove link operation
     */
    @Override
    public boolean handleRemoveLink(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the restore version operation
     */
    @Override
    public boolean handleRestoreVersion(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the create version operation
     */
    @Override
    public boolean handleCreateVersion(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get versions operation
     */
    @Override
    public boolean handleGetVersions(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the execute query operation
     */
    @Override
    public boolean handleExecuteQuery(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }
    
    /**
     * Handles the search operation
     */
    @Override
    public boolean handleSearchContent(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the resource exists operation
     */
    @Override
    public boolean handleResourceExists(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the dump operation
     */
    @Override
    public boolean handleDump(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the resource operation
     */
    @Override
    public boolean handleRestore(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }
    
    // ------------- Following will eventually move out of the kernel ----------------------------------------------------
    
    /**
     * Handles the edit comments operation
     */
    @Override
    public boolean handleEditComment(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the remove comments operation
     */
    @Override
    public boolean handleRemoveComment(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the add comments operation
     */
    @Override
    public boolean handleAddComment(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get comments operation
     */
    @Override
    public boolean handleGetComments(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get resource path with tags operation
     */
    @Override
    public boolean handleGetResourcePathsWithTag(RequestContext requestContext)
            throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get tags operation
     */
    @Override
    public boolean handleGetTags(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get average rating operation
     */
    @Override
    public boolean handleGetAverageRating(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get rating operation
     */
    @Override
    public boolean handleGetRating(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }
    
    /**
     * Handles the add association operation
     */
    @Override
    public boolean handleAddAssociation(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the remove association operation
     */
    @Override
    public boolean handleRemoveAssociation(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get all association operation
     */
    @Override
    public boolean handleGetAllAssociations(RequestContext requestContext)
            throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the get association operation
     */
    @Override
    public boolean handleGetAssociations(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the apply tag operation
     */
    @Override
    public boolean handleApplyTag(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the remove tag operation
     */
    @Override
    public boolean handleRemoveTag(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }

    /**
     * Handles the rate resource operation
     */
    @Override
    public boolean handleRateResource(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }
    
    /**
     * Handles the invoke aspect operation
     */
    @Override
    public boolean handleInvokeAspect(RequestContext requestContext) throws RepositoryException {
        return isSimulation();
    }
}
