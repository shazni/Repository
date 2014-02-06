
/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.registry.core.ghostregistry;

import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.CommentImpl;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RepositoryServerException;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.Activity;
import org.wso2.carbon.repository.Aspect;
import org.wso2.carbon.repository.Association;
import org.wso2.carbon.repository.Collection;
import org.wso2.carbon.repository.Comment;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.RegistryService;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.Tag;
import org.wso2.carbon.repository.TaggedResourcePath;
import org.wso2.carbon.repository.exceptions.RepositoryException;

/**
 * This implements the Ghost lazy loading pattern for the Registry. An actual registry instance will
 * not be created until first access.
 */
@Deprecated
public class GhostRegistry implements Registry {

    private static Log log = LogFactory.getLog(GhostRegistry.class);
    
    private RegistryService registryService;
    private int             tenantId;
    private RegistryType    registryType;

    private Registry registry;

    public GhostRegistry(RegistryService registryService, int tenantId, RegistryType registryType){
        this.registryService = registryService;
        this.tenantId = tenantId;
        this.registryType = registryType;
    }

    private Registry getRegistry() throws RepositoryException {
        if (registry != null) {
            return registry;
        }
        try {
            switch (registryType) {
                case SYSTEM_GOVERNANCE:
                    registry = (Registry) registryService.getGovernanceSystemRegistry(tenantId);
                    break;
                case SYSTEM_CONFIGURATION:
                    registry = (Registry) registryService.getConfigSystemRegistry(tenantId);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid registry type " + registryType);
            }
        } catch (org.wso2.carbon.repository.exceptions.RepositoryException e) {
            throw new RepositoryServerException(e.getMessage(), e);
        }
        return registry;
    }

    public Resource getMetaData(String path) throws RepositoryException {
        return getRegistry().getMetaData(path);
    }

    public String importResource(String suggestedPath, String sourceURL,
                                 Resource resource) throws RepositoryException {
        return getRegistry().importResource(suggestedPath, sourceURL, resource);
    }

    public String rename(String currentPath, String newName) throws RepositoryException {
        return getRegistry().rename(currentPath, newName);
    }

    public String move(String currentPath, String newPath) throws RepositoryException {
        return getRegistry().move(currentPath, newPath);
    }

    public String copy(String sourcePath, String targetPath) throws RepositoryException {
        return getRegistry().copy(sourcePath, targetPath);
    }

    public void createVersion(String path) throws RepositoryException {
        getRegistry().createVersion(path);
    }

    public String[] getVersions(String path) throws RepositoryException {
        return getRegistry().getVersions(path);
    }

    public void restoreVersion(String versionPath) throws RepositoryException {
        getRegistry().restoreVersion(versionPath);
    }

    public void addAssociation(String sourcePath, String targetPath,
                               String associationType) throws RepositoryException {
        getRegistry().addAssociation(sourcePath, targetPath, associationType);
    }

    public void removeAssociation(String sourcePath, String targetPath,
                                  String associationType) throws RepositoryException {
        getRegistry().removeAssociation(sourcePath, targetPath, associationType);
    }

    public Association[] getAllAssociations(String resourcePath) throws RepositoryException {
        return getRegistry().getAllAssociations(resourcePath);
    }

    public Association[] getAssociations(String resourcePath,
                                         String associationType) throws RepositoryException {
        return getRegistry().getAssociations(resourcePath, associationType);
    }

    public void applyTag(String resourcePath, String tag) throws RepositoryException {
        getRegistry().applyTag(resourcePath, tag);
    }

    public TaggedResourcePath[] getResourcePathsWithTag(String tag) throws RepositoryException {
        return getRegistry().getResourcePathsWithTag(tag);
    }

    public Tag[] getTags(String resourcePath) throws RepositoryException {
        return getRegistry().getTags(resourcePath);
    }

    public void removeTag(String path, String tag) throws RepositoryException {
        getRegistry().removeTag(path, tag);
    }

    public String addComment(String resourcePath, CommentImpl comment) throws RepositoryException {
        return getRegistry().addComment(resourcePath, comment);
    }

    public void editComment(String commentPath, String text) throws RepositoryException {
        getRegistry().editComment(commentPath, text);
    }

    public void removeComment(String commentPath) throws RepositoryException {
        getRegistry().removeComment(commentPath);
    }

    public Comment[] getComments(String resourcePath) throws RepositoryException {
        return getRegistry().getComments(resourcePath);
    }

    public void rateResource(String resourcePath, int rating) throws RepositoryException {
        getRegistry().rateResource(resourcePath, rating);
    }

    public float getAverageRating(String resourcePath) throws RepositoryException {
        return getRegistry().getAverageRating(resourcePath);
    }

    public int getRating(String path, String userName) throws RepositoryException {
        return getRegistry().getRating(path, userName);
    }
    

    public Collection executeQuery(String path, Map parameters) throws RepositoryException {
        return getRegistry().executeQuery(path, parameters);
    }

    public String[] getAvailableAspects() {
        try {
            return getRegistry().getAvailableAspects();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public void associateAspect(String resourcePath, String aspect) throws RepositoryException {
        getRegistry().associateAspect(resourcePath, aspect);
    }

    public void invokeAspect(String resourcePath, String aspectName,
                             String action) throws RepositoryException {
        getRegistry().invokeAspect(resourcePath, aspectName, action);
    }

    public String[] getAspectActions(String resourcePath,
                                     String aspectName) throws RepositoryException {
        return getRegistry().getAspectActions(resourcePath, aspectName);
    }

    public Collection searchContent(String keywords) throws RepositoryException {
        return getRegistry().searchContent(keywords);
    }

    public void createLink(String path, String target) throws RepositoryException {
        getRegistry().createLink(path, target);
    }

    public void createLink(String path, String target,
                           String subTargetPath) throws RepositoryException {
        getRegistry().createLink(path, target, subTargetPath);
    }

    public void removeLink(String path) throws RepositoryException {
        getRegistry().removeLink(path);
    }

    public void restore(String path, Reader reader) throws RepositoryException {
        getRegistry().restore(path, reader);
    }

    public void dump(String path, Writer writer) throws RepositoryException {
        getRegistry().dump(path, writer);
    }

    public String getEventingServiceURL(String path) throws RepositoryException {
        return getRegistry().getEventingServiceURL(path);
    }

    public void setEventingServiceURL(String path,
                                      String eventingServiceURL) throws RepositoryException {
        getRegistry().setEventingServiceURL(path, eventingServiceURL);
    }

    public Resource newResource() throws RepositoryException {
        return getRegistry().newResource();
    }

    public Collection newCollection() throws RepositoryException {
        return getRegistry().newCollection();
    }

    public Resource get(String path) throws RepositoryException {
        return getRegistry().get(path);
    }

    public Collection get(String path, int start, int pageSize) throws RepositoryException {
        return getRegistry().get(path, start, pageSize);
    }

    public boolean resourceExists(String path) throws RepositoryException {
        return getRegistry().resourceExists(path);
    }

    public void delete(String path) throws RepositoryException {
        getRegistry().delete(path);
    }

    @Override
    public String put(String suggestedPath, Resource resource) throws RepositoryException {
        return getRegistry().put(suggestedPath, resource);
    }

//    @Override
//    public String put(String suggestedPath, org.wso2.carbon.repository.api.Resource resource) 
//            throws org.wso2.carbon.repository.api.exceptions.RepositoryException {
//        return getRegistry().put(suggestedPath, resource);
//    }

    @Override
    public void beginTransaction() throws RepositoryException {
        getRegistry().beginTransaction();
    }

    @Override
    public void commitTransaction() throws RepositoryException {
        getRegistry().commitTransaction();
    }

    @Override
    public void rollbackTransaction() throws RepositoryException {
        getRegistry().rollbackTransaction();    
    }

    @Override
    public String addComment(String resourcePath, org.wso2.carbon.repository.Comment comment)
            throws org.wso2.carbon.repository.exceptions.RepositoryException {
        return getRegistry().addComment(resourcePath, comment);
    }

//    @Override
//    public String importResource(String suggestedPath, String sourceURL, 
//                                 org.wso2.carbon.repository.api.Resource resource)
//                                 throws org.wso2.carbon.repository.api.exceptions.RepositoryException {
//        return getRegistry().importResource(suggestedPath, sourceURL, resource);
//    }

    @Override
    public Activity[] getLogs(String resourcePath, int action, String userName,
                              Date from, Date to, boolean recentFirst) throws RepositoryException {
        return getRegistry().getLogs(resourcePath, action, userName, from, to, recentFirst);
    }

//    
//    @Deprecated
//    public LogEntryCollection getLogCollection(String resourcePath, int action,
//                                               String userName, Date from, Date to, boolean recentFirst)
//                                               throws RepositoryException {
//        return getRegistry().getLogCollection(resourcePath, action, userName, from, to, recentFirst);
//    }

    @Override
    public void invokeAspect(String resourcePath, String aspectName,
                             String action, Map<String, String> parameters) throws RepositoryException {
        getRegistry().invokeAspect(resourcePath, aspectName, action, parameters);
    }

/*  Aspect stuff is mostly removed   
    @Override
    public boolean removeAspect(String aspect) throws RepositoryException {
        return getRegistry().removeAspect(aspect);
    }

    @Override
    public boolean addAspect(String name, Aspect aspect) throws RepositoryException {
        return getRegistry().addAspect(name, aspect);
    }
    */

    @Override
    public boolean removeVersionHistory(String path, long snapshotId) throws RepositoryException {
        return getRegistry().removeVersionHistory(path, snapshotId);
    }

    public RegistryContext getRegistryContext() {
        try {
        	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
            return registryContext;
        } catch (Exception e) {
            log.error("Error getting Registry", e);
        }
        return null;
    }

	@Override
	public RegistryService getRegistryService() {
		return registryService;
	}

	@Override
	public String getResourceMediaTypes() throws RepositoryException {
		return getRegistry().getResourceMediaTypes() ;
	}

	@Override
	public void setResourceMediaTypes(String resourceMediaTypes) throws RepositoryException {
		getRegistry().setResourceMediaTypes(resourceMediaTypes);		
	}
	
	@Override
	public String getCollectionMediaTypes() throws RepositoryException {
		return getRegistry().getCollectionMediaTypes() ;
	}

	@Override
	public void setCollectionMediaTypes(String collectionMediaTypes) throws RepositoryException {
		getRegistry().setCollectionMediaTypes(collectionMediaTypes);	
	}
	
    public String getCustomUIMediaTypes() throws RepositoryException {
    	return registry.getCustomUIMediaTypes();
    }


    public void setCustomUIMediaTypes(String customUIMediaTypes) throws RepositoryException {
    	registry.setCustomUIMediaTypes(customUIMediaTypes);
    }

	@Override
	public boolean addAspect(String name, Aspect aspect)
			throws RepositoryException {
		return registry.addAspect(name, aspect);
	}

	@Override
	public boolean removeAspect(String name) throws RepositoryException {
		return registry.removeAspect(name);
	}

}