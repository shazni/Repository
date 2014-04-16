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

package org.wso2.carbon.repository.api.utils;

/**
 * Actions for the Activity
 *
 */
public enum Actions {
    /**
     * The log action to filter with. All is for don't filter at all.
     */
    ALL(-1),

    /**
     * Filter value for the resource adding action.
     */
    ADD(0),

    /**
     * Filter value for the resource updating action.
     */
    UPDATE(1),

    /**
     * Filter value for the resource deleting action.
     */
    DELETE_RESOURCE(7),

    /**
     * Filter value for the resource restoring action.
     */
    RESTORE(8),

    /**
     * Filter value for the resource renaming action.
     */
    RENAME(9),

    /**
     * Filter value for the resource moving action.
     */
    MOVE(10),

    /**
     * Filter value for the resource copying action.
     */
    COPY(11),

    /**
     * Filter value for the create remote link action.
     */
    CREATE_REMOTE_LINK(12),

    /**
     * Filter value for the create symbolic link action.
     */
    CREATE_SYMBOLIC_LINK(13),

    /**
     * Filter value for the removing link action.
     */
    REMOVE_LINK(14);

    private int id;

    Actions(int id) {
        this.id = id;
    }

    /**
     * Get the identifier for an Action
     *
     * @return identifier for the Action
     */
    public int getId () {
        return id;
    }

    /**
     * Compare identifier of the Action with the given value
     *
     * @param id identifier of the Action
     * @return whether the Actions identifier match with the given id
     */
    public boolean compare(int id){
        return this.id == id;
    }

    /**
     * Get Actions for the given identifier
     *
     * @param id  identifier for the Action
     * @return  Action for the identifier
     */
    public static Actions getAction (int id)
    {
        Actions[] actions = Actions.values();
        for(Actions action : actions)
        {
            if(action.compare(id))
                return action;
        }
        return Actions.ALL;
    }
}
