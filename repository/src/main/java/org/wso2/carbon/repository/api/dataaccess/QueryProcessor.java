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

package org.wso2.carbon.repository.api.dataaccess;

import java.util.Map;

import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;

/**
 * All query processors should extend this class. All configured query processor implementations
 * will be associated with a query type. Once an execute operation is called for a query of that
 * type, the associated query processor is invoked. Then all the tasks required for processing that
 * query should be taken over by the query processor implementation. This processing step include
 * interpreting of given query parameters, constructing the resulting resource object, etc.
 */
public interface QueryProcessor {

    /**
     * This method will be called when a query of a registered type is executed.
     *
     * @param repository   	the repository to execute the query
     * @param query      	query resource, possibly containing the query string
     * @param parameters 	query parameters as name->value pairs
     *
     * @return 		QueryProcessor implementation should map the query results to a resource and return the
     *         		resource. If the query may return multiple results it can return a collection
     *         		resource, containing the paths of individual results. Then a URL handler can be
     *         		registered to process those individual paths and return actual resulting resource.
     * @throws 		RepositoryException
     *          	QueryProcessor implementation should handle all exceptions and throw RegistryException if the
     *          	exception has to propagated to the client.
     */
    Collection executeQuery(Repository repository, Resource query, Map<?, ?> parameters) throws RepositoryException;
}
