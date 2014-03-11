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

package org.wso2.carbon.repository.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.*;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.exceptions.RepositoryErrorCodes;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.exceptions.RepositoryResourceNotFoundException;
import org.wso2.carbon.repository.api.exceptions.RepositoryUserContentException;
import org.wso2.carbon.repository.api.utils.RepositoryUtils;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.config.StaticConfiguration;
import org.wso2.carbon.repository.core.dataobjects.ResourceDO;
import org.wso2.carbon.repository.core.exceptions.RepositoryServerContentException;
import org.wso2.carbon.repository.core.utils.*;
import org.wso2.carbon.repository.spi.dao.ResourceDAO;
import org.wso2.carbon.repository.spi.dao.ResourceVersionDAO;
import org.wso2.carbon.repository.spi.dataaccess.DataAccessManager;

import javax.xml.bind.DatatypeConverter;
import javax.xml.stream.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Encapsulates the retrieving, storing, modifying and deleting of resources. This class only deals
 * with the current versions of resources and it is unaware of any versioning or snapshot activity.
 * Only the current version related tables are accessed and updated from the methods of this class.
 */
public class ResourceStorer {

    private static final Log log = LogFactory.getLog(ResourceStorer.class);

    private ResourceDAO resourceDAO;
    private ResourceVersionDAO resourceVersionDAO;

    private static final String ILLEGAL_CHARACTERS_FOR_PATH = ".*[~!@#;%^*+={}\\|\\\\<>\",\'].*";
    private Pattern illegalCharactersPattern;

    /**
     * Package-private constant for session key used to inform the repository whether an activity
     * must be logged or not. This constant is intended to be used only by the Repository and the
     * Embedded Registry.
     */
    static final String IS_LOGGING_ACTIVITY = "isLoggingActivity";

    /**
     * Determines whether to version resources automatically when a resource is modified.
     * Modifications that can be versioned, are adding new resources, changing content and changing
     * properties.
     */
    private boolean versionOnChange = false;

    private VersionResourceStorer versionRepository;

    private RecursionRepository recursionRepository;

    /**
     * Data source is set on all resources get from the repository.
     */
    private DataAccessManager dataAccessManager;

    /**
     * Constructs a Repository
     *
     * @param dataAccessManager   the data access manager that is used for database communication.
     * @param versionRepository   the version repository.
     * @param versionOnChange     whether versioning needs to be done on change.
     * @param recursionRepository the recursion repository for recursive operations.
     */
    public ResourceStorer(DataAccessManager dataAccessManager,
                      VersionResourceStorer versionRepository, boolean versionOnChange,
                      RecursionRepository recursionRepository) {
        this.dataAccessManager = dataAccessManager;
        this.versionRepository = versionRepository;
        this.versionOnChange = versionOnChange;
        this.recursionRepository = recursionRepository;
        recursionRepository.setRepository(this);
        this.resourceDAO = dataAccessManager.getDAOManager().getResourceDAO();
        this.resourceVersionDAO = dataAccessManager.getDAOManager().getResourceVersionDAO();
        this.illegalCharactersPattern = Pattern.compile(ILLEGAL_CHARACTERS_FOR_PATH);
    }

    /**
     * Checks if a pure resource exists in the given path.
     *
     * @param path Path of a possible pure resource.
     *
     * @return true if a resource exists in the given path. false otherwise.
     * @throws RepositoryException if the operation failed.
     */
    public boolean resourceExists(String path) throws RepositoryException {
        String purePath = InternalUtils.getPureResourcePath(path);
        return resourceDAO.resourceExists(purePath);
    }

    /**
     * Gets the meta data of resource referred by the given path.
     *
     * @param path Path of a "pure" resource. Path should map to an actual resource stored in the
     *             database. Paths referring to virtual resource are not handled (e.g.
     *             /c1/r1;comments).
     *
     * @return Resource referred by the given path. Resource can be a file or a collection.
     * @throws RepositoryException if the operation failed.
     */
    public Resource getMetaData(String path) throws RepositoryException {
        String purePath = InternalUtils.getPureResourcePath(path);
        Resource resource = resourceDAO.getResourceMetaData(purePath);
        
        if (resource == null) {
            return null;
        }
        
        ((ResourceImpl) resource).setDataAccessManager(dataAccessManager);
        ((ResourceImpl) resource).setTenantId(CurrentContext.getTenantId());

        return resource;
    }

    /**
     * Gets the pure resource referred by the given path.
     *
     * @param path Path of a "pure" resource. Path should map to an actual resource stored in the
     *             database. Paths referring to virtual resource are not handled (e.g.
     *             /c1/r1;comments).
     *
     * @return Resource referred by the given path. Resource can be a file or a collection.
     * @throws RepositoryException if the operation failed.
     */
    public Resource get(String path) throws RepositoryException {

        String purePath = InternalUtils.getPureResourcePath(path);

        Resource resource = resourceDAO.getResourceMetaData(purePath);
        if (resource == null) {
            return null;
        }
        
        resourceDAO.fillResource((ResourceImpl) resource);
        ((ResourceImpl) resource).setDataAccessManager(dataAccessManager);
        ((ResourceImpl) resource).setTenantId(CurrentContext.getTenantId());

        return resource;
    }

    /**
     * Method to get a paged collection.
     *
     * @param path    the collection path.
     * @param start   the starting index.
     * @param pageLen the page length.
     *
     * @return collection with resources on the given page.
     * @throws RepositoryException if the operation failed.
     */
    public Collection get(String path, int start, int pageLen) throws RepositoryException {
        String purePath = InternalUtils.getPureResourcePath(path);

        CollectionImpl resource = (CollectionImpl) resourceDAO.getResourceMetaData(purePath);
        if (resource == null) {
            return null;
        }
        
        resourceDAO.fillResource(resource, start, pageLen);
        resource.setDataAccessManager(dataAccessManager);
        resource.setTenantId(CurrentContext.getTenantId());

        return resource;
    }

    /**
     * Adds or updates the resource in the given path with the given resource. Put is executed if
     * the current user has authorization to do so. Below is the method of evaluating
     * authorizations.
     * <p/>
     * <ul> <li> user should have WRITE permissions for the resource, if he wants to update the
     * resource.</li> <li>user should have WRITE permissions for the parent of the new resource, if
     * he wants to add a new resource.</li> </ul>
     *
     * @param path     Path of the resource to be added or updated. Path should only refer to
     *                 current version a pure resource. i.e. Path should not contain any version
     *                 information as it is impossible to add or update old versions.
     * @param resource Resource to be added or updated.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void put(String path, Resource resource) throws RepositoryException {
        if (illegalCharactersPattern.matcher(path).matches()) {
            throw new RepositoryUserContentException("The path '" + path + "' contains one or more illegal " +
                    "characters (~!@#;%^*()+={}|\\<>\"\',)", RepositoryErrorCodes.INVALID_PATH_PROVIDED);
        } else if (InternalConstants.CHECK_IN_META_DIR.equals(path)) {
            throw new RepositoryUserContentException(InternalConstants.CHECK_IN_META_DIR + " is an illegal " +
                    "name for a resource.", RepositoryErrorCodes.ILLEGAL_NAME_OF_RESOURCE);
        }
        
        // validating the resource property names for NULL. This is important when adding properties via the API.
        validateProperties(path, resource);

        String purePath = InternalUtils.getPureResourcePath(path);

        ResourceIDImpl resourceID = resourceDAO.getResourceID(purePath, resource instanceof CollectionImpl);
        
        boolean resourceExists = false;
        
        if (resourceID != null) {
            // existence of the resource id doesn't mean the existence of the
            // resource, so need to verify resource existence

            // load the meta data for the existing resource
            ResourceDO oldResourceDO = resourceDAO.getResourceDO(resourceID);

            if (oldResourceDO != null) {
                resourceExists = true;
                prepareUpdate(resource, resourceID, oldResourceDO);
                update(resourceID, (ResourceImpl) resource, oldResourceDO);
            }
        }
        
        if (!resourceExists) {
            ResourceIDImpl inverseResourceID = resourceDAO.getResourceID(purePath,
                    !(resource instanceof CollectionImpl));
            if (inverseResourceID != null) {
                ResourceDO inverseResourceDO = resourceDAO.getResourceDO(inverseResourceID);
                if (inverseResourceDO != null) {
                    deleteSubTree(inverseResourceID, inverseResourceDO, false);
                }
            }

            add(purePath, (ResourceImpl) resource);
        }
    }

    /**
     * This method will validate the resource properties to make sure the values are legit.
     *
     * @param path  path of the resource
     * @param resource  resource object
     * @throws org.wso2.carbon.repository.api.exceptions.RepositoryException If operation failed
     */
    private void validateProperties(String path, Resource resource) throws RepositoryException {

        for (String key : resource.getPropertyKeys()) {
            if (rejectIfNull(key)) {
                String errMsg = "The resource at " + path +
                        " contains a property that has a key with NULL.";
                log.warn(errMsg);
                throw new RepositoryUserContentException(errMsg, RepositoryErrorCodes.INVALID_PROPERTY_WITH_NULL_KEY);
            }
        }

    }

    private boolean rejectIfNull(Object value) {
        return value == null;
    }

    // Method to prepare a resource for an update.
    private void prepareUpdate(Resource resource, ResourceIDImpl resourceID, ResourceDO oldResourceDO) throws RepositoryException {
        // copying the id attribute for the resource
        ((ResourceImpl) resource).setPathID(resourceID.getPathID());
        ((ResourceImpl) resource).setName(resourceID.getName());
        resource.setPath(resourceID.getPath());

        ((ResourceImpl) resource).setCreatedTime(new Date(oldResourceDO.getCreatedOn()));
        ((ResourceImpl) resource).setAuthorUserName(oldResourceDO.getAuthor());

        // we are always creating versions for resources (files), if the resource has changed.
        if (!(resource instanceof Collection) && this.versionOnChange && ((ResourceImpl) resource).isVersionableChange()) {
            ResourceImpl oldResourceImpl = new ResourceImpl(resourceID.getPath(), oldResourceDO);
            versionRepository.createSnapshot(oldResourceImpl, false, false);
        } else {
            ResourceImpl oldResourceImpl;
            if (resourceID.isCollection()) {
                oldResourceImpl = new CollectionImpl(resourceID.getPath(), oldResourceDO);
            } else {
                oldResourceImpl = new ResourceImpl(resourceID.getPath(), oldResourceDO);
            }
            // just delete the resource and content
            // delete the old entry from the resource table
            removeResource(oldResourceImpl, false);
        }
    }

    /**
     * Creates a resource with the content imported from the source URL and meta data extracted from
     * the given meta data resource instance. Then the created resource is put to the registry using
     * the Repository.put() method.
     *
     * @param path         Path to put the resource
     * @param sourceURL    URL to import resource content
     * @param metaResource Meta data for the new resource is extracted from this meta data resource
     *
     * @return Actual path where the new resource is stored in the registry
     * @throws RepositoryException for all exceptional scenarios
     */
    public String importResource(String path, String sourceURL, Resource metaResource) throws RepositoryException {
        String purePath = InternalUtils.getPureResourcePath(path);

        URL url;
        try {
            if (sourceURL == null ||  sourceURL.toLowerCase().startsWith("file:")) {
                String msg = "The source URL must not be file in the server's local file system";
                throw new RepositoryException(msg);
            }
            url = new URL(sourceURL);
        } catch (MalformedURLException e) {
            String msg = "Given source URL is not valid.";
            throw new RepositoryException(msg, e);
        }

        try {
            URLConnection uc = url.openConnection();
            InputStream in = uc.getInputStream();
            String mediaType = metaResource.getMediaType();
            if (mediaType == null) {
                mediaType = uc.getContentType();
            }
            metaResource.setMediaType(mediaType);
            metaResource.setDescription(metaResource.getDescription());
            metaResource.setContentStream(in);
            put(purePath, metaResource);

        } catch (IOException e) {

            String msg = "Could not read from the given URL: " + sourceURL;
            throw new RepositoryException(msg, e);
        }

        return purePath;
    }

    /**
     * Deletes the pure resource referred by the path.
     *
     * @param _path path Path of the resource deleted. Path should only refer to current version a
     *             pure resource. i.e. Path should not contain any version information as it is
     *             impossible to delete old versions.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void delete(String _path) throws RepositoryException {

        String path = _path;
        path = InternalUtils.getPureResourcePath(path);

        ResourceIDImpl resourceID = resourceDAO.getResourceID(path);
        ResourceDO resourceDO = resourceDAO.getResourceDO(resourceID);
        
        if (resourceDO == null) {
            boolean isCollection = resourceID.isCollection();
            // then we will check for non-collections as the getResourceID only check the collection exist
            if (isCollection) {
                resourceID = resourceDAO.getResourceID(path, false);
                if (resourceID != null) {
                    resourceDO = resourceDAO.getResourceDO(resourceID);
                }
            }
            if (resourceDO == null) {
                String msg = "Failed to delete resource " + path + ". Resource does not exist.";
                log.error(msg);
                throw new RepositoryException(msg);
            }
        }
        
        deleteSubTree(resourceID, resourceDO, false);
        updateParent(resourceDAO.getResourceID(RepositoryUtils.getParentPath(path), true));
    }

    /**
     * This will delete the entire resource, except it keeps the authorizations.
     *
     * @param _path the path to be restored.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void prepareVersionRestore(String _path) throws RepositoryException {
        String path=_path;
        path = InternalUtils.getPureResourcePath(path);

        ResourceIDImpl resourceID = resourceDAO.getResourceID(path);
        ResourceDO resourceDO = resourceDAO.getResourceDO(resourceID);
        
        if (resourceDO == null) {
            boolean isCollection = resourceID.isCollection();
            // then we will check for non-collections as the getResourceID only check the collection exist
            
            if (isCollection) {
                resourceID = resourceDAO.getResourceID(path, false);
                if (resourceID != null) {
                    resourceDO = resourceDAO.getResourceDO(resourceID);
                }
            }
            
            if (resourceDO == null) {
                return;
            }
        }
        
        deleteSubTree(resourceID, resourceDO, true);
    }

    /**
     * This will delete only the current resource, keeps the authorizations.
     *
     * @param path the path to be prepared to restore.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void prepareDumpRestore(String path) throws RepositoryException {
        path = InternalUtils.getPureResourcePath(path);

        ResourceIDImpl resourceID = resourceDAO.getResourceID(path);
        ResourceDO resourceDO = resourceDAO.getResourceDO(resourceID);
        
        if (resourceDO == null) {
            boolean isCollection = resourceID.isCollection();
            // then we will check for non-collections as the getResourceID only check the collection exist
            
            if (isCollection) {
                resourceID = resourceDAO.getResourceID(path, false);
                if (resourceID != null) {
                    resourceDO = resourceDAO.getResourceDO(resourceID);
                }
            }
            
            if (resourceDO == null) {
                return;
            }
        }
        
        deleteNode(resourceID, resourceDO, true);
    }

    /**
     * Method to delete a sub tree of the collection hierarchy.
     *
     * @param resourceID            the resource identifier.
     * @param resourceDO            the resource data object.
     * @param keepAuthorization whether to keep authorizations.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void deleteSubTree(ResourceIDImpl resourceID, ResourceDO resourceDO, boolean keepAuthorization) throws RepositoryException {
        if (resourceID.isCollection()) {
            // recursively call for all the resources in the tree..
            List<ResourceIDImpl> childIDs = resourceDAO.getChildPathIds(resourceID);
            for (ResourceIDImpl childID : childIDs) {
                ResourceDO childResourceDO = resourceDAO.getResourceDO(childID);
                if(childResourceDO != null){
                    recursionRepository.deleteSubTree(childID, childResourceDO, keepAuthorization);
                }
            }
        }
        
        deleteNode(resourceID, resourceDO, keepAuthorization);
    }

    /**
     * Method to delete just the node in the collection hierarchy.
     *
     * @param resourceID            the resource identifier.
     * @param resourceDO            the resource data object.
     * @param keepAuthorization whether to keep authorizations.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void deleteNode(ResourceIDImpl resourceID, ResourceDO resourceDO, boolean keepAuthorization) throws RepositoryException {
        ResourceImpl resourceImpl;
        
        if (resourceID.isCollection()) {
            resourceImpl = new CollectionImpl(resourceID.getPath(), resourceDO);
        } else {
            resourceImpl = new ResourceImpl(resourceID.getPath(), resourceDO);
        }

        // now do the versioning as delete is considered as a change, where
        // non-collections are versioned automatically unless it is configured otherwise
        if (!(resourceImpl instanceof CollectionImpl) && this.versionOnChange && resourceImpl.isVersionableChange()) {

            // we are creating snapshot without renewing old contents..
            versionRepository.createSnapshot(resourceImpl, false, true);
        } else {
            // just delete the resource and content
            // delete the old entry from the resource table
            boolean isResourcePathVersioned = resourceVersionDAO.isResourceHistoryExist(resourceImpl.getResourceIDImpl());
            removeResource(resourceImpl, isResourcePathVersioned);
        }
    }

    /**
     * Method to remove a resource.
     *
     * @param resourceImpl   the resource.
     * @param keepProperties whether to keep properties or not.
     *
     * @throws RepositoryException if the operation failed.
     */
    private void removeResource(ResourceImpl resourceImpl,
                                boolean keepProperties) throws RepositoryException {
        resourceDAO.deleteResource(resourceImpl.getResourceDO());
        if (!(resourceImpl instanceof CollectionImpl)) {
            int contentID = resourceImpl.getDbBasedContentID();
            if (contentID > 0) {
                // delete the old content stream from the latest table
                resourceDAO.deleteContentStream(contentID);
            }
        }
        if (!StaticConfiguration.isVersioningProperties() && !keepProperties) {
            resourceDAO.removeProperties(resourceImpl.getResourceDO());
        }
    }

    /**
     * Renames the resource at oldPath with the given newName. If oldPath is
     * "/projects/wso2/config.xml" and newName is "registry.xml" then the new path of the resource
     * will be "/projects/wso2/registry.xml". If the renamed resource is a collection, paths of all
     * its descendants will also be changed.
     *
     * @param oldResourcePath Path of the resource to be renamed.
     * @param newName         New name for the resource. Name should not contain "/".
     *
     * @return New path of the resource.
     * @throws RepositoryException if the operation failed.
     */
    public String rename(ResourcePath oldResourcePath, String newName) throws RepositoryException {

        if (!oldResourcePath.isCurrentVersion()) {
            String msg = "Failed to rename the resource " + oldResourcePath +
                    ". Given path refers to an archived version of the resource.";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        String oldPath = InternalUtils.getPureResourcePath(oldResourcePath.getPath());

        if (!newName.startsWith(RepositoryConstants.PATH_SEPARATOR)) {
            // relative URL
            String oldDir = oldPath.substring(0, oldPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) + 1);
            newName = oldDir + newName;
        }

        move(oldResourcePath, newName);

        return newName;
    }

    /**
     * Moves the resource at oldPath to the newPath. If the resource is a collection, all its
     * descendant resources will also be moved.
     *
     * @param oldResourcePath Path of a existing resource
     * @param newPath         New path of the resource
     *
     * @return the actual new path
     * @throws RepositoryException if the operation failed.
     */
    public String move(ResourcePath oldResourcePath, String newPath) throws RepositoryException {

        if (!oldResourcePath.isCurrentVersion()) {
            String msg = "Failed to copy the resource " + oldResourcePath +
                    ". Given path refers to an archived version of the resource.";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        // we are implementing move as copying the resource to the new path and deleting the old
        // resource. therefore, the moved resource will be treated as a new resource (i.e without
        // a version history. but the resource at old path (which is deleted) has its version
        // history. so it is possible to restore the parent collection of old path and get old
        // versions of that resource.

        String oldPath = oldResourcePath.getPath();
        if (newPath.equals(oldPath)) {
            return newPath;
        }

        // prepare the target parent path
        String targetParentPath = RepositoryUtils.getParentPath(newPath);
        
        // first create a parent path for the target if doesn't exist
        ResourceIDImpl targetParentResourceID = resourceDAO.getResourceID(targetParentPath, true);
        
        if (targetParentResourceID == null || !resourceDAO.resourceExists(targetParentResourceID)) {
            addEmptyCollection(targetParentPath);
            if (targetParentResourceID == null) {
                targetParentResourceID = resourceDAO.getResourceID(targetParentPath, true);
            }
        } 

        // get the source resource
        ResourceImpl sourceResource = (ResourceImpl) getMetaData(oldPath);
        
        if (sourceResource == null) {
            throw new RepositoryResourceNotFoundException(oldPath);
        }
        
        ResourceIDImpl sourceID = sourceResource.getResourceIDImpl();

        if (!(sourceResource instanceof CollectionImpl)) {
            prepareMove(oldPath, newPath);

            ResourceIDImpl targetID = resourceDAO.getResourceID(newPath, false);
            if (targetID == null) {
                targetID = resourceDAO.createResourceID(newPath, targetParentResourceID, false);
            }

            resourceDAO.moveResources(sourceID, targetID);
            resourceDAO.moveProperties(sourceID, targetID);

            return newPath;
        }

        moveRecursively(sourceID, newPath, targetParentResourceID);
        String sourceParentPath = RepositoryUtils.getParentPath(oldPath);
        if (sourceParentPath.equals(targetParentPath)) {
            updateParent(targetParentResourceID);
        } else {
            updateParent(targetParentResourceID);
            updateParent(resourceDAO.getResourceID(sourceParentPath, true));
        }
        return newPath;
    }

    /**
     * Method to do a recursive move.
     *
     * @param sourceID               the source resource's identifier.
     * @param targetPath             the target resource path.
     * @param targetParentResourceID the target resource's parent's identifier.
     *
     * @return the target path.
     * @throws RepositoryException if the operation failed.
     */
    public String moveRecursively(ResourceIDImpl sourceID, String targetPath, ResourceIDImpl targetParentResourceID) throws RepositoryException {
        prepareMove(sourceID.getPath(), targetPath);

        ResourceIDImpl targetID = resourceDAO.getResourceID(targetPath, sourceID.isCollection());
        if (targetID == null) {
            targetID = resourceDAO
                    .createResourceID(targetPath, targetParentResourceID, sourceID.isCollection());
        }

        List<ResourceIDImpl> sourceChildIDs = resourceDAO.getChildPathIds(sourceID);
        
        for (ResourceIDImpl sourceChildID : sourceChildIDs) {
            if (!sourceChildID.isCollection()) {
                String sourceChildPath = sourceChildID.getPath();
                String sourceChildResourceName = RepositoryUtils.getResourceName(sourceChildPath);
                String targetChildPath = targetPath + (targetPath.endsWith(RepositoryConstants.PATH_SEPARATOR) ? "" :
                                RepositoryConstants.PATH_SEPARATOR) + sourceChildResourceName;

                if (recursionRepository.moveRecursively(sourceChildID, targetChildPath, targetID) != null) {
                    continue;
                }
                // if it is a resource, we only need to prepare the move as all
                // the non-collection moves happens in below operation
                prepareMove(sourceChildPath, targetChildPath);
            }
        }

        resourceDAO.moveResourcePaths(sourceID, targetID);
        resourceDAO.movePropertyPaths(sourceID, targetID);

        // recursively call this for all the child collections

        for (ResourceIDImpl sourceChildID : sourceChildIDs) {
            if (sourceChildID.isCollection()) {
                String sourceChildPath = sourceChildID.getPath();
                String sourceChildResourceName = RepositoryUtils.getResourceName(sourceChildPath);
                String targetChildPath = targetPath + (targetPath.endsWith(RepositoryConstants.PATH_SEPARATOR) ? "" :
                                RepositoryConstants.PATH_SEPARATOR) + sourceChildResourceName;
                recursionRepository.moveRecursively(sourceChildID, targetChildPath, targetID);
            }
        }
        
        return targetPath;
    }

    // This will prepare the move by checking the authorizations etc..
    private void prepareMove(String sourcePath, String targetPath) throws RepositoryException {
        ResourceIDImpl targetExistingResourceID = resourceDAO.getResourceID(targetPath);
        ResourceDO targetExistingResourceDO;
        
        if (targetExistingResourceID != null) {
            targetExistingResourceDO = resourceDAO.getResourceDO(targetExistingResourceID);
            if (targetExistingResourceDO == null && targetExistingResourceID.isCollection()) {
                targetExistingResourceID = resourceDAO.getResourceID(targetPath, false);
                if (targetExistingResourceID != null) {
                    targetExistingResourceDO = resourceDAO.getResourceDO(targetExistingResourceID);
                }
            }

            if (targetExistingResourceDO != null) {
                removeIndividual(targetExistingResourceID, targetExistingResourceDO);
            }
        }
    }

    // Method to remove a resource, which is used when moving resources.
    private void removeIndividual(ResourceIDImpl resourceID, ResourceDO resourceDO) throws RepositoryException {
        ResourceImpl resource;
        
        if (resourceID.isCollection()) {
            resource = new CollectionImpl(resourceID.getPath(), resourceDO);
        } else {
            resource = new ResourceImpl(resourceID.getPath(), resourceDO);
        }
        
        if (!(resource instanceof Collection) && this.versionOnChange && resource.isVersionableChange()) {
            versionRepository.createSnapshot(resource, false, false);
        } else {
            // just delete the resource and content
            // delete the old entry from the resource table
            removeResource(resource, false);
        }
    }

    /**
     * Method to copy a resource from source to target.
     *
     * @param sourceResourcePath the source path.
     * @param targetResourcePath the target path.
     *
     * @return the target path.
     * @throws RepositoryException if the operation failed.
     */
    public String copy(ResourcePath sourceResourcePath, ResourcePath targetResourcePath) throws RepositoryException {
        if (!sourceResourcePath.isCurrentVersion() || !targetResourcePath.isCurrentVersion()) {
            String msg = "Failed to copy the resource " + sourceResourcePath + " to path " +
                    targetResourcePath + ". Both paths should refer to current versions.";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        String sourcePath = sourceResourcePath.getPath();
        String targetPath = targetResourcePath.getPath();

        sourcePath = InternalUtils.getPureResourcePath(sourcePath);
        targetPath = InternalUtils.getPureResourcePath(targetPath);

        if (sourcePath.equals(targetPath)) {
            return targetPath;
        } else if (sourcePath.indexOf(targetPath) == 0) {
            String msg = "Failed to copy the resource " + sourceResourcePath + " to path " +
                    targetResourcePath + ". The target path is a part of the source path.";
            log.error(msg);
            throw new RepositoryException(msg);
        }
        
        ResourceImpl sourceResource = (ResourceImpl) get(sourcePath);
        
        if (sourceResource instanceof CollectionImpl) {
            resourceDAO.fillChildren((CollectionImpl) sourceResource, 0, -1);
        }

        if (resourceDAO.resourceExists(targetPath)) {
            delete(targetPath);
        }

        ResourceImpl targetResource = sourceResource.getShallowCopy();

        setUUIDForResource(targetResource);

        put(targetPath, targetResource);

        if (sourceResource instanceof CollectionImpl) {
            CollectionImpl collection = (CollectionImpl) sourceResource;

            for (String childSourcePath : collection.getChildPaths()) {
                String childResourceName = RepositoryUtils.getResourceName(childSourcePath);
                String childTargetPath = targetPath + RepositoryConstants.PATH_SEPARATOR + childResourceName;

                ResourcePath childSourceResourcePath = new ResourcePath(childSourcePath);
                ResourcePath childTargetResourcePath = new ResourcePath(childTargetPath);

                recursionRepository.copy(childSourceResourcePath, childTargetResourcePath);
            }
        }

        return targetPath;
    }

    // Method to add a resource
    private void add(String path, ResourceImpl resource) throws RepositoryException {
        // first add all non-existing parent collections. note that whether the user has
        // permission to add this resource depends on the permissions of the nearest ascendant.

        String parentPath = RepositoryUtils.getParentPath(path);

        // first, let's check if there is a parent for this resource. we do this to speed up the
        // common use case where new resources are added to an existing parent.

        ResourceIDImpl parentResourceID = resourceDAO.getResourceID(parentPath, true);
        if (parentResourceID == null || !resourceDAO.resourceExists(parentResourceID)) {
            addEmptyCollection(parentPath);
            if (parentResourceID == null) {
                parentResourceID = resourceDAO.getResourceID(parentPath, true);
            }
        }

        // then add the new resource/collection
        RepositoryContext registryContext = null;
        if (CurrentContext.getRespository() != null) {
        	registryContext = InternalUtils.getRepositoryContext(CurrentContext.getRespository());
        }
        
        if (registryContext == null) {
            registryContext = RepositoryContext.getBaseInstance();
        }
        
        if (!Boolean.FALSE.equals(CurrentContext.getAttribute(IS_LOGGING_ACTIVITY))) {
            registryContext.getLogWriter().addLog(
                    path, CurrentContext.getUser(), Activity.ADD, null);
        }
        
        if (!(resource instanceof CollectionImpl)) {
            if (resource.getMediaType() == null || resource.getMediaType().length() == 0) {
                String temp = MediaTypesUtils.getMediaType(RepositoryUtils.getResourceName(path));
                if (temp != null) {
                    resource.setMediaType(temp);
                }
            }
        }
        
        if(resource.getUUID() == null){
            setUUIDForResource(resource);
        }

        resourceDAO.add(path, parentResourceID, resource);

        // Update the parent collection when adding a new resource.
        updateParent(parentResourceID);
    }

    // Method to update a parent resource
    private void updateParent(ResourceIDImpl parentResourceID) throws RepositoryException {
	//Commented out to avoid deadlock situation when concurrency level is high.
	//Done for Stratos-1.5.1 release.
	//This recursive parent update is causing deadlocks.
	//        if (parentResourceID.getPath() != null) {
	//            resourceDAO.updateCollectionLastUpdatedTime(parentResourceID);
	//        }
    }

    // this a method for updating a resource, which returns true, if the resource already exist,
    // or false otherwise.
    private void update(ResourceIDImpl resourceID, ResourceImpl resource, ResourceDO oldResourceDO) throws RepositoryException {

        RepositoryContext registryContext = null;
        if (CurrentContext.getRespository() != null) {
        	registryContext = InternalUtils.getRepositoryContext(CurrentContext.getRespository());
        }
        
        if (registryContext == null) {
            registryContext = RepositoryContext.getBaseInstance();
        }
        
        if (!Boolean.FALSE.equals(CurrentContext.getAttribute(IS_LOGGING_ACTIVITY))) {
            registryContext.getLogWriter().addLog(resourceID.getPath(), CurrentContext.getUser(),
                    Activity.UPDATE, null);
        }
        
        if (!(resource instanceof CollectionImpl)) {
            if (resource.getMediaType() == null || resource.getMediaType().length() == 0) {
                String temp = MediaTypesUtils.getMediaType(
                        RepositoryUtils.getResourceName(resourceID.getPath()));
                if (temp != null) {
                    resource.setMediaType(temp);
                }
            }
        }
        
        if(resource.getUUID() == null){
            setUUIDForResource(resource);
        }
        
        resourceDAO.update(resource);

        ResourceImpl oldResourceImpl;
        if (oldResourceDO.getName() == null) {
            // this is a collection
            oldResourceImpl = new CollectionImpl(resourceID.getPath(), oldResourceDO);
        } else {
            oldResourceImpl = new ResourceImpl(resourceID.getPath(), oldResourceDO);
        }
        // copying old required attributes to the new version
//        commentsDAO.copyComments(oldResourceImpl, resource);
//        tagsDAO.copyTags(oldResourceImpl, resource);
//        ratingsDAO.copyRatings(oldResourceImpl, resource);
    }

    private void setUUIDForResource(Resource resource) {
        resource.setUUID(UUID.randomUUID().toString());
    }

    /**
     * This will create an empty collection, if there is no such resource exist if the parent of the
     * empty collection doesn't exist, this will create they as well.
     *
     * @param path Path of which all non-existent collections are added.
     *
     * @throws RepositoryException If any ancestor of the given path is a resource.
     */
    private void addEmptyCollection(String path) throws RepositoryException {
        ResourceIDImpl assumedResourceID = resourceDAO.getResourceID(path, false);
        if (assumedResourceID != null && resourceDAO.resourceExists(assumedResourceID)) {
            String msg = "Failed to add new Collection " + path + "There already exist " +
                    "non collection resource.";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        String parentPath = RepositoryUtils.getParentPath(path);
        ResourceIDImpl parentResourceID = null;
        if (parentPath != null) {
            parentResourceID = resourceDAO.getResourceID(parentPath, true);
            if (parentResourceID == null || !resourceDAO.resourceExists(parentResourceID)) {
                addEmptyCollection(parentPath);
                if (parentResourceID == null) {
                    parentResourceID = resourceDAO.getResourceID(parentPath, true);
                }
            } 
        } else if (!path.equals(RepositoryConstants.ROOT_PATH)) {
            return;
        }

        CollectionImpl collection = new CollectionImpl();
        RepositoryContext registryContext = null;
        if (CurrentContext.getRespository() != null) {
        	registryContext = InternalUtils.getRepositoryContext(CurrentContext.getRespository());
        }
        if (registryContext == null) {
            registryContext = RepositoryContext.getBaseInstance();
        }
        if (!Boolean.FALSE.equals(CurrentContext.getAttribute(IS_LOGGING_ACTIVITY))) {
            registryContext.getLogWriter().addLog(
                    path, CurrentContext.getUser(), Activity.ADD, null);
        }
        if(collection.getUUID() == null){
            setUUIDForResource(collection);
        }
        resourceDAO.add(path, parentResourceID, collection);
    }

    /**
     * Method to restore a dump.
     *
     * @param _path               the path to restore from a dump.
     * @param reader             the reader used.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void restore(String _path, Reader reader) throws RepositoryException {
        String path=_path;
        boolean rootResourceExists = resourceExists(path);
        long currentVersion = -1;
        
        if (rootResourceExists) {
            ResourceImpl currentResource = resourceDAO.getResourceMetaData(path);
            if (!(currentResource instanceof Collection)) {
                currentVersion = currentResource.getVersionNumber();
            }
        }
        
        if (!path.equals("/") && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        if (path.equals(RepositoryConstants.ROOT_PATH)) {
        	// Do nothing
        } else {
            String parentPath = RepositoryUtils.getParentPath(path);
            ResourceImpl parentResource = resourceDAO.getResourceMetaData(parentPath);
            if (parentResource == null) {
                addEmptyCollection(parentPath);
            } else {
                if (!(parentResource instanceof CollectionImpl)) {
                    String msg = "Cannot restore into a non-collection at " + parentPath + ".";
                    log.error(msg);
                    throw new RepositoryServerContentException(msg);
                }
                if (!rootResourceExists) {
                	// Do nothing
                }
            }
        }
        
        DumpReader dumpReader = new DumpReader(reader);
        XMLStreamReader xmlReader;
        
        try {
            xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(dumpReader);
        } catch (Exception e) {
            String msg = "Error in creating the xml reader.";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
        
        try {
            restoreRecursively(path, xmlReader, dumpReader, currentVersion, rootResourceExists);
        } catch (XMLStreamException e) {
            String msg = "Failed to serialize the dumped element at " + path + ".";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Method to do a dump.
     *
     * @param _path   the path to obtain the dump from.
     * @param writer the writer used.
     *
     * @throws RepositoryException if the operation failed.
     */
    public void dump(String _path, Writer writer) throws RepositoryException {
        String path=_path;
        
        if (!path.equals("/") && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        
        XMLStreamWriter xmlWriter = null;
        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            xmlWriter = xof.createXMLStreamWriter(writer);

            // we are not using xmlWriter.writeStartDocument and writeEndDocument to get rid of the
            // xml descriptor it put in every child node
            dumpRecursively(path, xmlWriter, writer);            
        } catch (XMLStreamException e) {
            String msg = "Failed to serialize the dumped element at " + path + ".";
            log.error(msg);
            throw new RepositoryException(msg, e);
        } finally {
            if (xmlWriter != null) {
                try {
                    xmlWriter.close();
                } catch (XMLStreamException e) {
                }
            }
        }
    }

    // Method to do a recursive restore.
    private void restoreRecursively(String path, XMLStreamReader xmlReader, DumpReader dumpReader,
                                    long currentVersion, boolean resourceExists) throws RepositoryException, XMLStreamException {
        while (!xmlReader.isStartElement() && xmlReader.hasNext()) {
            xmlReader.next();
        }

        if (!xmlReader.hasNext()) {
            return;
        }

        if (!xmlReader.getLocalName().equals(DumpConstants.RESOURCE)) {
            String msg = "Invalid dump to restore at " + path;
            log.error(msg);
            throw new RepositoryException(msg);
        }

        String incomingParentPath = xmlReader.getAttributeValue(null, DumpConstants.RESOURCE_PATH);

        String resourceName = xmlReader.getAttributeValue(null, DumpConstants.RESOURCE_NAME);
        String ignoreConflictsStrValue = xmlReader.getAttributeValue(null, DumpConstants.IGNORE_CONFLICTS);

        boolean ignoreConflicts = true;
        
        if (ignoreConflictsStrValue != null && Boolean.toString(false).equals(ignoreConflictsStrValue)) {
            ignoreConflicts = false;
        }

        String isCollectionString = xmlReader.getAttributeValue(null, DumpConstants.RESOURCE_IS_COLLECTION);
        boolean isCollection = isCollectionString.equals(DumpConstants.RESOURCE_IS_COLLECTION_TRUE);

        if (path.equals(RepositoryConstants.ROOT_PATH) && !isCollection) {
            String msg = "Illegal to restore a non-collection in place of root collection.";
            log.error(msg);
            throw new RepositoryException(msg);
        }

        String status = xmlReader.getAttributeValue(null, DumpConstants.RESOURCE_STATUS);

        if (DumpConstants.RESOURCE_DELETED.equals(status) && resourceExists) {
            delete(path);
            return;
        }

        ResourceImpl resourceImpl;
        byte[] contentBytes = new byte[0];
        
        if (isCollection) {
            resourceImpl = new CollectionImpl();
        } else {
            resourceImpl = new ResourceImpl();
        }

        boolean isCreatorExisting = false;
        boolean isCreatedTimeExisting = false;
        boolean isUpdaterExisting = false;
        boolean isUpdatedTimeExisting = false;
        long dumpingResourceVersion = -1;

        do {
            xmlReader.next();
        } while (!xmlReader.isStartElement() && xmlReader.hasNext());

        while (xmlReader.hasNext()) {
            String localName = xmlReader.getLocalName();

            // setMediaType
            if (localName.equals(DumpConstants.MEDIA_TYPE)) {
                String text = xmlReader.getElementText();
                if (text.indexOf('/') < 0) {
                    text = MediaTypesUtils.getMediaType("dummy." + text);
                }
                if (text != null) {
                    resourceImpl.setMediaType(text);
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            } else if (localName.equals(DumpConstants.CREATOR)) {
                String text = xmlReader.getElementText();
                if (text != null) {
                    resourceImpl.setAuthorUserName(text);
                    isCreatorExisting = true;
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
            // version: just to keep track of the server changes
            else if (localName.equals(DumpConstants.VERSION)) {
                String text = xmlReader.getElementText();
                if (text != null) {
                    dumpingResourceVersion = Long.parseLong(text);
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
            // uuid: just to keep track of the server changes
            else if (localName.equals(DumpConstants.UUID)) {
                String text = xmlReader.getElementText();
                if (text != null) {
                    resourceImpl.setUUID(text);
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
            // createdTime
            else if (localName.equals(DumpConstants.CREATED_TIME)) {
                String text = xmlReader.getElementText();
                if (text != null) {
                    long date = Long.parseLong(text);
                    resourceImpl.setCreatedTime(new Date(date));
                    isCreatedTimeExisting = true;
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
            // setLastUpdater
            else if (localName.equals(DumpConstants.LAST_UPDATER)) {
                String text = xmlReader.getElementText();
                if (text != null) {
                    resourceImpl.setLastUpdaterUserName(text);
                    isUpdaterExisting = true;
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
            // LastModified
            else if (localName.equals(DumpConstants.LAST_MODIFIED)) {
                String text = xmlReader.getElementText();
                if (text != null) {
                    long date = Long.parseLong(text);
                    resourceImpl.setLastModified(new Date(date));
                    isUpdatedTimeExisting = true;
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
            // get description
            else if (localName.equals(DumpConstants.DESCRIPTION)) {
                String text = xmlReader.getElementText();
                if (text != null) {
                    resourceImpl.setDescription(text);
                }
                // now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
            // get properties
            else if (localName.equals(DumpConstants.PROPERTIES)) {
                // iterating trying to find the children..
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
                while (xmlReader.hasNext() &&
                        xmlReader.getLocalName().equals(DumpConstants.PROPERTY_ENTRY)) {
                    String key = xmlReader.getAttributeValue(null, DumpConstants.PROPERTY_ENTRY_KEY);
                    String text = xmlReader.getElementText();
                    if (text.equals("")) {
                        text = null;
                    }
                    if (text != null) {
                        resourceImpl.addPropertyWithNoUpdate(key, text);
                    }
                    do {
                        xmlReader.next();
                    } while (!xmlReader.isStartElement() && xmlReader.hasNext());
                }
            }
            // get content
            else if (localName.equals(DumpConstants.CONTENT)) {
                String text = xmlReader.getElementText();
                // we keep content as base64 encoded
                if (text != null) {
                	contentBytes = DatatypeConverter.parseBase64Binary(text);
                }
                do {
                    xmlReader.next();
                } while ((!xmlReader.isStartElement() && xmlReader.hasNext()) &&
                        !(xmlReader.isEndElement() &&
                                xmlReader.getLocalName().equals(DumpConstants.RESOURCE)));
            }

            // getting rating information
            else if (localName.equals(DumpConstants.ASSOCIATIONS)) {
                // iterating trying to find the children..
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
                while (xmlReader.hasNext() &&
                        xmlReader.getLocalName().equals(DumpConstants.ASSOCIATION_ENTRY)) {
                    String source = null;
                    String destination = null;
                    String type = null;

                    do {
                        xmlReader.next();
                    } while (!xmlReader.isStartElement() && xmlReader.hasNext());

                    localName = xmlReader.getLocalName();
                    while (xmlReader.hasNext() &&
                            (localName.equals(DumpConstants.ASSOCIATION_ENTRY_SOURCE) ||
                                    localName.equals(DumpConstants.ASSOCIATION_ENTRY_DESTINATION) ||
                                    localName.equals(DumpConstants.ASSOCIATION_ENTRY_TYPE))) {
                        if (localName.equals(DumpConstants.ASSOCIATION_ENTRY_SOURCE)) {
                            String text = xmlReader.getElementText();
                            if (text != null) {
                                source = text;
                            }
                        } else if (localName.equals(DumpConstants.ASSOCIATION_ENTRY_DESTINATION)) {
                            String text = xmlReader.getElementText();
                            if (text != null) {
                                destination = text;
                            }
                        } else if (localName.equals(DumpConstants.ASSOCIATION_ENTRY_TYPE)) {
                            String text = xmlReader.getElementText();
                            if (text != null) {
                                type = text;
                            }
                        }
                        do {
                            xmlReader.next();
                        } while (!xmlReader.isStartElement() && xmlReader.hasNext());
                        if (xmlReader.hasNext()) {
                            localName = xmlReader.getLocalName();
                        }
                    }
                    // get the source and destination as absolute paths
                    source = RepositoryUtils.getAbsoluteAssociationPath(source, path);
                    if (destination.startsWith(DumpConstants.EXTERNAL_ASSOCIATION_DESTINATION_PREFIX)) {
                        destination = destination.substring(DumpConstants.EXTERNAL_ASSOCIATION_DESTINATION_PREFIX.length());
                    } else {
                        destination = RepositoryUtils.getAbsoluteAssociationPath(destination, path);
                    }
//                    associationList.add(new Association(source, destination, type));
                }
            }
            
            // getting children, just storing in array list now, will used at the end
            // we are keeping old name to keep backward compatibility.
            else if (localName.equals(DumpConstants.CHILDREN) ||
                    localName.equals(DumpConstants.CHILDS)) {
                // we keep the stream to call this function recursively
                break;
            } else if (localName.equals(DumpConstants.RESOURCE)) {
                // we keep the stream to call this function recursively
                break;
            } else {
                // we don't mind having unwanted elements, now go to the next element
                do {
                    xmlReader.next();
                } while (!xmlReader.isStartElement() && xmlReader.hasNext());
            }
        }
        
        if (!ignoreConflicts) {
            // so we handling the conflicts.
            if (dumpingResourceVersion > 0) {
                if (currentVersion == -1) {
                    // the current version == -1 means the resource is deleted in the server
                    // but since the client is sending a version number, it has a previously checkout
                    // resource
                    String msg = "Resource is deleted in the server, resource path: " + path + ".";
                    log.error(msg);
                    throw new RepositoryException(msg);
                }
                // we should check whether our dump is up-to-date
                if (currentVersion > dumpingResourceVersion) {
                    // that mean the current resource is updated before the current version
                    // so we have to notify user to get an update
                    String msg = "Resource is in a newer version than the restoring version. " +
                            "resource path: " + path + ".";
                    log.error(msg);
                    throw new RepositoryException(msg);
                }
            }
        }

        // completing the empty fields
        if (!isCreatorExisting) {
            String creator = CurrentContext.getUser();
            resourceImpl.setAuthorUserName(creator);
        }
        
        if (!isCreatedTimeExisting) {
            long now = System.currentTimeMillis();
            resourceImpl.setCreatedTime(new Date(now));
        }
        
        if (!isUpdaterExisting) {
            String updater = CurrentContext.getUser();
            resourceImpl.setLastUpdaterUserName(updater);
        }
        
        if (!isUpdatedTimeExisting) {
            long now = System.currentTimeMillis();
            resourceImpl.setLastModified(new Date(now));
        }

        if(resourceImpl.getUUID() == null){
            setUUIDForResource(resourceImpl);
        }

        // create sym links
        String linkRestoration = resourceImpl.getPropertyValue(InternalConstants.REGISTRY_LINK_RESTORATION);
        if (linkRestoration != null) {
            String[] parts = linkRestoration.split(RepositoryConstants.URL_SEPARATOR);
            
            if (parts.length == 4) {
                if (parts[2] != null && parts[2].length() == 0) {
                    parts[2] = null;
                }
                if (parts[0] != null && parts[1] != null && parts[3] != null) {
                	InternalUtils.registerHandlerForRemoteLinks(RepositoryContext.getBaseInstance(), parts[0], parts[1], parts[2], parts[3]);
                }
            } else if (parts.length == 3) {
                // here parts[0] the current path, path[1] is the target path.
                if (parts[0] != null && parts[1] != null) {
                    // first we are calculating the relative path of path[1] to path[0]
                    String relativeTargetPath = RepositoryUtils.getRelativeAssociationPath(parts[1], parts[0]);
                    // then we derive the absolute path with reference to the current path.
                    String absoluteTargetPath = RepositoryUtils.getAbsoluteAssociationPath(relativeTargetPath, path);
                    InternalUtils.registerHandlerForSymbolicLinks(RepositoryContext.getBaseInstance(), path, absoluteTargetPath, parts[2]);
                }
            }
        }

        synchronized (this){
            ResourceIDImpl resourceID = null;
            ResourceDO resourceDO = null;
            
            if(resourceDAO.resourceExists(path)){
                resourceID = resourceDAO.getResourceID(path);
                resourceDO = resourceDAO.getResourceDO(resourceID);
                
                if (resourceDO == null) {
                    if (isCollection) {
                        resourceID = resourceDAO.getResourceID(path, isCollection);
                        
                        if (resourceID != null) {
                            resourceDO = resourceDAO.getResourceDO(resourceID);
                        }
                    }
                    
                    if (resourceDO == null) {
                        return;
                    }
                }
            }

            if(DumpConstants.RESOURCE_UPDATED.equals(status) || DumpConstants.RESOURCE_ADDED.equals(status) || DumpConstants.RESOURCE_DUMP.equals(status)) {
                if(resourceDAO.resourceExists(path)){
                    if (DumpConstants.RESOURCE_DUMP.equals(status)) {
                        delete(path);
                    } else {
                        deleteNode(resourceID, resourceDO, true);
                    }
                }
                
                if (resourceID == null) {
                    // need to create a resourceID
                    String parentPath = RepositoryUtils.getParentPath(path);

                    ResourceIDImpl parentResourceID = resourceDAO.getResourceID(parentPath, true);
                    if (parentResourceID == null || !resourceDAO.resourceExists(parentResourceID)) {
                        addEmptyCollection(parentPath);
                        if (parentResourceID == null) {
                            parentResourceID = resourceDAO.getResourceID(parentPath, true);
                        }
                    }
                    resourceDAO.createAndApplyResourceID(path, parentResourceID, resourceImpl);
                } else {
                    resourceImpl.setPathID(resourceID.getPathID());
                    resourceImpl.setName(resourceID.getName());
                    resourceImpl.setPath(path);
                }

                // adding resource followed by content (for nonCollection)
                if (!isCollection) {
                    int contentId = 0;
                    
                    if (contentBytes.length > 0) {
                        contentId = resourceDAO.addContentBytes(new ByteArrayInputStream(contentBytes));
                    }
                    
                    resourceImpl.setDbBasedContentID(contentId);
                }

                resourceDO = resourceImpl.getResourceDO();
                resourceDAO.addResourceDO(resourceDO);
                resourceImpl.setVersionNumber(resourceDO.getVersion());

                // adding the properties.
                resourceDAO.addProperties(resourceImpl);
            }
        }

        if (!xmlReader.hasNext() || !(xmlReader.getLocalName().equals(DumpConstants.CHILDREN) ||
                xmlReader.getLocalName().equals(DumpConstants.CHILDS))) {
            // finished the recursion
            return;
        }

        do {
            xmlReader.next();
            if (xmlReader.isEndElement() &&
                    (xmlReader.getLocalName().equals(DumpConstants.CHILDREN) ||
                            xmlReader.getLocalName().equals(DumpConstants.CHILDS))) {
                // this means empty children, just quit from here
                // before that we have to set the cursor to the start of the next element
                if (xmlReader.hasNext()) {
                    do {
                        xmlReader.next();
                    } while ((!xmlReader.isStartElement() && xmlReader.hasNext()) &&
                            !(xmlReader.isEndElement() &&
                                    xmlReader.getLocalName().equals(DumpConstants.RESOURCE)));
                }
                Resource resource = get(path);
                
                if (resource instanceof Collection) {
                    String[] existingChildren =  ((Collection)resource).getChildPaths();
                    for (String existingChild : existingChildren) {
                        delete(existingChild);
                    }
                }
                
                return;
            }
        } while (!xmlReader.isStartElement() && xmlReader.hasNext());

        int i = 0;
        
        if (xmlReader.hasNext() && xmlReader.getLocalName().equals(DumpConstants.RESOURCE)) {
            Set<String> childPathSet = new HashSet<String>();
            
            while (true) {
                if (i != 0) {
                    dumpReader.setReadingChildResourceIndex(i);

                    // otherwise we will set the stuff for the next resource
                    // get an xlm reader in the checking child by parent mode.
                    xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(dumpReader);

                    while (!xmlReader.isStartElement() && xmlReader.hasNext()) {
                        xmlReader.next();
                    }
                }

                String absoluteChildPath;
                
                if (incomingParentPath != null) {
                    // the code to support backward compatibility.
                    // prepare the children absolute path
                    String incomingChildPath = xmlReader.getAttributeValue(null, DumpConstants.RESOURCE_PATH);
/*                    if (!incomingChildPath.startsWith(incomingParentPath)) {
                        //break;
                    }*/

                    String relativeChildPath;
                    
                    if (incomingParentPath.equals(RepositoryConstants.ROOT_PATH)) {
                        relativeChildPath = incomingChildPath;
                    } else {
                        if (incomingParentPath.contains(incomingChildPath)) {
                            relativeChildPath =
                                    incomingChildPath.substring(incomingParentPath.length());
                        } else {
                            // this happens only at some custom editing of dump.xml
                            relativeChildPath = null;
                        }
                    }
                    if (relativeChildPath != null) {
                        if (path.equals(RepositoryConstants.ROOT_PATH)) {
                            absoluteChildPath = relativeChildPath;
                        } else {
                            absoluteChildPath = path + relativeChildPath;
                        }
                    } else {
                        String checkoutRoot =
                                path.substring(0, path.length() - incomingParentPath.length());
                        absoluteChildPath = checkoutRoot + incomingChildPath;
                    }
                } else if (resourceName != null) {
                    String childName = xmlReader.getAttributeValue(null,
                            DumpConstants.RESOURCE_NAME);
                    absoluteChildPath = path +
                            (path.equals(RepositoryConstants.ROOT_PATH) ? "" :
                                    RepositoryConstants.PATH_SEPARATOR) +
                            childName;
                } else {
                    String msg = "Error in deriving the child paths for collection. path: " + path + ".";
                    log.error(msg);
                    throw new RepositoryException(msg);
                }

                // we give the control back to the child.
                dumpReader.setCheckingChildByParent(false);

                dumpReader.setReadingChildResourceIndex(i);
                // call the check in method recursively

                recursionRepository.restoreRecursively(absoluteChildPath, dumpReader);
                childPathSet.add(absoluteChildPath);

                dumpReader.setCheckingChildByParent(true);
                
                try {
                    if (dumpReader.isLastResource(i)) {
                        dumpReader.setCheckingChildByParent(false);
                        break;
                    }
                } catch (IOException e) {
                    String msg = "Error in checking the last resource exists.";
                    log.error(msg, e);
                    throw new RepositoryException(msg + e.getMessage(), e);
                }
                // by this time i ++ child resource should exist
                i++;
            }
            
            Collection parent = (Collection) get(path);
            String[] existingChildren =  parent.getChildPaths();
            
            for (String existingChild : existingChildren) {
                if (!childPathSet.contains(existingChild)) {
                    delete(existingChild);
                }
            }
        }
    }

    // Method to do a recursive dump
    private void dumpRecursively(String path, XMLStreamWriter xmlWriter, Writer writer) throws RepositoryException, XMLStreamException {
        // adding resource meta data
        ResourceImpl resource = resourceDAO.getResourceMetaData(path);
        
        if (resource == null) {
            return;
        }

        xmlWriter.writeStartElement(DumpConstants.RESOURCE);

        // adding path as an attribute, updated dump has name instead of path
        xmlWriter.writeAttribute(DumpConstants.RESOURCE_NAME, RepositoryUtils.getResourceName(path));

        //adding dump attribute
        xmlWriter.writeAttribute(DumpConstants.RESOURCE_STATUS, DumpConstants.RESOURCE_DUMP);

        // adding isCollection as an attribute
        xmlWriter.writeAttribute(DumpConstants.RESOURCE_IS_COLLECTION,
                (resource instanceof CollectionImpl) ? DumpConstants.RESOURCE_IS_COLLECTION_TRUE : DumpConstants.RESOURCE_IS_COLLECTION_FALSE);
        
        // set media type
        String mediaType = resource.getMediaType();
        
        xmlWriter.writeStartElement(DumpConstants.MEDIA_TYPE);
        xmlWriter.writeCharacters(mediaType != null ? mediaType : "");
        xmlWriter.writeEndElement();

        // set version
        long version = resource.getVersionNumber();
        
        xmlWriter.writeStartElement(DumpConstants.VERSION);
        xmlWriter.writeCharacters(Long.toString(version) != null ? Long.toString(version) : "");
        xmlWriter.writeEndElement();

        // set creator
        String creator = resource.getAuthorUserName();
        
        xmlWriter.writeStartElement(DumpConstants.CREATOR);
        xmlWriter.writeCharacters(creator != null ? creator : "");
        xmlWriter.writeEndElement();

        // set createdTime
        Date createdTime = resource.getCreatedTime();
        
        xmlWriter.writeStartElement(DumpConstants.CREATED_TIME);
        xmlWriter.writeCharacters(Long.toString(createdTime.getTime()) != null ? Long.toString(createdTime.getTime()) : "");
        xmlWriter.writeEndElement();

        // set updater
        String updater = resource.getLastUpdaterUserName();
        
        xmlWriter.writeStartElement(DumpConstants.LAST_UPDATER);
        xmlWriter.writeCharacters(updater != null ? updater : "");
        xmlWriter.writeEndElement();

        // set LastModified
        Date lastModified = resource.getLastModified();
        
        xmlWriter.writeStartElement(DumpConstants.LAST_MODIFIED);
        xmlWriter.writeCharacters(Long.toString(lastModified.getTime()) != null ? Long.toString(lastModified.getTime()) : "");
        xmlWriter.writeEndElement();

        // set UUID
        String uuid = resource.getUUID();
        
        xmlWriter.writeStartElement(DumpConstants.UUID);
        xmlWriter.writeCharacters(uuid != null ? uuid : "");
        xmlWriter.writeEndElement();

        // set Description
        String description = resource.getDescription();
        
        xmlWriter.writeStartElement(DumpConstants.DESCRIPTION);
        xmlWriter.writeCharacters(description != null ? description : "");
        xmlWriter.writeEndElement();

        // fill properties
        resourceDAO.fillResourceProperties(resource);
        xmlWriter.writeStartElement(DumpConstants.PROPERTIES);

        for (Object keyObject : resource.getPropertyKeys()) {
            String key = (String) keyObject;
            List<String> propValues = resource.getPropertyValues(key);
            for (String value : propValues) {
                xmlWriter.writeStartElement(DumpConstants.PROPERTY_ENTRY);
                // adding the key and value as attributes

                xmlWriter.writeAttribute(DumpConstants.PROPERTY_ENTRY_KEY, key);

                if (value != null) {
                    xmlWriter.writeCharacters(value != null ? value : "");
                }

                xmlWriter.writeEndElement();
            }
        }
        xmlWriter.writeEndElement();
        
        // adding contents..
        if (!(resource instanceof CollectionImpl)) {
            resourceDAO.fillResourceContent(resource);

            byte[] content = (byte[]) resource.getContent();
            if (content != null) {
            	xmlWriter.writeStartElement(DumpConstants.CONTENT);
            	xmlWriter.writeCharacters(DatatypeConverter.printBase64Binary(content) != null ? DatatypeConverter.printBase64Binary(content) : "");
            	xmlWriter.writeEndElement();
            }
        }

        // getting children and applying dump recursively
        if (resource instanceof CollectionImpl) {
            CollectionImpl collection = (CollectionImpl) resource;
            resourceDAO.fillChildren(collection, 0, -1);
            String childPaths[] = collection.getChildPaths();

            xmlWriter.writeStartElement(DumpConstants.CHILDREN);
            xmlWriter.writeCharacters("");
            
            xmlWriter.flush();
            
            for (String childPath : childPaths) {
                // we would be writing the start element of the child and its name here.
                try {
                    String resourceName = RepositoryUtils.getResourceName(childPath);
                    writer.write("<resource name=\"" + resourceName + "\"");
                    writer.flush();
                } catch (IOException e) {
                    String msg = "Error in writing the start element for the path: " + childPath + ".";
                    log.error(msg, e);
                    throw new RepositoryException(msg, e);
                }
                
                recursionRepository.dumpRecursively(childPath, new DumpWriter(writer));
            }
            xmlWriter.writeEndElement();
        }
        
        xmlWriter.writeEndElement();
        xmlWriter.flush();
    }
}
