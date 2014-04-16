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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.*;
import org.wso2.carbon.repository.api.exceptions.RepositoryErrorCodes;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.exceptions.RepositoryResourceNotFoundException;
import org.wso2.carbon.repository.api.handlers.Handler;
import org.wso2.carbon.repository.api.handlers.HandlerContext;
import org.wso2.carbon.repository.core.*;
import org.wso2.carbon.repository.core.config.RepositoryContext;
import org.wso2.carbon.repository.core.config.StaticConfiguration;
import org.wso2.carbon.repository.core.exceptions.RepositoryDBException;
import org.wso2.carbon.repository.core.exceptions.RepositoryServerContentException;
import org.wso2.carbon.repository.core.handlers.HandlerLifecycleManager;
import org.wso2.carbon.repository.core.utils.InternalConstants;
import org.wso2.carbon.repository.core.utils.InternalUtils;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * This class is used to handle registry calls to mounted registry instances. This plays a key role
 * in making it possible to do remote mounting via configuration (registry.xml) as well as the user
 * interface in a transparent manner.
 */
public class MountHandler extends Handler {

    private static final Log log = LogFactory.getLog(ResourceImpl.class);

    private static final String EXECUTE_QUERY_CONNECTION_LIST_KEY = "execute.query.conn.key";

    private String id;
    private String conURL;
    private String mountPoint;
    private String userName;
    private String password;
    private String subPath;
    private String author;
    private String dbConfig;
    private boolean readOnly;
    private boolean cacheEnabled;
    private String repositoryRoot;
    private boolean remote;
    private String registryType;

    private EmbeddedRepositoryService repositoryService;

    private boolean isHandlerRegistered = false;

    private ThreadLocal<Boolean> inExecution =
            new ThreadLocal<Boolean>() {
                protected Boolean initialValue() {
                    return false;
                }
            };

    private Boolean isInExecution() {
        return inExecution.get();
    }

    private void setInExecution(Boolean input) {
        inExecution.set(input);
    }

    @SuppressWarnings("unchecked")
    private List<String> getQueriedConnectionList() {
        return (List<String>)CurrentContext.getAttribute(EXECUTE_QUERY_CONNECTION_LIST_KEY);
    }

    private void setQueriedConnectionList(List<String> input) {
        CurrentContext.setAttribute(EXECUTE_QUERY_CONNECTION_LIST_KEY, input);
    }

    public int hashCode() {
        return getEqualsComparator().hashCode();
    }

    public boolean equals(Object obj) {
        return (obj instanceof MountHandler &&
                ((MountHandler) obj).getEqualsComparator().equals(getEqualsComparator()));
    }

    // Method to generate a unique string that can be used to compare two objects of the same type
    // for equality.
    private String getEqualsComparator() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append("|");
        sb.append(id);
        sb.append("|");
        sb.append(conURL);
        sb.append("|");
        sb.append(mountPoint);
        sb.append("|");
        sb.append(userName);
        sb.append("|");
        sb.append(password);
        sb.append("|");
        sb.append(subPath);
        sb.append("|");
        sb.append(author);
        sb.append("|");
        sb.append(getDbConfig());
        sb.append("|");
        sb.append(getRegistryRoot());
        sb.append("|");
        sb.append(getReadOnly());
        sb.append("|");
        sb.append(getCacheEnabled());
        sb.append("|");
        sb.append(Boolean.toString(remote));
        sb.append("|");
        sb.append(registryType);
        return sb.toString();
    }

    // Obtains a registry instance to work with, based on the configuration
    private Repository getRepository(HandlerContext requestContext) throws RepositoryException {
        RepositoryContext baseContext = RepositoryContext.getBaseInstance();
        
        if (!isHandlerRegistered && baseContext != null) {
            try {               
                Repository systemRepository = ((EmbeddedRepositoryService) repositoryService).getSystemRepository(CurrentContext.getTenantId(), baseContext.getRepositoryRoot());

                if (subPath != null) {
                    InternalUtils.addMountEntry(systemRepository, RepositoryContext.getBaseInstance(), mountPoint, id, subPath, author);
                } else {
                	InternalUtils.addMountEntry(systemRepository, RepositoryContext.getBaseInstance(), mountPoint, id, true, author);
                }
                
                isHandlerRegistered = true;
            } catch (RepositoryException e) {
                log.error("Unable to add mount entry into the Repository", e);
            }
        }
        
        if (remote) {
        	// Do nothing
        }
        
        if (repositoryService == null) {
            RepositoryContext context = RepositoryContext.getCloneContext();
            
            context.setRegistryRoot(repositoryRoot);
            context.setReadOnly(readOnly);
            context.setCacheEnabled(cacheEnabled);
            context.selectDBConfig(dbConfig);
            
            repositoryService = context.getEmbeddedRepositoryService();
            
            repositoryService.setReadOnly(readOnly);
            repositoryService.setIsClone(true);
            repositoryService.setRepositoryRoot(repositoryRoot);
            
            StaticConfiguration.setRepositoryRoot(repositoryRoot);
        }
        
        return repositoryService.getRepository(CurrentContext.getUser(), CurrentContext.getCallerTenantId());
    }

    // Starts nested operation with a remotePath to localPath mapping
    private void beginNestedOperation(String remotePath, String localPath) {
        if (!remote) {
            Map<String, String> localPathMap = new HashMap<String, String>();
            localPathMap.put(remotePath, localPath);
            CurrentContext.setLocalPathMap(localPathMap);
            Transaction.pushTransaction();
        }
    }

    // Starts nested operation with multiple remotePath to localPath mappings
    private void beginNestedOperation(Map<String, String> localPathMap) {
        if (!remote) {
            CurrentContext.setLocalPathMap(localPathMap);
            Transaction.pushTransaction();
        }
    }

    // Ends nested operation.
    private void endNestedOperation() {
        if (!remote) {
            Transaction.popTransaction();
            CurrentContext.removeLocalPathMap();
        }
    }

    public void put(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            super.put(requestContext);
            return;
        } else {
            setInExecution(true);
        }
        
        String fullPath = requestContext.getResourcePath().getPath();
        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
        
        if (subPath != null) {
            actualPath = subPath + actualPath;
        }
        
        if (actualPath.length() == 0) {
            actualPath = "/";
        }
        
        try {
            Repository remoteRegistry = getRepository(requestContext);
            Resource resource = requestContext.getResource();

            // removes the following properties if they are present
            resource.removeProperty(RepositoryConstants.REGISTRY_LINK);
            resource.removeProperty(RepositoryConstants.REGISTRY_USER);
            resource.removeProperty(RepositoryConstants.REGISTRY_MOUNT);
            resource.removeProperty(RepositoryConstants.REGISTRY_AUTHOR);
            resource.removeProperty(RepositoryConstants.REGISTRY_REAL_PATH);

            // This is set to distinguish operations on xsd and wsdl on remote mounting.
            if( InternalConstants.XSD_MEDIA_TYPE.equals(resource.getMediaType())
                || RepositoryConstants.WSDL_MEDIA_TYPE.equals(resource.getMediaType() )) {
                resource.addProperty(RepositoryConstants.REMOTE_MOUNT_OPERATION, "true");
            }
            
            // sets the actual path of the resource
            ((ResourceImpl) resource).setPath(actualPath);
            beginNestedOperation(actualPath, fullPath);
            
            try {
                remoteRegistry.put(actualPath, resource);
            } finally {
                endNestedOperation();
            }
            
            requestContext.setProcessingComplete(true);
        } catch (Exception e) {
            throw new RepositoryServerContentException("Unable to put resource", e);
        } finally {
            setInExecution(false);
        }
    }

    public boolean resourceExists(HandlerContext requestContext) throws RepositoryException {
        return resourceExists(requestContext, false);
    }

    private boolean resourceExists(HandlerContext requestContext, boolean verify)
            throws RepositoryException {
        if (isInExecution()) {
            return super.resourceExists(requestContext);
        } else {
            setInExecution(true);
        }
        String fullPath = requestContext.getResourcePath().getPath();
        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
        boolean resourceExists = false;
        if (subPath != null) {
            actualPath = subPath + actualPath;
        }
        if (actualPath.length() == 0) {
            actualPath = "/";
        }
        try {
            Repository remoteRegistry = getRepository(requestContext);
            beginNestedOperation(actualPath, fullPath);
            try {
                resourceExists = remoteRegistry.resourceExists(actualPath);
                if (verify && resourceExists) {
                    Resource resource = remoteRegistry.get(actualPath);
                    if (resource.getPropertyValue(/*RepositoryConstants.*/ InternalConstants.REGISTRY_LINK_RESTORATION) != null) {
                        resourceExists = false;
                    }
                }
            } finally {
                endNestedOperation();
            }
            requestContext.setProcessingComplete(true);
        } catch (Exception e) {
            log.error("Unable to test existence of resource", e);
        }
        setInExecution(false);
        return resourceExists;
    }

    public RepositoryContext getRegistryContext(HandlerContext requestContext) {
        if (isInExecution()) {
            //return super.getRegistryContext(requestContext);  //effectively this is null
        	return null ;  // Shazni
        } else {
            setInExecution(true);
        }
        RepositoryContext registryContext;
        Repository remoteRegistry;
        try {
            remoteRegistry = getRepository(requestContext);
            beginNestedOperation(new HashMap<String, String>());
            try {
//                registryContext = ((RemoteRegistry) remoteRegistry).getRegistryContext();
            	registryContext = InternalUtils.getRepositoryContext(remoteRegistry);
            } finally {
                endNestedOperation();
            }
        } catch (Exception e) {
            log.error("An error occurred while obtaining the remote registry instance", e);
            return null;
        }
        requestContext.setProcessingComplete(true);
        setInExecution(false);
        return registryContext;
    }

    public Resource get(HandlerContext requestContext) throws RepositoryException {
    	
        if (isInExecution()) {
            return super.get(requestContext);
        } else {
            setInExecution(true);           
        }
        
        String fullPath = requestContext.getResourcePath().getPath();
        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
        
        if (subPath != null) {
            actualPath = subPath + actualPath;
        }
        
        if (actualPath.length() == 0) {
            actualPath = "/";
        }
        
        Resource tempResource;
        
        try {
            Repository remoteRegistry = getRepository(requestContext);
            beginNestedOperation(actualPath, fullPath);
            
            boolean resourceExists = false;
            
            try {
                resourceExists = remoteRegistry.resourceExists(actualPath);
            } finally {
                endNestedOperation();
            }
            
            if (resourceExists) {
                beginNestedOperation(actualPath, fullPath);
                try {
                    tempResource = remoteRegistry.get(actualPath);
                    if (!remote) {
                        // We need to get content here, so that the content is fetched using the
                        // nested transaction. If not, it will be fetched using the base transaction
                        // which will cause problems, in the mounted scenario.
                        tempResource.getContent();
                    }
                } finally {
                    endNestedOperation();
                }
                
                if (tempResource instanceof Collection) {
                    String[] paths = (String[]) tempResource.getContent();
                    Set<String> nonLinkPaths = new HashSet<String>();
                    try {
                        // We need to set inExecution to false, in order to run the resourceExists
                        // operation. Since inExecution is ThreadLocal, there is no harm in doing
                        // this, as long as you set this to true after completing the operation.
                        setInExecution(false);
                        
                        if(paths != null) {
                            for (int i = 0; i < paths.length; i++) {
                                if (subPath != null && subPath.length() != 0) {
                                    paths[i] = this.mountPoint + paths[i].substring(subPath.length());
                                } else {
                                    paths[i] = this.mountPoint + paths[i];
                                }
                                
                                HandlerContext childContext = new HandlerContext(requestContext.getRepository());
                                childContext.setResourcePath(new ResourcePath(paths[i]));
                                
                                if (resourceExists(childContext, false)) {
                                    nonLinkPaths.add(paths[i]);
                                }
                            }
                        }
                    } finally {
                        setInExecution(true);
                    }
                    
                    ((ResourceImpl) tempResource).setContentWithNoUpdate(nonLinkPaths.toArray(new String[nonLinkPaths.size()]));
                }
                
                ((ResourceImpl) tempResource).setPath(fullPath);
                ((ResourceImpl) tempResource).setAuthorUserName(author);
                ((ResourceImpl) tempResource).setTenantId(CurrentContext.getCallerTenantId());
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Could not mount remote instance " + this.conURL +
                            ". Resource doesn't exist at " + actualPath + ".");
                }
                
                setInExecution(false);
                throw new RepositoryResourceNotFoundException(fullPath, RepositoryErrorCodes.INVALID_OR_MALFORMED_URL);
            }
        } catch (RepositoryResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Could not mount remote instance " + this.conURL + " " + e.getMessage());
            throw new RepositoryResourceNotFoundException(fullPath, e, RepositoryErrorCodes.INVALID_OR_MALFORMED_URL);
        } finally {
            setInExecution(false);
        }

        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_LINK, "true");
        ((ResourceImpl) tempResource).removePropertyWithNoUpdate(RepositoryConstants.REGISTRY_NON_RECURSIVE);
        ((ResourceImpl) tempResource).removePropertyWithNoUpdate(/*RepositoryConstants.*/ InternalConstants.REGISTRY_LINK_RESTORATION);
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_MOUNT, "true");
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_AUTHOR, author);
        
        if (remote) {
            ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_USER, this.userName);
        } else {
            ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_USER, CurrentContext.getUser());
        }
        
        ((ResourceImpl) tempResource).addPropertyWithNoUpdate(RepositoryConstants.REGISTRY_REAL_PATH, this.conURL + "/resourceContent?path=" + actualPath);
        requestContext.setProcessingComplete(true);
        setInExecution(false);
        return tempResource;
    }

    public void delete(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            super.delete(requestContext);
            return;
        } else {
            setInExecution(true);
        }
        
        String fullPath = requestContext.getResourcePath().getPath();
        RepositoryService registryService = requestContext.getRepository().getRepositoryService();
        
        if (fullPath.equals(this.mountPoint)) {
            requestContext.getRepository().removeLink(requestContext.getResource().getPath());
            if (registryService != null) {
            	registryService.removeHandler(this, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
            }
        } else {
            String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
            if (subPath != null) {
                actualPath = subPath + actualPath;
            }
            if (actualPath.length() == 0) {
                actualPath = "/";
            }
            try {
                Repository remoteRegistry = getRepository(requestContext);
                beginNestedOperation(actualPath, fullPath);
                try {
                    remoteRegistry.delete(actualPath);
                } finally {
                    endNestedOperation();
                }
            } catch (Exception e) {
                String msg = "Could not delete the remote resource.";
                log.error(msg);
                setInExecution(false);
                throw new RepositoryServerContentException(msg, e);
            }
        }
        
        requestContext.setProcessingComplete(true);
        setInExecution(false);
    }

	public String rename(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            return super.rename(requestContext);
        } else {
            setInExecution(true);
        }
        
        String fullResourcePath = requestContext.getSourcePath();
        String fullTargetPath = requestContext.getInstanceId();

        RepositoryService registryService = requestContext.getRepository().getRepositoryService();

        String newPath;
        
        if (fullResourcePath.equals(this.mountPoint)) {
            newPath = ((EmbeddedRepository) (requestContext.getRepository())).getRepository().rename(new ResourcePath(fullResourcePath), fullTargetPath);
            requestContext.getRepository().createLink(fullTargetPath, this.id);
            requestContext.getRepository().removeLink(this.mountPoint);
            
            if (registryService != null) {
            	registryService.removeHandler(this, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
            }
        } else {
            String actualResourcePath = fullResourcePath.substring(this.mountPoint.length(), fullResourcePath.length());
            String actualTargetPath = fullTargetPath.substring(this.mountPoint.length(), fullTargetPath.length());
            
            if (subPath != null) {
                actualResourcePath = subPath + actualResourcePath;
                actualTargetPath = subPath + actualTargetPath;
            }
            
            if (actualResourcePath.length() == 0) {
                actualResourcePath = "/";
            }
            
            if (actualTargetPath.length() == 0) {
                actualTargetPath = "/";
            }

            try {
                Repository remoteRegistry = getRepository(requestContext);
                Map<String, String> pathMap = new HashMap<String, String>();
                pathMap.put(actualResourcePath, fullResourcePath);
                pathMap.put(actualTargetPath, fullTargetPath);
                beginNestedOperation(pathMap);
                
                try {
                    newPath = remoteRegistry.rename(actualResourcePath, actualTargetPath);
                } finally {
                    endNestedOperation();
                }
            } catch (Exception e) {
                String msg = "Could not rename the remote resource.";
                log.error(msg);
                setInExecution(false);
                throw new RepositoryServerContentException(msg, e);
            }
        }
        
        requestContext.setProcessingComplete(true);
        setInExecution(false);
        
        return newPath;
    }

    public String move(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            return super.move(requestContext);
        } else {
            setInExecution(true);
        }
        
        String fullResourcePath = requestContext.getSourcePath();
        String fullTargetPath = requestContext.getInstanceId();
        
        RepositoryService registryService = requestContext.getRepository().getRepositoryService();

        try {
            if (fullResourcePath.equals(this.mountPoint)) {
                ((EmbeddedRepository) requestContext.getRepository()).getRepository().move(new ResourcePath(fullResourcePath), fullTargetPath);
                requestContext.getRepository().createLink(fullTargetPath, this.id);
                requestContext.getRepository().removeLink(this.mountPoint);
                
                if (registryService != null) {
                	registryService.removeHandler(this, HandlerLifecycleManager.TENANT_SPECIFIC_SYSTEM_HANDLER_PHASE);
                }
            } else if (fullResourcePath.startsWith(this.mountPoint)) {
                String actualResourcePath = fullResourcePath.substring(this.mountPoint.length(), fullResourcePath.length());
                String actualTargetPath;
                
                if (subPath != null) {
                    actualResourcePath = subPath + actualResourcePath;
                }

                Repository remoteRegistry = getRepository(requestContext);
                if (fullTargetPath.startsWith(this.mountPoint)) {
                    // both the source and the target is in remote registry
                    actualTargetPath = fullTargetPath.substring(this.mountPoint.length(), fullTargetPath.length());
                    
                    if (subPath != null) {
                        actualTargetPath = subPath + actualTargetPath;
                    }
                    
                    Map<String, String> pathMap = new HashMap<String, String>();
                    pathMap.put(actualResourcePath, fullResourcePath);
                    pathMap.put(actualTargetPath, fullTargetPath);
                    beginNestedOperation(pathMap);
                    
                    try {
                        remoteRegistry.move(actualResourcePath, actualTargetPath);
                    } finally {
                        endNestedOperation();
                    }
                } else {
                    // source is in remote, target is in current registry
                    actualTargetPath = fullTargetPath;
                    Map<String, String> pathMap = new HashMap<String, String>();
                    pathMap.put(actualResourcePath, fullResourcePath);
                    pathMap.put(actualTargetPath, fullTargetPath);

                    StringWriter out = new StringWriter();
                    beginNestedOperation(pathMap);
                    
                    try {
                        remoteRegistry.dump(actualResourcePath, out);
                    } finally {
                        endNestedOperation();
                    }
                    
                    // we are converting the dump to the buffer, which may not cost effective..
                    Reader input = new StringReader(out.toString());
                    requestContext.getRepository().restore(actualTargetPath, input);
                    beginNestedOperation(pathMap);
                    
                    try {
                        remoteRegistry.delete(actualResourcePath);
                    } finally {
                        endNestedOperation();
                    }
                }
            } else if (fullTargetPath.contains(this.mountPoint)) {
                // source is the current registry, target the remote registry
                String actualTargetPath = fullTargetPath.substring(this.mountPoint.length(), fullTargetPath.length());
                String actualResourcePath;
                
                if (subPath != null) {
                    actualTargetPath = subPath + actualTargetPath;
                }
                
                Repository remoteRegistry = getRepository(requestContext);

                actualResourcePath = fullResourcePath;
                StringWriter out = new StringWriter();

                requestContext.getRepository().dump(actualResourcePath, out);
                Reader input = new StringReader(out.toString());
                Map<String, String> pathMap = new HashMap<String, String>();
                pathMap.put(actualResourcePath, fullResourcePath);
                pathMap.put(actualTargetPath, fullTargetPath);
                beginNestedOperation(pathMap);
                
                try {
                    remoteRegistry.restore(actualTargetPath, input);
                } finally {
                    endNestedOperation();
                }
                
                requestContext.getRepository().delete(actualResourcePath);
            }
        } catch (RuntimeException e) {
            String msg = "Could not move the remote resource " + fullResourcePath + ".";
            log.error(msg);
            setInExecution(false);
            throw new RepositoryServerContentException(msg, e);
        }
        requestContext.setProcessingComplete(true);
        setInExecution(false);
        return fullTargetPath;
    }

    public String copy(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            return super.copy(requestContext);
        } else {
            setInExecution(true);
        }
        String fullResourcePath = requestContext.getSourcePath();
        String fullTargetPath = requestContext.getInstanceId();

        try {
            if (fullResourcePath.equals(this.mountPoint)) {
                requestContext.getRepository().createLink(fullTargetPath, this.id);
            } else if (fullResourcePath.startsWith(this.mountPoint)) {
                String actualResourcePath = fullResourcePath.substring(
                        this.mountPoint.length(), fullResourcePath.length());
                String actualTargetPath;
                if (subPath != null) {
                    actualResourcePath = subPath + actualResourcePath;
                }

                Repository remoteRegistry = getRepository(requestContext);
                if (fullTargetPath.startsWith(this.mountPoint)) {
                    actualTargetPath = fullTargetPath.substring(
                            this.mountPoint.length(), fullTargetPath.length());
                    if (subPath != null) {
                        actualTargetPath = subPath + actualTargetPath;
                    }
                    Map<String, String> pathMap = new HashMap<String, String>();
                    pathMap.put(actualResourcePath, fullResourcePath);
                    pathMap.put(actualTargetPath, fullTargetPath);
                    beginNestedOperation(pathMap);
                    try {
                        remoteRegistry.copy(actualResourcePath, actualTargetPath);
                    } finally {
                        endNestedOperation();
                    }
                } else {
                    actualTargetPath = fullTargetPath;

                    StringWriter out = new StringWriter();
                    Map<String, String> pathMap = new HashMap<String, String>();
                    pathMap.put(actualResourcePath, fullResourcePath);
                    pathMap.put(actualTargetPath, fullTargetPath);
                    beginNestedOperation(pathMap);
                    try {
                        remoteRegistry.dump(actualResourcePath, out);
                    } finally {
                        endNestedOperation();
                    }
                    Reader input = new StringReader(out.toString());
                    requestContext.getRepository().restore(actualTargetPath, input);

                }
            } else if (fullTargetPath.contains(this.mountPoint)) {
                String actualTargetPath = fullTargetPath.substring(
                        this.mountPoint.length(), fullTargetPath.length());
                String actualResourcePath;
                if (subPath != null) {
                    actualTargetPath = subPath + actualTargetPath;
                }
                Repository remoteRegistry = getRepository(requestContext);

                actualResourcePath = fullResourcePath;
                StringWriter out = new StringWriter();


                requestContext.getRepository().dump(actualResourcePath, out);
                Reader input = new StringReader(out.toString());
                Map<String, String> pathMap = new HashMap<String, String>();
                pathMap.put(actualResourcePath, fullResourcePath);
                pathMap.put(actualTargetPath, fullTargetPath);
                beginNestedOperation(pathMap);
                try {
                    remoteRegistry.restore(actualTargetPath, input);
                } finally {
                    endNestedOperation();
                }
            }
        } catch (RuntimeException e) {
            String msg = "Could not copy the remote resource " + fullResourcePath + ".";
            log.error(msg);
            setInExecution(false);
            throw new RepositoryServerContentException(msg, e);
        }
        requestContext.setProcessingComplete(true);
        setInExecution(false);
        return fullTargetPath;
    }

//    public float getAverageRating(RequestContext requestContext) throws RepositoryException {
//        if (isInExecution()) {
//            return super.getAverageRating(requestContext);
//        } else {
//            setInExecution(true);
//        }
//        String fullPath = requestContext.getPath().getPath();
//        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
//        float rating;
//        if (subPath != null) {
//            actualPath = subPath + actualPath;
//        }
//        if (actualPath.length() == 0) {
//            actualPath = "/";
//        }
//        try {
//            Registry remoteRegistry = getRegistry(requestContext);
//            beginNestedOperation(actualPath, fullPath);
//            try {
//                rating = remoteRegistry.getAverageRating(actualPath);
//            } finally {
//                endNestedOperation();
//            }
//        } catch (Exception e) {
//            if (log.isWarnEnabled()) {
//                log.warn("Could not get average ratings from" + this.conURL);
//            }
//            log.debug("Caused by: ", e);
//            setInExecution(false);
//            return 0;
//        }
//        requestContext.setProcessingComplete(true);
//        setInExecution(false);
//        return rating;
//    }

//    public int getRating(RequestContext requestContext) throws RepositoryException {
//        if (isInExecution()) {
//            return super.getRating(requestContext);
//        } else {
//            setInExecution(true);
//        }
//        String fullPath = requestContext.getPath().getPath();
//        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
//        int rating;
//        if (subPath != null) {
//            actualPath = subPath + actualPath;
//        }
//        if (actualPath.length() == 0) {
//            actualPath = "/";
//        }
//        try {
//            Registry remoteRegistry = getRegistry(requestContext);
//            if (remote) {
//                rating = remoteRegistry.getRating(actualPath, this.userName);
//            } else {
//                beginNestedOperation(actualPath, fullPath);
//                try {
//                    rating = remoteRegistry.getRating(actualPath, CurrentSession.getUser());
//                } finally {
//                    endNestedOperation();
//                }
//            }
//        } catch (Exception e) {
//            if (log.isWarnEnabled()) {
//                log.warn("Could not get ratings from " + this.conURL);
//            }
//            log.debug("Caused by: ", e);
//            setInExecution(false);
//            return 0;
//        }
//        requestContext.setProcessingComplete(true);
//        setInExecution(false);
//        return rating;
//    }
//
//    public void rateResource(RequestContext requestContext) throws RepositoryException {
//        if (isInExecution()) {
//            super.rateResource(requestContext);
//            return;
//        } else {
//            setInExecution(true);
//        }
//        String fullPath = requestContext.getPath().getPath();
//        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
//        if (subPath != null) {
//            actualPath = subPath + actualPath;
//        }
//        if (actualPath.length() == 0) {
//            actualPath = "/";
//        }
//        if (remote) {
//            CurrentSession.setUser(this.userName);
//        }
//
//        try {
//            Registry remoteRegistry = getRegistry(requestContext);
//            beginNestedOperation(actualPath, fullPath);
//            try {
//                remoteRegistry.rateResource(actualPath, requestContext.getRating());
//            } finally {
//                endNestedOperation();
//            }
//            requestContext.setProcessingComplete(true);
//        } catch (Exception e) {
//            throw new RepositoryServerContentException("Unable to rate resource", e);
//        } finally {
//            setInExecution(false);
//        }
//    }

    @SuppressWarnings("unchecked")
    public Collection executeQuery(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            return super.executeQuery(requestContext);
        } else {
            setInExecution(true);
        }
        Collection collection = null;
        Resource query = requestContext.getResource();
        Map paramMap = new HashMap(requestContext.getQueryParameters());
        if (query != null) {
            // If query resource is being passed, use that to override the parameter map.
            paramMap.put("query", query.getContent());
            String resultType = query.getPropertyValue(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME);
            if (resultType != null) {
                paramMap.put(RepositoryConstants.RESULT_TYPE_PROPERTY_NAME, resultType);
            }
        }
        try {
            Repository remoteRegistry = getRepository(requestContext);
            beginNestedOperation(null);
            
            try {
                boolean connectionExists = false;
                List<String> conList = getQueriedConnectionList();
                
                if (conList == null) {
                    conList = new LinkedList<String>();
                }
                
                for (String con: conList) {
                    if (id != null && mountPoint != null && (id + mountPoint).equals(con)) {
                        connectionExists = true;
                        break;
                    }
                }
                
                if (!connectionExists) {
                    conList.add(id + mountPoint);
                    setQueriedConnectionList(conList);
                    paramMap.put("remote", "true");
                    collection = remoteRegistry.executeQuery(null, paramMap);
                }
            } finally {
                endNestedOperation();
            }
            // We are not interested in setting processing as completed since we want to enforce a
            // federated query across multiple mounts.
        } catch (Exception e) {
            String msg = "Could not execute query in remote mount at " + this.conURL;
            
            if (log.isWarnEnabled()) {
                log.warn(msg);
            }
            
            throw new RepositoryDBException(msg, e);
        } finally {
            setInExecution(false);
        }
        
        List<String> results = new LinkedList<String>();
        
        if (collection != null) {
            String[] children = collection.getChildPaths();
            
            if (children != null) {
                for (String child : children) {
                    if (child != null) {
                        if (subPath != null && subPath.length() != 0 && child.startsWith(subPath)) {
                                child = this.mountPoint + child.substring(subPath.length());
                        }
                        // In this case the '/' of the remote instance has been mounted. We are
                        // unable to exactly figure out whether the path was added locally or
                        // remotely, since both the cases would look similar. We therefor assume
                        // that the associations have been added locally (via the remote link), and
                        // treat it as such.
                        results.add(child);
                    }
                }
            }
            
            collection.setContent(results.toArray(new String[results.size()]));
        }
        
        return collection;
    }

    public void importResource(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            super.importResource(requestContext);
            return;
        } else {
            setInExecution(true);
        }
        
        String fullPath = requestContext.getResourcePath().getPath();
        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());

        if (subPath != null) {
            actualPath = subPath + actualPath;
        }
        
        if (actualPath.length() == 0) {
            actualPath = "/";
        }
        
        try {
            Repository remoteRegistry = getRepository(requestContext);
            beginNestedOperation(actualPath, fullPath);
            
            try {
                remoteRegistry.importResource(actualPath, requestContext.getSourceURL(),
                        requestContext.getResource());
            } finally {
                endNestedOperation();
            }
            
            requestContext.setProcessingComplete(true);
        } catch (Exception e) {
            String msg = "Could not import resource from " + requestContext.getSourceURL();
            if (log.isWarnEnabled()) {
                log.warn(msg);
            }
            log.debug("Caused by: ", e);
        } finally {
            setInExecution(false);
        }
    }

    public void dump(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            super.dump(requestContext);
            return;
        } else {
            setInExecution(true);
        }
        
        String fullPath = requestContext.getResourcePath().getPath();

        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
        
        if (subPath != null) {
            actualPath = subPath + actualPath;
        }
        
        if (actualPath.length() == 0) {
            actualPath = "/";
        }
        
        try {
            Repository remoteRegistry = getRepository(requestContext);
            beginNestedOperation(actualPath, fullPath);
            
            try {
                remoteRegistry.dump(actualPath, requestContext.getDumpingWriter());
            } finally {
                endNestedOperation();
            }
            
            requestContext.setProcessingComplete(true);
        } catch (Exception e) {
            throw new RepositoryServerContentException("Unable to dump content", e);
        } finally {
            setInExecution(false);
        }
    }


    public void restore(HandlerContext requestContext) throws RepositoryException {
        if (isInExecution()) {
            super.restore(requestContext);
            return;
        } else {
            setInExecution(true);
        }
        
        String fullPath = requestContext.getResourcePath().getPath();
        String actualPath = fullPath.substring(this.mountPoint.length(), fullPath.length());
        
        if (subPath != null) {
            actualPath = subPath + actualPath;
        }
        
        if (actualPath.length() == 0) {
            actualPath = "/";
        }
        
        try {
            Repository remoteRegistry = getRepository(requestContext);
            beginNestedOperation(actualPath, fullPath);
            try {
                remoteRegistry.restore(actualPath, requestContext.getDumpingReader());
            } finally {
                endNestedOperation();
            }
            requestContext.setProcessingComplete(true);
        } catch (Exception e) {
            throw new RepositoryServerContentException("Unable to restore content", e);
        } finally {
            setInExecution(false);
        }
    }

    /**
     * Method to set instance identifier
     *
     * @param id instance identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Method to set connection URL
     *
     * @param conURL connection URL
     */
    public void setConURL(String conURL) {
        if (conURL.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
            this.conURL = conURL.substring(0, conURL.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) - 1);
        }
        
        this.conURL = conURL;
    }

    /**
     * Method to set mount point
     *
     * @param mountPoint the mount point
     */
    public void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
    }

    /**
     * Method to set username
     *
     * @param username the username.
     */
    public void setUserName(String username) {
        this.userName = username;
    }

    /**
     * Method to set password
     *
     * @param password the password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Method to set type of remote registry
     *
     * @param registryType the type of remote registry.
     */
    public void setRegistryType(String registryType) {
        this.registryType = registryType;
    }

    /**
     * Method to set target sub path
     *
     * @param subPath target sub path.
     */
    public void setSubPath(String subPath) {
        if (subPath.equals(RepositoryConstants.ROOT_PATH)) {
            // skip just the ROOT PATH
            this.subPath = "";
        } else if (subPath.length() > 1 && subPath.charAt(subPath.length() - 1) == RepositoryConstants.PATH_SEPARATOR.charAt(0)) {
            // skip the PATH separator at the end
            this.subPath = subPath.substring(0, subPath.length() - 1);
        } else {
            this.subPath = subPath;
        }
    }

    /**
     * Method to set the author
     *
     * @param author the author.
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Method to set whether this mount is remote (atom-based) or not
     *
     * @param remote whether remote or not.
     */
    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    /**
     * Method to obtain the database configuration
     *
     * @return the database configuration.
     */
    public String getDbConfig() {
        return dbConfig;
    }

    /**
     * Method to set the database configuration
     *
     * @param dbConfig the database configuration.
     */
    public void setDbConfig(String dbConfig) {
        this.dbConfig = dbConfig;
    }

    /**
     * Method to obtain whether in read-only mode or not.
     *
     * @return whether in read-only mode or not.
     */
    public boolean getReadOnly() {
        return readOnly;
    }

    /**
     * Method to set whether caching is enabled or not.
     *
     * @param cacheEnabled whether caching is enabled or not.
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * Method to obtain whether caching is enabled or not.
     *
     * @return whether caching is enabled or not.
     */
    public boolean getCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Method to set whether in read-only mode or not.
     *
     * @param readOnly whether in read-only mode or not.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Method to obtain the registry root.
     *
     * @return the registry root.
     */
    public String getRegistryRoot() {
        return repositoryRoot;
    }

    /**
     * Method to set the registry root.
     *
     * @param repositoryRoot the registry root.
     */
    public void setRepositoryRoot(String repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }
}
