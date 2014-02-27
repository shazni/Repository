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

package org.wso2.carbon.repository.core.handlers.builtin;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.core.CollectionImpl;
import org.wso2.carbon.repository.core.CurrentContext;
import org.wso2.carbon.repository.core.EmbeddedRepository;
import org.wso2.carbon.repository.core.ResourceImpl;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.exceptions.RepositoryServerContentException;
import org.wso2.carbon.repository.core.handlers.HandlerLifecycleManager;
import org.wso2.carbon.repository.core.handlers.HandlerManager;
import org.wso2.carbon.repository.core.utils.InternalConstants;
import org.wso2.carbon.repository.core.utils.InternalUtils;

/**
 * This handler is used to create a symbolic link from one resource to another and perform registry
 * operations on the symbolic link and have them applied on the actual resource as if the actual
 * resource itself was being used. The symbolic link handler plays a key role to make it possible to
 * create and work with symbolic links via the user interface.
 */
public class SymLinkHandler extends Handler {

    private static final Log log = LogFactory.getLog(ResourceImpl.class);

    // sym-links map to resolve cyclic symlink
    private static Set<SymLinkHandler> symLinkHandlers = new HashSet<SymLinkHandler>();

    private String mountPoint;
    private String targetPoint;
    private String author;

    private boolean isHandlerRegistered = false;

    public int hashCode() {
        return getEqualsComparator().hashCode();
    }

    public boolean equals(Object obj) {
        return (obj != null && obj instanceof SymLinkHandler &&
                ((SymLinkHandler) obj).getEqualsComparator().equals(getEqualsComparator()));
    }


    // Method to generate a unique string that can be used to compare two objects of the same type
    // for equality.
    private String getEqualsComparator() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append("|");
        sb.append(mountPoint);
        sb.append("|");
        sb.append(targetPoint);
        sb.append("|");
        sb.append(author);
        return sb.toString();
    }

    public void put(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        String fullPath = requestContext.getResourcePath().getPath();
        String actualPath = getActualPath(fullPath);
        
        try {
            Resource resource = requestContext.getResource();

            // removes the following properties if they are present
            resource.removeProperty(RepositoryConstants.REGISTRY_MOUNT_POINT);
            resource.removeProperty(RepositoryConstants.REGISTRY_TARGET_POINT);
            resource.removeProperty(RepositoryConstants.REGISTRY_AUTHOR);
            resource.removeProperty(RepositoryConstants.REGISTRY_LINK);
            resource.removeProperty(RepositoryConstants.REGISTRY_ACTUAL_PATH);

            // sets the actual path of the resource
            requestContext.getRepository().put(actualPath, requestContext.getResource());
            requestContext.setProcessingComplete(true);
        } catch (Exception e) {
            throw new RepositoryServerContentException(e.getMessage());
        }
    }

    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        boolean resourceExists = false;
        String fullPath = requestContext.getResourcePath().getPath();
        String subPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
        String actualPath = this.targetPoint + subPath;
        
        try {
            resourceExists = requestContext.getRepository().resourceExists(actualPath);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Could not create the symbolic link. Target " + actualPath + "not found.");
            }
            log.debug("Caused by: ", e);
        }
        
        requestContext.setProcessingComplete(true);
        
        return resourceExists;
    }

    public Resource get(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        String fullPath = requestContext.getResourcePath().getPath();
        String subPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
        String actualPath = this.targetPoint + subPath;
        
        Resource tempResource;
        
        if (requestContext.getRepository().resourceExists(actualPath)) {
            tempResource = requestContext.getRepository().get(actualPath);
            if (tempResource instanceof Collection) {
                String[] paths = (String[]) tempResource.getContent();
                for (int i = 0; i < paths.length; i++) {
                    if (this.targetPoint.equals(RepositoryConstants.PATH_SEPARATOR)) {
                        paths[i] = this.mountPoint + paths[i];
                    } else {
                        if(((EmbeddedRepository) requestContext.getRepository()).getConcatenatedRoot() != null && 
                        		!paths[i].startsWith(((EmbeddedRepository) requestContext.getRepository()).getConcatenatedRoot())) {
                        	paths[i] = ((EmbeddedRepository) requestContext.getRepository()).getConcatenatedRoot() + paths[i] ;
                        }
                        paths[i] = this.mountPoint + paths[i].substring(this.targetPoint.length(), paths[i].length());
                    }
                }
                ((CollectionImpl) tempResource).setContentWithNoUpdate(paths);
            }
            
            ((ResourceImpl) tempResource).setPath(fullPath);
            ((ResourceImpl) tempResource).setAuthorUserName(author);
            ((ResourceImpl) tempResource).setUserName(CurrentContext.getUser());
            ((ResourceImpl) tempResource).setTenantId(CurrentContext.getTenantId());
        } else {
        	tempResource = ((EmbeddedRepository) requestContext.getRepository()).getRepository().get(this.mountPoint);
            ((ResourceImpl) tempResource).addPropertyWithNoUpdate("registry.absent", "true");
            tempResource.setDescription("Couldn't create the symbolic link. Content can't be displayed.");
        }
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_LINK, "true");
        ((ResourceImpl) tempResource).removePropertyWithNoUpdate(RepositoryConstants.REGISTRY_NON_RECURSIVE);
        ((ResourceImpl) tempResource).removePropertyWithNoUpdate(InternalConstants.REGISTRY_LINK_RESTORATION);
        
        // ensure that a symlink to a remote link will not become a remote link.
        ((ResourceImpl) tempResource).removePropertyWithNoUpdate(RepositoryConstants.REGISTRY_REAL_PATH);
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_MOUNT_POINT, this.mountPoint);
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_TARGET_POINT, this.targetPoint);
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_AUTHOR, author);

        if (tempResource.getProperty(RepositoryConstants.REGISTRY_ACTUAL_PATH) == null) {
            ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_ACTUAL_PATH, actualPath);
        }
        
        //Used to store paths when there are recursive calls
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate("registry.path", fullPath);
        requestContext.setProcessingComplete(true);
        
        return tempResource;
    }

    public void delete(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        String fullPath = requestContext.getResourcePath().getPath();
        
        Repository registry = requestContext.getRepository();
        RepositoryContext registryContext = InternalUtils.getRepositoryContext(registry) ;
        
        if (fullPath.equals(this.mountPoint)) {
            requestContext.getRepository().removeLink(fullPath);
            if (registryContext != null) {
                HandlerManager hm = registryContext.getHandlerManager();
                hm.removeHandler(this,
                        HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
            }
        } else {
            String subPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
            String actualPath = this.targetPoint + subPath;
            requestContext.getRepository().delete(actualPath);
        }
        
        requestContext.setProcessingComplete(true);
    }

    public String rename(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        String fullResourcePath = requestContext.getSourcePath();
        String fullTargetPath = requestContext.getInstanceId();
        
        Repository registry = requestContext.getRepository();
        RepositoryContext registryContext = InternalUtils.getRepositoryContext(registry) ;
        
        if (fullResourcePath.equals(this.mountPoint)) {
            requestContext.getRepository().removeLink(this.mountPoint);
            
            if (registryContext != null) {
                HandlerManager hm = registryContext.getHandlerManager();
                hm.removeHandler(this, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
            }
            
            requestContext.getRepository().createLink(fullTargetPath, this.targetPoint);
        } else {
            String subPath = fullResourcePath.substring(this.mountPoint.length(), fullResourcePath.length());
            String actualResourcePath = this.targetPoint + subPath;
            subPath = fullTargetPath.substring(this.mountPoint.length(), fullTargetPath.length());
            String actualTargetPath = this.targetPoint + subPath;
            requestContext.getRepository().rename(actualResourcePath, actualTargetPath);
        }
        
        requestContext.setProcessingComplete(true);

        return fullTargetPath;
    }

    public String move(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        String fullResourcePath = requestContext.getSourcePath();
        String fullTargetPath = requestContext.getInstanceId();
        
        Repository registry = requestContext.getRepository();
        RepositoryContext registryContext = InternalUtils.getRepositoryContext(registry) ;
        
        if (fullResourcePath.equals(this.mountPoint)) {
            requestContext.getRepository().removeLink(this.mountPoint);
            
            if (registryContext != null) {
                HandlerManager hm = registryContext.getHandlerManager();
                hm.removeHandler(this, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
            }
            
            requestContext.getRepository().createLink(fullTargetPath, this.targetPoint);
        } else if (fullResourcePath.startsWith(this.mountPoint)) {
            String subPath = fullResourcePath.substring(this.mountPoint.length(), fullResourcePath.length());
            String actualResourcePath;

            if (this.targetPoint.equals(RepositoryConstants.PATH_SEPARATOR)) {
                actualResourcePath = subPath;
            } else {
                actualResourcePath = this.targetPoint + subPath;
            }
            
            requestContext.getRepository().move(actualResourcePath, fullTargetPath);
        } else if (fullTargetPath.startsWith(this.mountPoint)) {
            String subPath = fullTargetPath.substring(this.mountPoint.length(), fullTargetPath.length());
            String actualTargetPath;

            if (this.targetPoint.equals(RepositoryConstants.PATH_SEPARATOR)) {
                actualTargetPath = subPath;
            } else {
                actualTargetPath = this.targetPoint + subPath;
            }
            
            requestContext.getRepository().move(fullResourcePath, actualTargetPath);
        }
        
        requestContext.setProcessingComplete(true);

        return fullTargetPath;
    }

    public String copy(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        String fullResourcePath = requestContext.getSourcePath();
        String fullTargetPath = requestContext.getInstanceId();
        
        if (fullResourcePath.equals(this.mountPoint)) {
            requestContext.getRepository().createLink(fullTargetPath, this.targetPoint);
        } else if (fullResourcePath.startsWith(this.mountPoint)) {
            String subPath = fullResourcePath.substring(this.mountPoint.length(), fullResourcePath.length());
            String actualResourcePath;

            if (this.targetPoint.equals(RepositoryConstants.PATH_SEPARATOR)) {
                actualResourcePath = subPath;
            } else {
                actualResourcePath = this.targetPoint + subPath;
            }
            
            requestContext.getRepository().copy(actualResourcePath, fullTargetPath);
        } else if (fullTargetPath.startsWith(this.mountPoint)) {
            String subPath = fullTargetPath.substring(this.mountPoint.length(), fullTargetPath.length());
            String actualTargetPath;

            if (this.targetPoint.equals(RepositoryConstants.PATH_SEPARATOR)) {
                actualTargetPath = subPath;
            } else {
                actualTargetPath = this.targetPoint + subPath;
            }
            
            requestContext.getRepository().copy(fullResourcePath, actualTargetPath);
        }
        
        requestContext.setProcessingComplete(true);

        return fullTargetPath;
    }

    public void importResource(HandlerContext requestContext) throws RepositoryException {
    	registerHandler(InternalUtils.getSystemRegistry(requestContext.getRepository()));
        String fullPath = requestContext.getResourcePath().getPath();
        String actualPath = getActualPath(fullPath);

        requestContext.getRepository().importResource(actualPath, requestContext.getSourceURL(), requestContext.getResource());

        requestContext.setProcessingComplete(true);
    }



    public void removeLink(HandlerContext requestContext) {
        requestContext.setProperty(InternalConstants.SYMLINK_TO_REMOVE_PROPERTY_NAME, this);
        // we are not setting the processing complete true, as the basic registry itself
        // has the operation to do in removing permanent entries.
    }

    private void registerHandler(Repository registry) throws RepositoryException {
        if (!isHandlerRegistered) {
            InternalUtils.addMountEntry(registry, RepositoryContext.getBaseInstance(), mountPoint, targetPoint, false, author);
            isHandlerRegistered = true;
        }
    }

    /**
     * Method to set the mount point
     *
     * @param mountPoint the mount point
     */
    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    /**
     * Method to remove an already mount point
     *
     * @return mountPoint the mount point
     */
    public String getMountPoint() {
        return this.mountPoint;
    }

    /**
     * Method to set the target point
     *
     * @param targetPoint the target point
     */
    public void setTargetPoint(String targetPoint) {
        if (targetPoint.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
            this.targetPoint = targetPoint.substring(0, targetPoint.length() - 1);
        } else {
            this.targetPoint = targetPoint;
        }
    }

    /**
     * Method to get the target point
     *
     * @return the target point
     */
    public String getTargetPoint() {
        return targetPoint;
    }

    // Utility method used to compute the actual path
    private String getActualPath(String fullPath) {
        String actualPath;
        
        if (fullPath.equals(this.mountPoint)) {
            actualPath = this.targetPoint;
        } else {
            String subPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
            if (this.targetPoint.equals(RepositoryConstants.PATH_SEPARATOR)) {
                actualPath = subPath;
            } else {
                actualPath = this.targetPoint + subPath;
            }
        }

        return actualPath;
    }

    /**
     * Method to set the author
     *
     * @param author the author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Return the sym-link handler set
     *
     * @return  sym-link handler set
     */
    public static Set<SymLinkHandler> getSymLinkHandlers() {
        return symLinkHandlers;
    }
}
