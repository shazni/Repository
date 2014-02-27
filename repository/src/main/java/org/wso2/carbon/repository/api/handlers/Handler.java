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

package org.wso2.carbon.repository.api.handlers;

import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.handlers.Handler;

/**
 * Base class of all handler implementations. Provides the methods that handlers should implement.
 * This class also provides the data source and repository instances to be
 * used by handler implementations.
 * <p/>
 * Handlers can be chained by providing necessary filter combinations. But in such cases, handler
 * authors should make sure that handlers in the chain do not perform conflicting operations. Unless
 * there is a critical requirement and handler authors are confident that handlers do not have
 * negative impact on each other, it is recommended that handlers are configured to execute only one
 * handler per request.
 * <p/>
 * Handler instances may be accessed concurrently by multiple threads. Therefore, handlers should be
 * thread safe. It is recommended that handlers are made state-less, instead of synchronizing them as
 * it could become a performance bottleneck in highly concurrent environments.
 * <p/>
 * Implementations of handlers should be optimized to take the minimum time for processing. As the
 * handlers are executed they are always executed before executing the generic database layer code, time
 * consuming operations in handlers could slow down the whole repository.
 */
public abstract class Handler {

    /**
     * Processes the GET action for resource path of the requestContext.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.resourcePath: Path of the resource
     *                       <p/>
     *                       requestContext.resource: Resource at the given path. This can be null
     *                       if no other handler has retrieved that resource so far. If it contains
     *                       a value, matching handlers are free to do any change to the resource,
     *                       even they can replace the resource with completely new instance.
     *
     * @return 		Resource instance if the handler processed the GET action successfully.
     * @throws 		RepositoryException If the handler is supposed to handle the get on the repository and if the
     *          	get fails due a handler specific error
     */
    public Resource get(HandlerContext requestContext) throws RepositoryException {
        return null;
    }

    /**
     * Processes the PUT action. Actual path to which the resource is put may differ from the path
     * given in the requestContext.resourcePath. Therefore, after putting the resource, the actual
     * path to which the resource is put is set in the requestContext.actualPath.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.resourcePath: Path to put the resource.
     *                       requestContext.resource: Resource to put
     *
     * @throws RepositoryException If the handler is supposed to handle the put on the repository and if the
     *          	put fails due a handler specific error
     */
    public void put(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Creates a resource in the given path by fetching the resource content from the given URL.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.resourcePath: Path to add the new resource.
     *                       <p/>
     *                       requestContext.sourceURL: URL to fetch the resource content
     *                       <p/>
     *                       requestContext.resource: Resource instance containing the meta data for
     *                       the resource to be imported. Once import is done, new resource is
     *                       created combining the meta data of this meta data object and the
     *                       imported content.
     *
     * @throws RepositoryException If the handler is supposed to handle the import on the repository and if the
     *          	import fails due a handler specific error
     */
    public void importResource(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Move a resource in the repository.  This is equivalent to 1) delete the resource, then 2) add
     * the resource to the new location.  The operation is atomic, so if it fails the old resource
     * will still be there.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.sourcePath: Source/Current Path
     *                       <p/>
     *                       requestContext.targetPath: Destination/New Path
     *
     * @return the actual path for the new resource if the handler processed the MOVE action
     *         successfully.
     * @throws RepositoryException if something goes wrong
     */
    public String move(HandlerContext requestContext) throws RepositoryException {
        return null;
    }

    /**
     * Copy a resource in the repository.  This is equivalent to adding the resource to the new
     * location. The operation is atomic, so if it fails the resource won't be added.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.sourcePath: Source/Current Path
     *                       <p/>
     *                       requestContext.targetPath: Destination/New Path
     *
     * @return the actual path for the new resource if the handler processed the COPY action
     *         successfully.
     * @throws RepositoryException if something goes wrong
     */
    public String copy(HandlerContext requestContext) throws RepositoryException {
        return null;
    }

    /**
     * Rename a resource in the repository.  This is equivalent to 1) delete the resource, then 2) add
     * the resource to the new location.  The operation is atomic, so if it fails the old resource
     * will still be there.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.sourcePath: Source/Current Path
     *                       <p/>
     *                       requestContext.targetPath: Destination/New Path
     *
     * @return the actual path for the new resource if the handler processed the RENAME action
     *         successfully.
     * @throws RepositoryException if something goes wrong
     */
    public String rename(HandlerContext requestContext) throws RepositoryException {
        return null;
    }

    /**
     * Create a symbolic link or mount a repository.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.targetPath: Destination/New Path
     *
     * @throws RepositoryException if something goes wrong
     */
    public void createLink(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Remove a symbolic link or un-mount a repository.
     *
     * @param requestContext Information about the current request.
     *
     * @throws RepositoryException if something goes wrong
     */
    public void removeLink(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Processes the DELETE action of the media type.
     *
     * @param requestContext Information about the current request.
     *                       <p/>
     *                       requestContext.resourcePath: path of the resource to be deleted.
     *
     * @throws RepositoryException If the handler is supposed to handle the delete on the repository and if the
     *          	delete fails due a handler specific error
     */
    public void delete(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Invokes when a child resource is added. Only the media type handlers of collection resources
     * may have a meaningful implementation of this method.
     *
     * @param requestContext requestContext.resourcePath: path of the parent collection
     *                       requestContext.resource: New child resource to be added
     *
     * @throws RepositoryException If the handler is supposed to handle the putChild on the repository and if the
     *          	putChild fails due a handler specific error
     */
    public void putChild(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Invokes when a child resource is imported. Only the media type handlers of collection
     * resources may have a meaningful implementation of this method.
     *
     * @param requestContext requestContext.resourcePath
     *
     * @throws RepositoryException If the handler is supposed to handle the importChild on the repository and if the
     *          	importChild fails due a handler specific error
     */
    public void importChild(HandlerContext requestContext) throws RepositoryException {
    }
    
    /**
     * Gets called when executing Queries.
     *
     * @param requestContext Information about the current request. requestContext.resourcePath:
     *                       Path of Resource requestContext.queryParameters: Map of query
     *                       parameters.
     *
     * @return A collection containing results as its child resources if the handler processed the
     *         EXECUTE_QUERY action successfully.
     * @throws RepositoryException If the handler is supposed to handle the executeQuery on the repository and if the
     *          	executeQuery fails due a handler specific error
     */
    public Collection executeQuery(HandlerContext requestContext) throws RepositoryException {
        return null;
    }

    /**
     * Gets called when searching for content.
     *
     * @param requestContext Information about the current request. requestContext.keywords: Search
     *                       keywords.
     *
     * @return The result set as a collection if the handler processed the SEARCH_CONTENT action
     *         successfully.
     * @throws RepositoryException If the handler is supposed to handle the searchContent on the repository and if the
     *          	searchContent fails due a handler specific error
     */
    public Collection searchContent(HandlerContext requestContext) throws RepositoryException {
        return null;
    }

    /**
     * Gets called when searching for existence of resource.
     *
     * @param requestContext Information about the current request.
     *
     * @return True if the resource exists and false if not  if the handler processed the
     *         RESOURCE_EXISTS action successfully.
     * @throws RepositoryException If the handler is supposed to handle the resourceExists on the repository and if the
     *          	resourceExists fails due a handler specific error
     */
    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
        return false;
    }

    /**
     * Gets called when dumping an path
     *
     * @param requestContext Information about the current request. requestContext.keywords: Search
     *                       keywords.
     *
     * @throws RepositoryException If the handler is supposed to handle the dump on the repository and if the
     *          	dump fails due a handler specific error
     */
    public void dump(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Gets called when restoring a path
     *
     * @param requestContext Information about the current request. requestContext.keywords: Search
     *                       keywords.
     *
     * @throws RepositoryException If the handler is supposed to handle the restore on the repository and if the
     *          	restore fails due a handler specific error
     */
    public void restore(HandlerContext requestContext) throws RepositoryException {
    }
    
    /**
     * Gets called when restoring a version.
     *
     * @param requestContext Information about the current request. requestContext.versionPath: Path
     *                       of Resource with version This can be used to derive the path of the
     *                       resource as well.
     *
     * @throws RepositoryException If the handler is supposed to handle the restoreVersion on the repository and if the
     *          	restoreVersion fails due a handler specific error
     */
    public void restoreVersion(HandlerContext requestContext) throws RepositoryException {
    }

    /**
     * Gets called when creating a version.
     *
     * @param requestContext Information about the current request. requestContext.resourcePath:
     *                       Path of Resource
     *
     * @throws RepositoryException If the handler is supposed to handle the createVersion on the repository and if the
     *          	createVersion fails due a handler specific error
     */
    public void createVersion(HandlerContext requestContext) throws RepositoryException {
    }
    
    /**
     * Gets called when getting versions.
     *
     * @param requestContext Information about the current request. requestContext.resourcePath:
     *                       Path of Resource
     *
     * @return an array of Version paths are returned in the form /projects/resource?v=12 if the
     *         handler processed the GET_VERSIONS action successfully.
     * @throws RepositoryException If the handler is supposed to handle the getVersions on the repository and if the
     *          	getVersions fails due a handler specific error
     */
    public String[] getVersions(HandlerContext requestContext) throws RepositoryException {
        return null;
    }

    /**
     * This overrides the default hash code implementation for handler objects, to make sure that
     * each handler of the same type will have identical hash codes unless otherwise it has its own
     * extension.
     *
     * @return hash code for this handler type.
     */
    public int hashCode() {
        // As per contract for hashCode, If two objects are equal according to the equals(Object)
        // method, then calling the hashCode method on each of the two objects must produce the same
        // integer result. Therefore, two Handler objects having the same class name will have
        // identical hash codes.
        return getClass().getName().hashCode();
    }

    /**
     * Revised implementation of the equals comparison to suite the modified hashCode method.
     *
     * @param obj object to compare for equality.
     *
     * @return whether equal or not.
     */
    public boolean equals(Object obj) {
        return (obj != null && obj instanceof Handler && obj.getClass().getName().equals(getClass().getName()));
    }
}
