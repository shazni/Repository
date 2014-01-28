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

package org.wso2.carbon.registry.core.jdbc.handlers.builtin;

import java.util.Date;

import org.wso2.carbon.registry.core.ResourceImpl;
import org.wso2.carbon.registry.core.caching.CacheBackedRegistry;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.dao.RatingsDAO;
import org.wso2.carbon.registry.core.dao.ResourceDAO;
import org.wso2.carbon.registry.core.jdbc.EmbeddedRegistry;
import org.wso2.carbon.registry.core.jdbc.dataobjects.RatingDO;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.RepositoryConstants;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.ResourcePath;
import org.wso2.carbon.repository.config.StaticConfiguration;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.Handler;
import org.wso2.carbon.repository.handlers.RequestContext;

/**
 * Handles paths of the form <b>pure resource path</b>;ratings:<b>username</b> e.g.
 * /projects/ids/config.xml;ratings:foo
 */
public class RatingURLHandler extends Handler {

    public Resource get(RequestContext requestContext) throws RepositoryException {
//        RegistryContext registryContext = requestContext.getRegistryContext();
    	
    	
    	Registry registry = requestContext.getRegistry() ;
    	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
    	
//    	RegistryContext registryContext = ((EmbeddedRegistry) requestContext.getRegistry()).getRegistryContext();
        if (registryContext == null) {
            registryContext = RegistryContext.getBaseInstance();
        }
        ResourceDAO resourceDAO = registryContext.getDataAccessManager().getDAOManager().
                getResourceDAO();
        RatingsDAO ratingsDAO = registryContext.getDataAccessManager().getDAOManager().
                getRatingsDAO(StaticConfiguration.isVersioningRatings());

        ResourcePath resourcePath = requestContext.getResourcePath();

        String ratedUserName = resourcePath.getParameterValue("ratings");
        if (ratedUserName != null) {
            ResourceImpl resourceImpl = resourceDAO.getResourceMetaData(resourcePath.getPath());

            RatingDO ratingDO = ratingsDAO.getRatingDO(resourceImpl, ratedUserName);
            int rating = ratingDO.getRating();
            Date ratedTime = ratingDO.getRatedTime();

            ResourceImpl resource = new ResourceImpl();
            resource.setMediaType(RepositoryConstants.RATING_MEDIA_TYPE);
            resource.setContent(rating);
            resource.setAuthorUserName(ratedUserName);
            resource.setPath(resourcePath.getCompletePath());
            if (ratedTime != null) {
                resource.setCreatedTime(ratedTime);
                resource.setLastModified(ratedTime);
            }
            resource.addProperty("resourcePath", resourcePath.getPath());

            return resource;
        }

        return null;
    }
}
