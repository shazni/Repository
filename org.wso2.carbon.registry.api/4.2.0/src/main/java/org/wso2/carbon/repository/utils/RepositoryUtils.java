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

package org.wso2.carbon.repository.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.wso2.carbon.base.ServerConfiguration;
//import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.repository.Registry;
import org.wso2.carbon.repository.RegistryService;
import org.wso2.carbon.repository.RepositoryConstants;
import org.wso2.carbon.repository.Resource;
import org.wso2.carbon.repository.StatisticsCollector;
import org.wso2.carbon.repository.config.StaticConfiguration;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.exceptions.RepositoryUserContentException;
//import org.wso2.carbon.utils.CarbonUtils;
//import org.wso2.carbon.utils.ServerConstants;
//import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * This class contains a set of useful utility methods used by the Registry Kernel. These can also
 * be used by third party components as well as the various clients written to access the registry
 * via its remote APIs.
 */
public final class RepositoryUtils {

    private static final Log log = LogFactory.getLog(RepositoryUtils.class);
    private static final String ENCODING = System.getProperty("carbon.registry.character.encoding");
    private static String resourceMediaTypeMappings = null ;
    private static String collectionMediaTypeMappings = null;
    
    private static volatile List<StatisticsCollector> statisticsCollectors = new LinkedList<StatisticsCollector>();

    private RepositoryUtils() {
    }

    /**
     * Convert an input stream into a byte array.
     *
     * @param inputStream the input stream.
     *
     * @return the byte array.
     * @throws RepositoryException if the operation failed.
     */
    public static byte[] getByteArray(InputStream inputStream) throws RepositoryException {

        if (inputStream == null) {
            String msg = "Could not create memory based content for null input stream.";
            log.error(msg);
            throw new RepositoryUserContentException(msg);
        }

        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            byte[] contentChunk = new byte[RepositoryConstants.DEFAULT_BUFFER_SIZE];
            int byteCount;
            while ((byteCount = inputStream.read(contentChunk)) != -1) {
                out.write(contentChunk, 0, byteCount);
            }
            out.flush();

            return out.toByteArray();

        } catch (IOException e) {
            String msg = "Failed to write data to byte array input stream. " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryUserContentException(msg, e);
        } finally {
            try {
                inputStream.close();
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                String msg = "Failed to close streams used for creating memory stream. "
                        + e.getMessage();
                log.error(msg, e);
            }
        }
    }
    
    /**
     * Create an in-memory input stream for the given input stream.
     *
     * @param inputStream the input stream.
     *
     * @return the in-memory input stream.
     * @throws RepositoryException if the operation failed.
     */
    public static InputStream getMemoryStream(InputStream inputStream) throws RepositoryException {
        return new ByteArrayInputStream(getByteArray(inputStream));
    }

    /**
     * Method to obtain the parent path of the given resource path.
     *
     * @param resourcePath the resource path.
     *
     * @return the parent path.
     */
    public static String getParentPath(String resourcePath) {
        if (resourcePath == null) {
            return null;
        }

        String parentPath;
        if (resourcePath.equals(RepositoryConstants.ROOT_PATH)) {
            parentPath = null;
        } else {
            String formattedPath = resourcePath;
            if (resourcePath.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
                formattedPath = resourcePath.substring(
                        0, resourcePath.length() - RepositoryConstants.PATH_SEPARATOR.length());
            }

            if (formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) <= 0) {
                parentPath = RepositoryConstants.ROOT_PATH;
            } else {
                parentPath = formattedPath.substring(
                        0, formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR));
            }
        }

        return parentPath;
    }

    /**
     * Returns resource name when full resource path is passed.
     *
     * @param resourcePath full resource path.
     *
     * @return the resource name.
     */
    public static String getResourceName(String resourcePath) {
        String resourceName;
        if (resourcePath.equals(RepositoryConstants.ROOT_PATH)) {
            resourceName = RepositoryConstants.ROOT_PATH;

        } else {

            String formattedPath = resourcePath;
            if (resourcePath.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
                formattedPath = resourcePath.substring(
                        0, resourcePath.length() - RepositoryConstants.PATH_SEPARATOR.length());
            }

            if (formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) == 0) {
                resourceName = formattedPath.substring(1, formattedPath.length());
            } else {
                resourceName = formattedPath.substring(
                        formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) + 1,
                        formattedPath.length());
            }
        }

        return resourceName;
    }
    
    /**
     * Method to determine whether the given array of strings contains the given string.
     *
     * @param value the string to search.
     * @param array the array of string.
     *
     * @return whether the given string was found.
     */
    public static boolean containsString(String value, String[] array) {
        boolean found = false;

        for (String anArray : array) {
            if (anArray.equals(value)) {
                found = true;
            }
        }

        return found;
    }

    /**
     * Method to determine whether the given array of strings contains a string which has the given
     * string as a portion (sub-string) of it.
     *
     * @param value the string to search.
     * @param array the array of string.
     *
     * @return whether the given string was found.
     */
    public static boolean containsAsSubString(String value, String[] array) {
        for (String anArray : array) {
            if (anArray.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method to record statistics.
     *
     * @param parameters The parameters to be passed to the statistics collector interface.
     *                   Generally, it is expected that each method invoking this method will pass
     *                   all relevant parameters such that the statistics collectors can examine
     *                   them as required.
     */
    public static void recordStatistics(Object ... parameters) {
        StatisticsCollector[] statisticsCollectors = getStatisticsCollectors();
        for (StatisticsCollector collector : statisticsCollectors) {
            collector.collect(parameters);
        }
    }
    
    /**
     * Method to obtain a list of statistics collectors.
     *
     * @return array of statistics collectors if one or more statistics collectors exist, or an
     * empty array.
     */
    public static StatisticsCollector[] getStatisticsCollectors() {
        return statisticsCollectors.isEmpty() ? new StatisticsCollector[0] :
                statisticsCollectors.toArray(new StatisticsCollector[statisticsCollectors.size()]);
    }

    /**
     * Method to add a statistics collector
     *
     * @param statisticsCollector the statistics collector to be added.
     */
    public static void addStatisticsCollector(StatisticsCollector statisticsCollector) {
        statisticsCollectors.add(statisticsCollector);
    }
    
    /**
     * Method to obtain the relative path for the given absolute path.
     *
     * @param context      the registry context.
     * @param absolutePath the absolute path.
     *
     * @return the relative path.
     */
    public static String getRelativePath(Registry registry, String absolutePath) {
    	if(registry.getRegistryService() != null) {
    		return getRelativePathToOriginal(absolutePath, registry.getRegistryService().getRegistryRoot());
    	} else {
    		return getRelativePathToOriginal(absolutePath, StaticConfiguration.getRegistryRoot());
    	}
    }
    
    /**
     * Method to obtain the absolute path of a given relative path
     * 
     * @param registryService     the corresponding registry service for the operation
     * @param relativePath        the relative path of which absolute path is required
     * @return                    the absolute path of the given relative path
     */
    public static String getAbsolutePath(RegistryService registryService, String relativePath) {
    	if(registryService != null) {
    		return getAbsolutePathToOriginal(relativePath, registryService.getRegistryRoot());
    	} else {
    		return getAbsolutePathToOriginal(relativePath, StaticConfiguration.getRegistryRoot());
    	}
    }

    /**
     * Method to obtain the absolute path of the given relative path
     * 
     * @param registry            the corresponding registry on which the operation is performed 
     * @param relativePath        the relative path of which absolute path is required
     * @return                    the absolute path of the given relative path
     */
    public static String getAbsolutePath(Registry registry, String relativePath) {
    	if(registry.getRegistryService() != null) {
    		return getAbsolutePathToOriginal(relativePath, registry.getRegistryService().getRegistryRoot());
    	} else {
    		return getAbsolutePathToOriginal(relativePath, StaticConfiguration.getRegistryRoot());
    	}
    }

    /**
     * Method to obtain the path relative to the given path.
     *
     * @param absolutePath the absolute path.
     * @param originalPath the path to which we need to make the given absolute path a relative
     *                     one.
     *
     * @return the relative path.
     */
    public static String getRelativePathToOriginal(String absolutePath, String originalPath) {
        // the relative path of a path that is null, will be null
        if (absolutePath == null) {
            return null;
        }
        if (!absolutePath.startsWith("/")) {
            // we can consider this is not a path
            return absolutePath;
        }
        // No worries if there's no original path
        if (originalPath == null || originalPath.length() == 0 || originalPath.equals("/")) {
            return absolutePath;
        }

        if (originalPath.equals(absolutePath)) {
            return "/";
        }

        if (absolutePath.startsWith(originalPath)) {
            return absolutePath.substring(originalPath.length());
        }

        // Somewhere else, so make sure there are dual slashes at the beginning
        return "/" + absolutePath;
    }

    /**
     * Method to obtain the path absolute to the given path.
     *
     * @param relativePath the relative path.
     * @param originalPath the path to which we need to make the given relative path a absolute
     *                     one.
     *
     * @return the absolute path.
     */
    public static String getAbsolutePathToOriginal(String relativePath, String originalPath) {
        // the absolute path of a path that is null, will be null
        if (relativePath == null) {
            return null;
        }
        if (!relativePath.startsWith("/")) {
            // we can consider this is not a path
            return relativePath;
        }
        // No worries if there's no original path
        if (originalPath == null || originalPath.length() == 0 || originalPath.equals("/")) {
            return relativePath;
        }
        if (relativePath.startsWith("//")) {
            // the path is outside the scope of the original path, so return absolute path removing
            // the first '/'
            return relativePath.substring(1);
        }
        // then it is the concatenate that make the absolute path
        return originalPath + relativePath;
    }

    /**
     * Load the class with the given name
     *
     * @param name name of the class
     *
     * @return java class
     * @throws ClassNotFoundException if the class does not exists in the classpath
     */
    public static Class loadClass(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch(ClassNotFoundException e) {
        	throw e;
        }
    }

    /**
     * Method to determine whether a property is a hidden property or not.
     * @param propertyName the name of property.
     * @return true if the property is a hidden property or false if not.
     */
    public static boolean isHiddenProperty(String propertyName) {
        return propertyName.startsWith("registry.");
    }

    /**
     * Method to decode a given set of bytes in configured encoding type
     * 
     * @param byteContent			     the Content to be decoded
     * @return                           decoded content as a Sting
     * @throws RepositoryException       thrown when an error occurs
     */
    public static String decodeBytes(byte[] byteContent)
            throws RepositoryException {
        String co;
        try {
            if (ENCODING == null) {
                co = new String(byteContent);
            } else {
                co = new String(byteContent, ENCODING);
            }
        } catch (UnsupportedEncodingException e) {
            String msg = ENCODING + " is unsupported encoding type";
            log.error(msg ,e);
            throw new RepositoryUserContentException(msg, e);
        }
        return co;
    }

    /**
     * Method to encode a Sting by a given encoding type
     *  
     * @param content                 String content to be encoded
     * @return                        The encoded String as a byte
     * @throws RepositoryException    throws when an error occurs 
     */
    public static byte[] encodeString(String content) throws RepositoryException {
        byte[] bytes;
        try {
            if (ENCODING == null) {
                bytes = content.getBytes();
            } else {
                bytes = content.getBytes(ENCODING);
            }

        } catch (UnsupportedEncodingException e) {
            String msg = ENCODING + " is unsupported encoding type";
            log.error(msg,e);
            throw new RepositoryUserContentException(msg, e);
        }
        return bytes;
    }
    
    /**
     * Method to obtain resource media types.
     *
     * @return the resource media types.
     */
    public static String getResourceMediaTypes() throws RepositoryException {
    	return resourceMediaTypeMappings;
    }

    /**
     * Method to set resource media types.
     *
     * @param resourceMediaTypes the resource media types.
     */
    public static void setResourceMediaTypes(String resourceMediaTypes) throws RepositoryException {
    	resourceMediaTypeMappings = resourceMediaTypes;
    }
    
    /**
     * Method to obtain resource media types.
     *
     * @return the resource media types.
     */
    public static String getCollectionMediaTypes() throws RepositoryException {
    	return collectionMediaTypeMappings;
    }

    /**
     * Method to set resource media types.
     *
     * @param resourceMediaTypes the resource media types.
     */
    public static void setCollectionMediaTypes(String collectionMediaTypes) throws RepositoryException {
    	collectionMediaTypeMappings = collectionMediaTypes;
    }

	/**
	 * Calculate the absolute associations path if a relative path is given to the reference path.
	 * This works only ".." components are there at the very start. This is used in restore.
	 *
	 * @param path          relative path value.
	 * @param referencePath the reference path
	 *
	 * @return the absolute path
	 */
	public static String getAbsoluteAssociationPath(String path,
	                                                String referencePath) {
	    //StringTokenizer basePathTokenizer = new StringTokenizer(referencePath);
	    String[] referencePathParts = referencePath.split("/");
	    String[] pathParts = path.split("/");
	
	    int i;
	    // here the limit of going up = referencePathParts.length - 2
	    // (excluding "" component and resource name)
	    for (i = 0; i < referencePathParts.length - 2 && i < pathParts.length; i++) {
	        if (!pathParts[i].equals("..")) {
	            break;
	        }
	    }
	    int goUps = i;
	    StringBuilder absolutePath = new StringBuilder();
	    // we are checking whether the path parts have more .. beyond the reference paths reach
	    for (; i < pathParts.length; i++) {
	        if (pathParts[i].equals("..")) {
	            absolutePath.append("/");
	        } else {
	            break;
	        }
	    }
	    int remainLen = i;
	
	    for (i = 0; i < referencePathParts.length - goUps - 1; i++) {
	        absolutePath.append(referencePathParts[i]).append("/");
	    }
	    // from that onwards we collecting path parts.
	    for (i = remainLen; i < pathParts.length - 1; i++) {
	        absolutePath.append(pathParts[i]).append("/");
	    }
	    return absolutePath.append(pathParts[pathParts.length - 1]).toString();
	}

	/**
	 * Calculate the relative associations path  to the reference path if an absolute path is given.
	 * This is used in dump.
	 *
	 * @param path          absolute path value.
	 * @param referencePath the reference path
	 *
	 * @return the relative path
	 */
	public static String getRelativeAssociationPath(String path, String referencePath) {
	    //StringTokenizer basePathTokenizer = new StringTokenizer(referencePath);
	    String[] referencePathParts = referencePath.split("/");
	    String[] pathParts = path.split("/");
	
	    int i;
	    for (i = 0; i < referencePathParts.length - 1 && i < pathParts.length - 1; i++) {
	        if (!referencePathParts[i].equals(pathParts[i])) {
	            break;
	        }
	    }
	
	
	    StringBuilder prefix = new StringBuilder();
	    int j = i;
	    for (; i < referencePathParts.length - 1; i++) {
	        prefix.append("../");
	    }
	
	    for (; j < pathParts.length - 1; j++) {
	        prefix.append(pathParts[j]).append("/");
	    }
	
	    String relPath;
	    if(pathParts.length != 0){
	        relPath = prefix.append(pathParts[j]).toString();
	    } else {
	        relPath = path; //For the root("/") associations
	    }
	
	    while (relPath.contains("//")) {
	        relPath = relPath.replaceAll("//", "/../"); // in case "//" is found.
	    }
	    return relPath;
	}
  
    /**
     * Prepare the resource content to be put.
     *
     * @throws RepositoryException throws if the operation fail.
     */
    public static void prepareContentForPut(Resource resource) throws RepositoryException {

    	Object content = resource.getContent();
        if (content instanceof String) {
            content = RepositoryUtils.encodeString((String) content);
        } else if (content instanceof InputStream) {
            content = RepositoryUtils.getByteArray((InputStream) content);
        }
    }
}

