/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.wso2.carbon.repository.api.RepositoryConstants;

/**
 * This class represents resource paths used inside the repository. i.e. paths in the form
 * /c1/c2/r1;version:2;view:1
 */
public class ResourcePath {

	/**
	 * Version parameter name
	 */
    private static final String VERSION_PARAMETER_NAME = "version";

    /**
     * Complete path of the resource
     */
    private String completePath;

    /**
     * path of the resource
     */
    private String path;

    /**
     * Flag to see whether the resource referred in path is in current version
     */
    private boolean currentVersion = true;

    /**
     * Map of all the parameters
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * Create a resource path with a row path.
     *
     * @param rawPath the raw path.
     */
    public ResourcePath(String rawPath) {
        completePath = rawPath;

        if (!completePath.startsWith(RepositoryConstants.ROOT_PATH)) {
            completePath = RepositoryConstants.ROOT_PATH + completePath;
        }

        String[] parts = completePath.split(RepositoryConstants.URL_SEPARATOR);

        path = parts[0];
        if (!path.equals(RepositoryConstants.ROOT_PATH) && path.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
            path = path.substring(0, path.length() - RepositoryConstants.PATH_SEPARATOR.length());
        }

        for (int i = 1; i < parts.length; i++) {
            String[] paramParts = parts[i].split(RepositoryConstants.URL_PARAMETER_SEPARATOR);
            String key = paramParts[0];
            
            if (paramParts.length > 1) {
                StringBuilder sb = new StringBuilder(paramParts[1]);
                // if a parameter has more than one value, they are just appended to the value.
                // e.g. raw path: /c1/r1;p1:v1:v2
                // parameter name: p1
                // parameter value: v1:v2
                
                for (int j = 2; j < paramParts.length; j++) {
                    sb.append(RepositoryConstants.URL_PARAMETER_SEPARATOR).append(paramParts[j]);
                }
                
                parameters.put(key, sb.toString());
            } else {
                parameters.put(key, null);
            }

            if (VERSION_PARAMETER_NAME.equals(key)) {
                currentVersion = false;
            }
        }
    }

    /**
     * Return the complete path.
     *
     * @return the complete path.
     */
    public String getCompletePath() {
        return completePath;
    }

    /**
     * Return the path component, without any parameters. (the path component before the ';'
     * token).
     *
     * @return the path value.
     */
    public String getPath() {
        return path;
    }

    /**
     * Check whether a parameter with the provided key exist.
     *
     * @param key the parameter key.
     *
     * @return true, if the parameter exists, false otherwise.
     */
    public boolean parameterExists(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Get the parameter value.
     *
     * @param key the parameter key.
     *
     * @return the parameter value.
     */
    public String getParameterValue(String key) {
        return parameters.get(key);
    }

    /**
     * Set the parameter.
     *
     * @param key   the parameter key.
     * @param value the parameter value.
     */
    public void setParameter(String key, String value) {
        parameters.put(key, value);
        completePath = path + getParameterPath();
    }

    /**
     * Append a component to the current path.
     *
     * @param pathToAppend the path to append.
     */
    public void appendPath(String pathToAppend) {
        if (pathToAppend == null || pathToAppend.length() == 0) {
            return;
        }

        if (pathToAppend.startsWith(RepositoryConstants.PATH_SEPARATOR)) {
            pathToAppend = pathToAppend.substring(RepositoryConstants.PATH_SEPARATOR.length());
        }

        if (!path.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
            path = path + RepositoryConstants.PATH_SEPARATOR;
        }

        path = path + pathToAppend;
        completePath = path + getParameterPath();
    }

    /**
     * Check whether the path is not a version-ed path.
     *
     * @return true, if it is a non-version-ed path, false otherwise.
     */
    public boolean isCurrentVersion() {
        return currentVersion;
    }

    /**
     * Return the path with version, if there is no version, just return the path.
     *
     * @return the path with the version.
     */
    public String getPathWithVersion() {
        if (isCurrentVersion()) {
            return getPath();
        }

        String versionString = getParameterValue(RepositoryConstants.VERSION_PARAMETER_NAME);
        
        if (versionString != null) {
            return getPath() + RepositoryConstants.URL_SEPARATOR + RepositoryConstants.VERSION_PARAMETER_NAME +
                    RepositoryConstants.URL_PARAMETER_SEPARATOR + versionString;
        }

        return getPath();
    }

    /**
     * The path to string, same as getCompletePath
     *
     * @return the string value of the path.
     */
    public String toString() {
        return completePath;
    }

    /**
     * Get the parameter component of the path.
     *
     * @return the parameter path.
     */
    private String getParameterPath() {
        StringBuilder paramPath = new StringBuilder();
        
        for (Map.Entry<String, String> e : parameters.entrySet()) {
            paramPath.append(RepositoryConstants.URL_SEPARATOR).append(e.getKey()).append(
                    RepositoryConstants.URL_PARAMETER_SEPARATOR).append(e.getValue());
            
            if (VERSION_PARAMETER_NAME.equals(e.getKey())) {
                currentVersion = false;
            }
        }

        return paramPath.toString();
    }
}
