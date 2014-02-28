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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.repository.api.Collection;
import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.RepositoryConstants;
import org.wso2.carbon.repository.api.RepositoryService;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.StatisticsCollector;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.api.exceptions.RepositoryUserContentException;

/**
 * This class contains a set of useful utility methods used by the Repository Kernel. These can also
 * be used by third party components as well as the various clients written to access the repository
 * via its remote APIs.
 */
public final class RepositoryUtils {

    private static final Log log = LogFactory.getLog(RepositoryUtils.class);
    private static final String ENCODING = System.getProperty("carbon.registry.character.encoding");
    
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
                formattedPath = resourcePath.substring(0, resourcePath.length() - RepositoryConstants.PATH_SEPARATOR.length());
            }

            if (formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) <= 0) {
                parentPath = RepositoryConstants.ROOT_PATH;
            } else {
                parentPath = formattedPath.substring(0, formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR));
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
                formattedPath = resourcePath.substring(0, resourcePath.length() - RepositoryConstants.PATH_SEPARATOR.length());
            }

            if (formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) == 0) {
                resourceName = formattedPath.substring(1, formattedPath.length());
            } else {
                resourceName = formattedPath.substring(formattedPath.lastIndexOf(RepositoryConstants.PATH_SEPARATOR) + 1, formattedPath.length());
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
        return statisticsCollectors.isEmpty() ? new StatisticsCollector[0] : statisticsCollectors.toArray(new StatisticsCollector[statisticsCollectors.size()]);
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
     * @param registry      the registry instance.
     * @param absolutePath the absolute path.
     *
     * @return the relative path.
     */
    public static String getRelativePath(Repository registry, String absolutePath) {
    	if(registry.getRepositoryService() != null) {
    		return getRelativePathToOriginal(absolutePath, registry.getRepositoryService().getRepositoryRoot());
    	} 
    	
    	return null ;
    }
    
    /**
     * Method to obtain the absolute path of a given relative path
     * 
     * @param registryService     the corresponding registry service for the operation
     * @param relativePath        the relative path of which absolute path is required
     * @return                    the absolute path of the given relative path
     */
    public static String getAbsolutePath(RepositoryService registryService, String relativePath) {
    	if(registryService != null) {
    		return getAbsolutePathToOriginal(relativePath, registryService.getRepositoryRoot());
    	} 
    	
    	return null;
    }

    /**
     * Method to obtain the absolute path of the given relative path
     * 
     * @param registry            the corresponding registry on which the operation is performed 
     * @param relativePath        the relative path of which absolute path is required
     * @return                    the absolute path of the given relative path
     */
    public static String getAbsolutePath(Repository registry, String relativePath) {
    	if(registry.getRepositoryService() != null) {
    		return getAbsolutePathToOriginal(relativePath, registry.getRepositoryService().getRepositoryRoot());
    	}
    	
    	return null ;
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
    public static Class<?> loadClass(String name) throws ClassNotFoundException {
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
    public static String decodeBytes(byte[] byteContent) throws RepositoryException {
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
	 * Calculate the absolute associations path if a relative path is given to the reference path.
	 * This works only ".." components are there at the very start. This is used in restore.
	 *
	 * @param path          relative path value.
	 * @param referencePath the reference path
	 *
	 * @return the absolute path
	 */
	public static String getAbsoluteAssociationPath(String path, String referencePath) {
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
    
    /**
     * This method can be used to import a local file system into a running instance of a registry.
     * Need to create a file object representing the local file and the need to tell where to add
     * the resource in the registry.
     *
     * @param file     : File representing local file system
     * @param path     : Where to put the file
     * @param registry : Registry instance
     *
     * @throws org.wso2.carbon.registry.core.exceptions.RepositoryException
     *          : if something went wrong
     */
    public static void importToRegistry(File file, String path, Repository registry) throws RepositoryException {
        try {
            if (file == null || path == null) {
                throw new RepositoryException("The values of the mandatory parameters, file and path cannot be null.");
            }
            
            processImport(file, path, registry);
        } catch (Exception e) {
            log.error("Failed to import to registry", e);
            throw new RepositoryException("Failed to import to registry", e);
        }
    }

    /**
     * This method can be used to export registry instance or node in a registry to a local file
     * system. When we call this method a file structure will be created to match the structure to
     * map the structure in the registry
     *
     * @param toFile   : File in the local file system
     * @param path     : To which node export
     * @param registry : Registry instance
     *
     * @throws RepositoryException : If something went wrong
     */
    public static void exportFromRegistry(File toFile, String path, Repository registry) throws RepositoryException {
        try {
            processExport(path, toFile, registry, true);
        } catch (Exception e) {
            log.error("Failed to export from registry", e);
            throw new RepositoryException("Failed to export from registry", e);
        }
    }
    
    private static void processImport(File file, String path, Repository repository) throws Exception {
        String filePart = file.getName();
        String resourcePath = path + "/" + filePart;
        
        if (file.isDirectory()) {
            File files[] = file.listFiles();
            
            if (files.length > 0) {
                for (File childFile : files) {
                    processImport(childFile, resourcePath, repository);
                }
            } else {
                Collection resource = repository.newCollection() ;
                resource.setPath(resourcePath);
                repository.put(resourcePath, resource);
            }
        } else {
            Resource resource = repository.newResource() ;
            resource.setContent(new FileInputStream(file));
            resource.setPath(resourcePath);
            repository.put(resourcePath, resource);
        }
    }

    private static void processExport(String fromPath, File toFile, Repository repository, boolean useOriginal) throws RepositoryException {
        Resource resource = repository.get(fromPath);
        
        if (resource != null) {
            String resourcePath = resource.getPath();
            
            int versionIndex = resourcePath.lastIndexOf(RepositoryConstants.URL_SEPARATOR +
                    RepositoryConstants.VERSION_PARAMETER_NAME +
                    RepositoryConstants.URL_PARAMETER_SEPARATOR);
            
            if (versionIndex > 0) {
                resourcePath = resourcePath.substring(0, versionIndex);
            }
            
            int slashIndex = resourcePath.lastIndexOf('/');
            //getting only the last part of the resource path
            resourcePath = resourcePath.substring(slashIndex, resourcePath.length());
            File tempFile;
            
            if (!useOriginal) {
                tempFile = new File(toFile, resourcePath);
                if (!tempFile.exists() && resource instanceof Collection) {
                    boolean ignore = tempFile.mkdirs();
                    if (log.isTraceEnabled()) {
                        log.trace("Making a directory --> is success " + ignore);
                    }
                }
            } else {
                tempFile = toFile;
            }
            
            if (resource instanceof Collection) {
                String childNodes[] = (String[]) resource.getContent();
                ArrayList<String> tobeDeleted = new ArrayList<String>();
                String[] files = tempFile.list();
                
                if (files != null) {
                    for (String file : files) {
                        tobeDeleted.add("/" + file);
                    }
                }
                
                for (String childNode : childNodes) {
                    versionIndex = childNode.lastIndexOf(RepositoryConstants.URL_SEPARATOR +
                            RepositoryConstants.VERSION_PARAMETER_NAME +
                            RepositoryConstants.URL_PARAMETER_SEPARATOR);
                    
                    if (versionIndex > 0) {
                        childNode = childNode.substring(0, versionIndex);
                    }
                    
                    slashIndex = childNode.lastIndexOf('/');
                    //getting only the last part of the resource path
                    childNode = childNode.substring(slashIndex, childNode.length());
                    //tobeDeleted.remove(childNode);
                    
                    if (tobeDeleted.contains(childNode)) {
                        slashIndex = childNode.lastIndexOf('/');
                        childNode = childNode.substring(slashIndex, childNode.length());
                        File deleteFile = new File(tempFile, childNode);
                        
                        if (deleteFile.exists() && deleteFile.isDirectory()) {
                            deleteDir(deleteFile);
                            if (log.isTraceEnabled()) {
                                log.trace("Deleting a directory : " + deleteFile.getPath());
                            }
                        } else {
                            boolean ignore = deleteFile.delete();
                            if (log.isTraceEnabled()) {
                                log.trace("Deleting a file : " + deleteFile.getPath() + " is success " + ignore);
                            }
                        }
                    }
                }

                for (String childNode : childNodes) {
                    processExport(childNode, tempFile, repository, false);
                }
            } else {
                try {
                    FileOutputStream out = new FileOutputStream(tempFile);
                    out.write((byte[]) resource.getContent());
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    throw new RepositoryException("An error occurred while creating the file: " + tempFile.getAbsolutePath(), e);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No resource found for : " + fromPath);
            }
        }
    }

    // deletes a directory
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                
                if (!success) {
                    return false;
                }
            }
        }
        
        // The directory is now empty so delete it
        return dir.delete();
    }
}

