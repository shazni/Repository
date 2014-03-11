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

package org.wso2.carbon.repository.spi;

import org.wso2.carbon.repository.api.Activity;
import org.wso2.carbon.repository.api.utils.Actions;

import java.util.Date;

public class ResourceActivity extends Activity {
    /**
     * Method to set the action.
     *
     * @param action the action.
     */
    public void setAction(Actions action) {
        this.action = action;
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
     * Method to set the action data.
     *
     * @param actionData the additional data
     */
    public void setActionData(String actionData) {
        this.actionData = actionData;
    }

    /**
     * Set the resource path to the log entry.
     *
     * @param resourcePath the resource path.
     */
    public void setPath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * Set the user name the action is logged with.
     *
     * @param userName the user name
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
