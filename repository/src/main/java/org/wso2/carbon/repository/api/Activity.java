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

import java.util.Date;

/**
 * Representation of an activity that has been performed on the repository. For example, a put
 * operation will lead to activity of adding a resource into the repository, or updating an existing
 * resource in the repository. Each activity performed in the repository will have a corresponding log
 * entry, which is a record of a single action performed on the repository.
 */
public class Activity {

    /**
     * The log action to filter with. All is for don't filter at all.
     */
    public static final int ALL = -1;

    /**
     * Filter value for the resource adding action.
     */
    public static final int ADD = 0;

    /**
     * Filter value for the resource updating action.
     */
    public static final int UPDATE = 1;

    /**
     * Filter value for the resource deleting action.
     */
    public static final int DELETE_RESOURCE = 7;

    /**
     * Filter value for the resource restoring action.
     */
    public static final int RESTORE = 8;

    /**
     * Filter value for the resource renaming action.
     */
    public static final int RENAME = 9;

    /**
     * Filter value for the resource moving action.
     */
    public static final int MOVE = 10;

    /**
     * Filter value for the resource copying action.
     */
    public static final int COPY = 11;

    /**
     * Filter value for the create remote link action.
     */
    public static final int CREATE_REMOTE_LINK = 12;

    /**
     * Filter value for the create symbolic link action.
     */
    public static final int CREATE_SYMBOLIC_LINK = 13;

    /**
     * Filter value for the removing link action.
     */
    public static final int REMOVE_LINK = 14;

    /**
     * Path of the resource on which the action is performed.
     */
    private String resourcePath;

    /**
     * User who has performed the action.
     */
    private String userName;

    /**
     * Date and time at which the action is performed.
     */
    private long date;

    /**
     * Name of the actions. e.g. put, get
     */
    private int action;

    /**
     * Additional data to describe the actions. This depends on the action.
     */
    private String actionData;

    /**
     * Get the resource path of the log entry.
     *
     * @return the resource path
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * Set the resource path to the log entry.
     *
     * @param resourcePath the resource path.
     */
    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
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
     * Set the user name the action is logged with.
     *
     * @param userName the user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
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
     * Method to set the date.
     *
     * @param date the date object
     */
    public void setDate(Date date) {
        this.date = date.getTime();
    }

    /**
     * Method to get the action.
     *
     * @return the action.
     */
    public int getAction() {
        return action;
    }

    /**
     * Method to set the action.
     *
     * @param action the action.
     */
    public void setAction(int action) {
        this.action = action;
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
     * Method to set the action data.
     *
     * @param actionData the additional data
     */
    public void setActionData(String actionData) {
        this.actionData = actionData;
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
        
        entryBuf.append(getResourcePath());
        
        return entryBuf.toString();
    }

    /**
     * Method to set the text for the log entry.
     *
     * @return the text of the log entry
     */
    public String getText() {
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
        
        entryBuf.append(getResourcePath());
        entryBuf.append(" on ");
        entryBuf.append(getDate().toString());
        entryBuf.append(".");
        
        return entryBuf.toString();
    }
}