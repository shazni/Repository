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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.registry.core.CommentImpl;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.dao.CommentsDAO;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.registry.core.utils.AuthorizationUtils;
import org.wso2.carbon.registry.core.utils.InternalUtils;
import org.wso2.carbon.repository.ActionConstants;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.ResourcePath;
import org.wso2.carbon.repository.config.StaticConfiguration;
import org.wso2.carbon.repository.exceptions.RepositoryAuthException;
import org.wso2.carbon.repository.exceptions.RepositoryErrorCodes;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.exceptions.RepositoryResourceNotFoundException;
import org.wso2.carbon.repository.handlers.Handler;
import org.wso2.carbon.repository.handlers.RequestContext;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;

/**
 * Handles paths of the form <b>pure resource path</b>;comments:<b>comment ID</b> e.g.
 * /projects/ids/config.xml;comments:2
 */
@Deprecated
public class CommentURLHandler extends Handler {

    private static final Log log = LogFactory.getLog(CommentURLHandler.class);

    public Resource get(RequestContext requestContext) throws RepositoryException {
//        RegistryContext registryContext = requestContext.getRegistryContext();

    	Registry registry = requestContext.getRegistry() ;	
    	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
//    	RegistryContext registryContext = ((EmbeddedRegistry) (requestContext.getRegistry())).getRegistryContext();
        if (registryContext == null) {
            registryContext = RegistryContext.getBaseInstance();
        }
        CommentsDAO commentsDAO = registryContext.getDataAccessManager().getDAOManager().
                getCommentsDAO(StaticConfiguration.isVersioningComments());
        ResourcePath resourcePath = requestContext.getResourcePath();

        String commentID = resourcePath.getParameterValue("comments");
        if (commentID != null) {

            long cID;
            try {
                cID = Long.parseLong(commentID);

            } catch (NumberFormatException e) {
                // note that this might NOT be an exceptional scenario. there could be a different
                // URL form, which contains strings after "comment".
                // it is just that it is not the URL we expect here
                return null;
            }

            CommentImpl comment = commentsDAO.getComment(cID, resourcePath.getPath());

            if (comment == null) {
                String msg = "Requested comment with ID: " + cID + " is not available.";
                log.error(msg);
                throw new RepositoryResourceNotFoundException(msg, RepositoryErrorCodes.RESOURCE_UNAVAILABLE);
            }

            requestContext.setProcessingComplete(true);
            return comment;

        }

        return null;
    }

    public void delete(RequestContext requestContext) throws RepositoryException {
//        RegistryContext registryContext = requestContext.getRegistryContext();
    	
    	Registry registry = requestContext.getRegistry() ;
    	
    	RegistryContext registryContext = InternalUtils.getRegistryContext(registry);
    	
//    	RegistryContext registryContext = ((EmbeddedRegistry) (requestContext.getRegistry())).getRegistryContext();
        if (registryContext == null) {
            registryContext = RegistryContext.getBaseInstance();
        }
        CommentsDAO commentsDAO = registryContext.getDataAccessManager().getDAOManager().
                getCommentsDAO(StaticConfiguration.isVersioningComments());
        requestContext.setProcessingComplete(false);
        ResourcePath resourcePath = requestContext.getResourcePath();

        String commentID = resourcePath.getParameterValue("comments");
        if (commentID != null) {

            long cID;
            try {
                cID = Long.parseLong(commentID);

            } catch (NumberFormatException e) {
                // note that this might not be an exceptional scenario. there could be a different
                // URL form, which contains strings after "comment".
                // it is just that it is not the URL we expect here
                return;
            }

            String userID = CurrentSession.getUser();
            String authorizationPath =
                    AuthorizationUtils.getAuthorizationPath(resourcePath.getPath());
            String commentAuthor;

            CommentImpl comment = commentsDAO.getComment(cID, resourcePath.getPath());
            commentAuthor = comment.getUser();

            // check if the current user has permission to delete this comment.
            // users who have PUT permission on the commented resource can delete any comment on
            // that resource. Any user can delete his own comment.

            try {
                UserRealm realm = CurrentSession.getUserRealm();

                if (!userID.equals(commentAuthor) &&
                        !realm.getAuthorizationManager().isUserAuthorized(userID, authorizationPath,
                                ActionConstants.PUT)) {

                    String msg = "User: " + userID +
                            " is not authorized to delete the comment on the resource: " +
                            authorizationPath;
                    log.warn(msg);
                    throw new RepositoryAuthException(msg, RepositoryErrorCodes.USER_NOT_AUTHORISED);
                }

            } catch (UserStoreException e) {
                //
            }

            commentsDAO.deleteComment(cID);

            requestContext.setProcessingComplete(true);
        }
    }
}
