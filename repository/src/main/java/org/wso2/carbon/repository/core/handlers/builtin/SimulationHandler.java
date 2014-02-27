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

import java.util.List;
import java.util.Map;

import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;

/**
 * This handler is used to capture results after a handler simulation has taken place. The
 * simulation handler is a reporting handler that runs in the reporting handler lifecycle phase.
 * Handler simulation is useful to debug and also to identify the various operations that take place
 * during the execution of the various handlers for a particular operation.
 */
public class SimulationHandler extends Handler {

    private static ThreadLocal<Map<String, List<String[]>>> status =
            new ThreadLocal<Map<String, List<String[]>>>() {
                protected Map<String, List<String[]>> initialValue() {
                    return null;
                }
            };

    /**
     * Method to obtain the simulation status
     *
     * @return the simulation status as a map.
     */
    public static Map<String, List<String[]>> getStatus() {
        return status.get();
    }

    private static void setStatus(Map<String, List<String[]>> input) {
        status.set(input);
    }

    @Override
    public Resource get(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.get(requestContext);
    }

    @Override
    public void put(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.put(requestContext);
    }

    @Override
    public void importResource(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.importResource(requestContext);
    }

    @Override
    public String move(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.move(requestContext);
    }

    @Override
    public String copy(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.copy(requestContext);
    }

    @Override
    public String rename(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.rename(requestContext);
    }

    @Override
    public void createLink(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.createLink(requestContext);
    }

    @Override
    public void removeLink(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.removeLink(requestContext);
    }

    @Override
    public void delete(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.delete(requestContext);
    }

    @Override
    public void importChild(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.importChild(requestContext);
    }

    @Override
    public void restoreVersion(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.restoreVersion(requestContext);
    }

    @Override
    public void createVersion(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.createVersion(requestContext);
    }

    @Override
    public String[] getVersions(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.getVersions(requestContext);
    }

    @Override
    public Collection executeQuery(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.executeQuery(requestContext);
    }

    @Override
    public Collection searchContent(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.searchContent(requestContext);
    }

    @Override
    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        return super.resourceExists(requestContext);
    }

    @Override
    public void dump(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.dump(requestContext);
    }

    @Override
    public void restore(HandlerContext requestContext) throws RepositoryException {
        requestContext.setProcessingComplete(true);
        setStatus(requestContext.getHandlerExecutionStatusMap());
        super.restore(requestContext);
    }
}
