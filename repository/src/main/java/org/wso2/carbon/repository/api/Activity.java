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

package org.wso2.carbon.repository.api;

import org.wso2.carbon.repository.api.utils.Actions;

import java.util.Date;

/**
 * Representation of an activity that has been performed on the repository. For example, a put
 * operation will lead to activity of adding a resource into the repository, or updating an existing
 * resource in the repository. Each activity performed in the repository will have a corresponding log
 * entry, which is a record of a single action performed on the repository.
 */
public abstract class Activity {

    /**
     * Path of the resource on which the action is performed.
     */
    protected String resourcePath;

    /**
     * User who has performed the action.
     */
    protected String userName;

    /**
     * Date and time at which the action is performed.
     */
    protected long date;

    /**
     * Name of the actions. e.g. put, get
     */
    protected Actions action;

    /**
     * Additional data to describe the actions. This depends on the action.
     */
    protected String actionData;

    /**
     * Get the resource path of the log entry.
     *
     * @return the resource path
     */
    public String getPath() {
        return resourcePath;
    }

    /**
     * Method to get the user name the action is logged with.
     *
     * @return the user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Method to get the date.
     *
     * @return the date
     */
    public Date getDate() {
        return new Date(date);
    }

    /**
     * Method to get the action.
     *
     * @return the action.
     */
    public Actions getAction() {
        return action;
    }

    /**
     * Method to get the action data.
     *
     * @return the action data
     */
    public String getActionData() {
        return actionData;
    }

    /**
     * Method to get the title of the log entry.
     *
     * @return the title
     */
    public String getTitle() {
        StringBuffer entryBuf = new StringBuffer();
        
        switch (getAction()) {
            case UPDATE:
                entryBuf.append("Update of ");
                break;
            default:
            	entryBuf.append("Operation of ");
            	break;
        }
        
        entryBuf.append(getPath());
        
        return entryBuf.toString();
    }

    /**
     * Method to set the text for the log entry.
     *
     * @return the text of the log entry
     */
    public String toString() {
        StringBuffer entryBuf = new StringBuffer();
        entryBuf.append(getUserName());
        
        switch (getAction()) {
            case ADD:
                entryBuf.append(" added the resource ");
                break;
            case UPDATE:
                entryBuf.append(" updated the resource '");
                break;
            case RENAME:
                entryBuf.append(" renamed the resource ");
                break;
            case RESTORE:
                entryBuf.append(" restored the resource ");
                break;
            case COPY:
                entryBuf.append(" copied the resource ");
                break;
            case MOVE:
                entryBuf.append(" moved the resource ");
                break;
            case CREATE_REMOTE_LINK:
                entryBuf.append(" created a remote link ");
                break;
            case CREATE_SYMBOLIC_LINK:
                entryBuf.append(" created a symbolic link ");
                break;
            case REMOVE_LINK:
                entryBuf.append(" removed link ");
                break;
            default:
        }
        
        entryBuf.append(getPath());
        entryBuf.append(" on ");
        entryBuf.append(getDate().toString());
        entryBuf.append(".");
        
        return entryBuf.toString();
    }
}