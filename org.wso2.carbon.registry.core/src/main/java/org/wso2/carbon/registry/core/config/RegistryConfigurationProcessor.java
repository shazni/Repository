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

package org.wso2.carbon.registry.core.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RepositoryConfigurationException;
import org.wso2.carbon.registry.core.exceptions.RepositoryDBException;
import org.wso2.carbon.registry.core.exceptions.RepositoryInitException;
import org.wso2.carbon.registry.core.jdbc.handlers.CustomEditManager;
import org.wso2.carbon.registry.core.jdbc.handlers.EditProcessor;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerLifecycleManager;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.repository.Aspect;
import org.wso2.carbon.repository.RegistryService;
import org.wso2.carbon.repository.RepositoryConstants;
import org.wso2.carbon.repository.config.StaticConfiguration;
import org.wso2.carbon.repository.exceptions.RepositoryException;
import org.wso2.carbon.repository.handlers.Handler;
import org.wso2.carbon.repository.handlers.filters.Filter;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.xml.sax.SAXException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Builds the registry configuration from xml document. Configuration has to be given as an input
 * stream. Registry configuration consists of details of data sources, handlers and aspects. These
 * information is extracted from the configuration populates the necessary components.
 */
//@SuppressWarnings("unused")
public class RegistryConfigurationProcessor {

    private static final Log log = LogFactory.getLog(RegistryConfigurationProcessor.class);

    /**
     * Read XML configuration from the passed InputStream, or from the classpath.
     *
     * @param in              an InputStream containing XML data, or null.
     * @param registryContext the RegistryContext to populate
     *
     * @throws RepositoryException if there's a problem
     */
    public static void populateRegistryConfig(InputStream in, RegistryContext registryContext, RegistryService registryService)
            throws RepositoryException {
    	
        try {
        	InputStream replacedStream = CarbonUtils.replaceSystemVariablesInXml(in);
        	DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
      
        	Document document = documentBuilder.parse(replacedStream);
        	Element documentElement = document.getDocumentElement() ;
        	documentElement.normalize();
        	
        	NodeList rootLists = documentElement.getElementsByTagName("registryRoot");
        	
        	if(rootLists != null && rootLists.getLength() > 0) {
        		Node node = rootLists.item(0);
        		if(node != null && node.getNodeType() == Node.ELEMENT_NODE) {
        			String registryRoot = ((Element) node).getTextContent();
        			
                    if (registryRoot != null && !registryRoot.equals(RepositoryConstants.ROOT_PATH)) {
                        if (registryRoot.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
                            registryRoot = registryRoot.substring(0, registryRoot.length() - 1);
                        } else if (!registryRoot.startsWith(RepositoryConstants.PATH_SEPARATOR)) {
                            registryRoot = RepositoryConstants.ROOT_PATH + registryRoot;
                        }
                    } else {
                        registryRoot = null;
                    }
                    
                    registryContext.setRegistryRoot(registryRoot);
//                    registryService.setRegistryRoot(registryRoot);
//                    StaticConfiguration.setRegistryRoot(registryRoot);
        		}
        	}
        	
        	NodeList readOnlyElements = documentElement.getElementsByTagName("readOnly");
        	
        	if(readOnlyElements != null && readOnlyElements.getLength() > 0) {
        		Node node = readOnlyElements.item(0);
        		if(node != null && node.getNodeType() == Node.ELEMENT_NODE) {
        			String isReadOnly = ((Element) node).getTextContent();
                    registryContext.setReadOnly(CarbonUtils.isReadOnlyNode() ||
                            "true".equals(isReadOnly));
//                    registryService.setReadOnly(CarbonUtils.isReadOnlyNode() ||
//                            "true".equals(readOnlyEle.getText()));
        		}
        	}
        	
        	NodeList enableCacheElements = documentElement.getElementsByTagName("enableCache");
        	
        	if(enableCacheElements != null && enableCacheElements.getLength() > 0) {
        		Node node = enableCacheElements.item(0);
        		if(node != null && node.getNodeType() == Node.ELEMENT_NODE) {
        			String enableCacheElement = ((Element) node).getTextContent();
                    registryContext.setCacheEnabled("true".equals(enableCacheElement));
//                    registryService.setCacheEnabled("true".equals(enableCachingEle.getText()));
        		}
        	}
        	
        	SecretResolver secretResolver = SecretResolverFactory.create(documentElement, false);
        	
        	NodeList dbConfigElements = documentElement.getElementsByTagName("dbConfig");
        	
        	for( int index = 0 ; index < dbConfigElements.getLength() ; index++ ) {
                Node dbConfig = dbConfigElements.item(index) ; 
                DataBaseConfiguration dataBaseConfiguration = new DataBaseConfiguration();

                dataBaseConfiguration.setPasswordManager(secretResolver);
                
                if(dbConfig != null && dbConfig.getNodeType() == Node.ELEMENT_NODE) {
                	Element dbConfigElement = (Element) dbConfig ;
                	String dbName = dbConfigElement.getAttribute("name");
                	
                    if (dbName == null) {
                        throw new RepositoryConfigurationException("The database configuration name cannot be null.");
                    }
                    
                    dataBaseConfiguration.setConfigName(dbName);
                    
                    NodeList dbConfigDataSources = dbConfigElement.getChildNodes();
                    
                    for( int content = 0 ; content < dbConfigDataSources.getLength() ; content++ ) {
                    	Node dbConfigNode = dbConfigDataSources.item(content) ;
                    	
                    	if(dbConfigNode != null && dbConfigNode.getNodeType() == Node.ELEMENT_NODE) {
	                    	if(dbConfigNode.getNodeName() == "dataSource") {
	                    		String dataSourceName = dbConfigNode.getTextContent();
	                            dataBaseConfiguration.setDataSourceName(dataSourceName);
	                            try {
	                                Context context = new InitialContext();
	                                Connection connection = null;
	                                try {
	                                    connection = ((DataSource) context.lookup(dataSourceName)).getConnection();
	                                    DatabaseMetaData metaData = connection.getMetaData();
	
	                                    dataBaseConfiguration.setDbUrl(metaData.getURL());
	                                    dataBaseConfiguration.setUserName(metaData.getUserName());
	                                } finally {
	                                    if (connection != null) {
	                                        connection.close();
	                                    }
	                                }
	                            } catch (NamingException ignored) {
	                                log.warn("Unable to look-up JNDI name " + dataSourceName);
	                            } catch (SQLException e) {
	                                e.printStackTrace();
	                                throw new RepositoryDBException("Unable to connect to Data Source", e);
	                            }
	                    	} else {
	                    		if(dbConfigNode.getNodeName() == "userName") {
	                    			dataBaseConfiguration.setUserName(dbConfigNode.getTextContent());
	                    		} else if(dbConfigNode.getNodeName() == "password") {
	                    			dataBaseConfiguration.setPassWord(dbConfigNode.getTextContent());
	                    		} else if(dbConfigNode.getNodeName() == "url") {
	                    			String dbUrl = dbConfigNode.getTextContent();
	                    			
		                            if (dbUrl != null) {
		                                if (dbUrl.contains(CarbonConstants.CARBON_HOME_PARAMETER)) {
		                                    File carbonHomeDir;
		                                    carbonHomeDir = new File(CarbonUtils.getCarbonHome());
		                                    String path = carbonHomeDir.getPath();
		                                    path = path.replaceAll(Pattern.quote("\\"), "/");
		                                    if (carbonHomeDir.exists() && carbonHomeDir.isDirectory()) {
		                                        dbUrl = dbUrl.replaceAll(
		                                                Pattern.quote(CarbonConstants.CARBON_HOME_PARAMETER),
		                                                path);
		                                    } else {
		                                        log.warn("carbon home invalid");
		                                        String[] tempStrings1 = dbUrl.split(
		                                                Pattern.quote(CarbonConstants.CARBON_HOME_PARAMETER));
		                                        String tempUrl = tempStrings1[1];
		                                        String[] tempStrings2 = tempUrl.split("/");
		                                        for (int i = 0; i < tempStrings2.length - 1; i++) {
		                                            dbUrl = tempStrings1[0] + tempStrings2[i] + "/";
		                                        }
		                                        dbUrl = dbUrl + tempStrings2[tempStrings2.length - 1];
		                                    }
		                                }
		                            }
		                            dataBaseConfiguration.setDbUrl(dbUrl);
	                    		} else if(dbConfigNode.getNodeName() == "maxWait") {
	                    			dataBaseConfiguration.setMaxWait(dbConfigNode.getTextContent());
	                    		} else if(dbConfigNode.getNodeName() == "maxActive") {
	                    			dataBaseConfiguration.setMaxActive(dbConfigNode.getTextContent());
	                    		} else if(dbConfigNode.getNodeName() == "minIdle") {
	                    			dataBaseConfiguration.setMinIdle(dbConfigNode.getTextContent());
	                    		} else if(dbConfigNode.getNodeName() == "driverName") {
	                    			dataBaseConfiguration.setDriverName(dbConfigNode.getTextContent());
	                    		}
	                    	}
                    	}
                    }
                    registryContext.addDBConfig(dbName, dataBaseConfiguration);
                }
        	}

            	NodeList staticConfigNodes = documentElement.getElementsByTagName("staticConfiguration");
            	if(staticConfigNodes != null && staticConfigNodes.getLength() > 0) {
            		Node staticConfigNode = staticConfigNodes.item(0);
            		NodeList staticConfigItems = staticConfigNode.getChildNodes();
            		
            		for(int index = 0 ; index < staticConfigItems.getLength() ; index++) {
            			Node staticConfig = staticConfigItems.item(index);
            			
            			if(staticConfig != null && staticConfig.getNodeType() == Node.ELEMENT_NODE) {
	                        if (staticConfig.getNodeName().equals("versioningProperties")) {
	                            String versioningProperties = staticConfig.getTextContent();
	                            StaticConfiguration
	                                    .setVersioningProperties(versioningProperties.equals("true"));
	                        } else if (staticConfig.getNodeName().equals("versioningComments")) {
	                            String versioningComments = staticConfig.getTextContent();
	                            StaticConfiguration
	                                    .setVersioningComments(versioningComments.equals("true"));
	                        } else if (staticConfig.getNodeName().equals("versioningTags")) {
	                            String versioningTags = staticConfig.getTextContent();
	                            StaticConfiguration.setVersioningTags(versioningTags.equals("true"));
	                        } else if (staticConfig.getNodeName().equals("versioningRatings")) {
	                            String versioningRatings = staticConfig.getTextContent();
	                            StaticConfiguration
	                                    .setVersioningRatings(versioningRatings.equals("true"));
	                        } else if (staticConfig.getNodeName().equals("versioningAssociations")) {
	                            String versioningAssociations = staticConfig.getTextContent();
	                            StaticConfiguration.setVersioningAssociations(
	                                    versioningAssociations.equals("true"));
	                        } else if (staticConfig.getNodeName().equals("profilesPath")) {
	                            String profilesPath = staticConfig.getTextContent();
	                            if (!profilesPath.startsWith(
	                                    RepositoryConstants.PATH_SEPARATOR)) {
	                                //if user give the path like test or test/
	                                profilesPath = RepositoryConstants.PATH_SEPARATOR + profilesPath;
	                            }
	                            if (profilesPath.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
	                                profilesPath = profilesPath.substring(0, (profilesPath.length() -
	                                        1)); //if user give the path like this /test/
	                            }
	
	                            if (profilesPath != null) {
	                                if (profilesPath.startsWith(
	                                        RepositoryConstants.CONFIG_REGISTRY_BASE_PATH)) {
	                                    registryContext.setProfilesPath(profilesPath);
	                                } else {
	                                    registryContext.setProfilesPath(
	                                            RepositoryConstants.CONFIG_REGISTRY_BASE_PATH +
	                                                    profilesPath);
	                                }
	                            }
	                        } else if (staticConfig.getNodeName().equals("servicePath")) {
	                            String servicePath = staticConfig.getTextContent();
	                            if (!servicePath.startsWith(
	                                    RepositoryConstants.PATH_SEPARATOR)) {
	                                //if user give the path like test or test/
	                                servicePath = RepositoryConstants.PATH_SEPARATOR + servicePath;
	                            }
	                            if (servicePath.endsWith(RepositoryConstants.PATH_SEPARATOR)) {
	                                servicePath = servicePath.substring(0, (servicePath.length() -
	                                        1)); //if user give the path like this /test/
	                            }
	
	                            if (servicePath != null) {
	                                if (servicePath.startsWith(
	                                        RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH)) {
	                                    registryContext.setServicePath(servicePath);
	                                } else {
	                                    registryContext.setServicePath(
	                                            RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
	                                                    servicePath);
	//                                    registryService.setServicePath(RepositoryConstants.GOVERNANCE_REGISTRY_BASE_PATH +
	//                                                    servicePath);
	                                }
	                            }
	                        }
            			}
            		}
            	}
                  	
            	NodeList currentDBConfigs = documentElement.getElementsByTagName("currentDBConfig");
            	if(currentDBConfigs == null) {
            		throw new RepositoryConfigurationException("The current database configuration is not defined.");
            	} 
            	
            	String currentConfigName = currentDBConfigs.item(0).getTextContent();

//                String currentConfigName = currentConfigElement.getText();
            	
            	readRemoteInstances(documentElement, registryContext, secretResolver);
//                readRemoteInstances(configElement, registryContext, secretResolver);
            	
            	readMounts(documentElement, registryContext);
//                readMounts(configElement, registryContext);
            	
                DataBaseConfiguration dbConfiguration = registryContext.selectDBConfig(currentConfigName);
                registryContext.setDefaultDataBaseConfiguration(dbConfiguration);

                NodeList versionConfigList = documentElement.getElementsByTagName("versionResourcesOnChange");
                if(versionConfigList != null && versionConfigList.getLength() > 0) {
                	Node versionConfig = versionConfigList.item(0);
                	if (versionConfig != null && "true".equals(versionConfig.getTextContent())) {
                        registryContext.setVersionOnChange(true);
                    } else {
                    	registryContext.setVersionOnChange(false);              
                    }
                }
                
//                OMElement versionConfig =
//                        configElement.getFirstChildWithName(new QName("versionResourcesOnChange"));
//                if (versionConfig != null && "true".equals(versionConfig.getText())) {
//                    registryContext.setVersionOnChange(true);
//                } else {
//                	registryContext.setVersionOnChange(false);              
//                }
                
                initializeHandlers(documentElement, registryContext);
//                initializeHandlers(configElement, registryContext);

                // process query processor config
                NodeList queryProcessors = documentElement.getElementsByTagName("queryProcessor");
                
                for( int index = 0 ; index < queryProcessors.getLength() ; index++ ) {
                    QueryProcessorConfiguration queryProcessorConfiguration = new QueryProcessorConfiguration();

                    Node queryProcessorNode = queryProcessors.item(index);
                    NodeList queryProcessorChildren = queryProcessorNode.getChildNodes();
                    
                    for( int childIndex = 0 ; childIndex < queryProcessorChildren.getLength() ; childIndex++ ) {
                    	Node queryProcessorChild = queryProcessorChildren.item(childIndex);
                    	
                    	if(queryProcessorChild.getNodeName() == "queryType") {
                    		queryProcessorConfiguration.setQueryType(queryProcessorChild.getTextContent());
                    	} else if(queryProcessorChild.getNodeName() == "processor") {
                    		queryProcessorConfiguration.setProcessorClassName(queryProcessorChild.getTextContent());
                    	}
                    }
                    
                    registryContext.addQueryProcessor(queryProcessorConfiguration);
                }
                
//                Iterator queryProcessors = configElement.
//                        getChildrenWithName(new QName("queryProcessor"));
//                while (queryProcessors.hasNext()) {
//
//                    QueryProcessorConfiguration queryProcessorConfiguration =
//                            new QueryProcessorConfiguration();
//
//                    OMElement queryProcessorElement = (OMElement) queryProcessors.next();
//                    OMElement queryType = queryProcessorElement.
//                            getFirstChildWithName(new QName("queryType"));
//                    if (queryType != null) {
//                        queryProcessorConfiguration.setQueryType(queryType.getText());
//                    }
//
//                    OMElement processorName = queryProcessorElement.
//                            getFirstChildWithName(new QName("processor"));
//                    if (processorName != null) {
//                        queryProcessorConfiguration.
//                                setProcessorClassName(processorName.getText());
//                    }
//
//                    registryContext.addQueryProcessor(queryProcessorConfiguration);
//                }

                initializeAspects(documentElement, registryContext);
//                initializeAspects(configElement, registryContext);

//            }

        } catch (SAXException e1) {
        	throw new RepositoryInitException(e1.getMessage());
		} catch (IOException e1) {
			throw new RepositoryInitException(e1.getMessage());
		} catch (ParserConfigurationException e1) {
        	throw new RepositoryInitException(e1.getMessage());
		} catch (CarbonException e) {
            log.error("An error occurred during system variable replacement", e);
        } 
    }

    // Creates and initializes a handler
    private static void initializeHandlers(Element configElement, RegistryContext registryContext)
            throws RepositoryException {
        // process handler configurations
        CustomEditManager customEditManager = registryContext.getCustomEditManager();
        try {
        	NodeList handlerConfigs = configElement.getElementsByTagName("handler");
        	String currentProfile = System.getProperty("profile", "default");
        	
        	for(int index = 0 ; index < handlerConfigs.getLength() ; index++ ) {
        		Node handlerNode = handlerConfigs.item(index);
        		
        		if(handlerNode != null && handlerNode.getNodeType() == Node.ELEMENT_NODE) {
        			Element handlerConfigElement = (Element) handlerNode ;
                    String profileStr = handlerConfigElement.getAttribute("profiles");
                    if (profileStr != null){
                        String[] profiles = profileStr.split(",");
                        for (String profile : profiles) {
                            if (profile.trim().equals(currentProfile)) {
                                buildHandler(registryContext, customEditManager, handlerConfigElement, null);
                            }
                        }
                    } else {
                        buildHandler(registryContext, customEditManager, handlerConfigElement, null);
                    }
        		}
        	}
        } catch (Exception e) {
            String msg = "Could not initialize custom handlers. Caused by: " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryConfigurationException(msg, e);
        }
    }

    /**
     * Updates a handler based on given configuration.
     *
     * @param configElement   the handler configuration element.
     * @param lifecyclePhase  the lifecycle phase to which this handler belongs. The possible values
     *                        are "default", "reporting" and "user".
     * @param registryContext the Registry Context used by this registry instance.
     *
     * @return Created handler
     * @throws RepositoryException if anything goes wrong.
     */
    public static boolean updateHandler(Element configElement, RegistryContext registryContext,
                                        String lifecyclePhase)
            throws RepositoryException {
        try {
        	NodeList handlerConfigs = configElement.getElementsByTagName("handler");
        	
        	if(handlerConfigs != null && handlerConfigs.getLength() > 0) {
        		Node handlerConfigNode = handlerConfigs.item(0);
        		
        		if(handlerConfigNode != null && handlerConfigNode.getNodeType() == Node.ELEMENT_NODE) {
        			Element handlerConfigElement = (Element) handlerConfigNode ;
        			return buildHandler(registryContext, null, handlerConfigElement, lifecyclePhase);
        		}
        	}
        	
//            Iterator handlerConfigs =
//                    configElement.getChildrenWithName(new QName("handler"));
//            if (handlerConfigs != null) {
//                OMElement handlerConfigElement = (OMElement) handlerConfigs.next();
//                // We won't be adding custom edit processors for handlers inserted through the UI.
//                // This is because the CustomEditManager is not MT aware.
//                return buildHandler(registryContext, null, handlerConfigElement, lifecyclePhase);
//            }
            return false;
        } catch (Exception e) {
            String msg = "Could not create custom handler. Caused by: " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryConfigurationException(msg, e);
        }
    }

    // common method to build a handler
    private static boolean buildHandler(RegistryContext registryContext,
                                        CustomEditManager customEditManager,
                                        Element handlerConfigElement,
                                        String lifecyclePhase)
            throws InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException, UserStoreException {
        HandlerDefinitionObject handlerDefinitionObject =
                new HandlerDefinitionObject(customEditManager, handlerConfigElement).invoke();
        String[] methods = handlerDefinitionObject.getMethods();
        Filter filter = handlerDefinitionObject.getFilter();
        Handler handler = handlerDefinitionObject.getHandler();
        if (filter == null || handler == null) {
            return false;
        }
        if (lifecyclePhase != null) {
            if (handlerDefinitionObject.getTenantId() != MultitenantConstants.INVALID_TENANT_ID &&
                    !HandlerLifecycleManager.DEFAULT_SYSTEM_HANDLER_PHASE.equals(lifecyclePhase) &&
                    !HandlerLifecycleManager.USER_DEFINED_SYSTEM_HANDLER_PHASE.equals(
                            lifecyclePhase)) {
                CurrentSession.setCallerTenantId(handlerDefinitionObject.getTenantId());
                try {
                    // We need to swap the tenant id for this call, if the handler overrides the
                    // default value.
                    registryContext.getHandlerManager().addHandler(methods, filter,
                            handler, lifecyclePhase);
                } finally {
                    CurrentSession.removeCallerTenantId();
                }
            } else {
                registryContext.getHandlerManager().addHandler(methods, filter,
                        handler, lifecyclePhase);
            }
        } else {
            registryContext.getHandlerManager().addHandler(methods, filter, handler,
                    HandlerLifecycleManager.USER_DEFINED_SYSTEM_HANDLER_PHASE);
        }
        return true;
    }

    // reads remote instances from the configuration
    private static void readRemoteInstances(Element configElement,
                                            RegistryContext registryContext,
                                            SecretResolver secretResolver) throws RepositoryException {
        try {
            NodeList remoteConfigs = configElement.getElementsByTagName("remoteInstance");
            List<String> idList = new ArrayList<String>();

            for(int index = 0 ; index < remoteConfigs.getLength() ; index++) {
            	Node remoteConfigNode = remoteConfigs.item(index);
            	
            	if(remoteConfigNode != null && remoteConfigNode.getNodeType() == Node.ELEMENT_NODE) {
            		Element remoteConfigElement = (Element) remoteConfigNode ;
            		
            		String url = remoteConfigElement.getAttribute("url");
            		
            		NodeList remoteConfigItems = remoteConfigElement.getChildNodes() ;
            		
            		for( int itemNum = 0 ; itemNum < remoteConfigItems.getLength() ; itemNum++ ) {
            			Node remoteChildItem = remoteConfigItems.item(itemNum);
            			
            			if(remoteChildItem != null && remoteChildItem.getNodeType() == Node.ELEMENT_NODE) {
            			
	            			String id = null;
	            			String trustedUser = null;
	            			String trustedPwd = null;
	            			String type = null;
	            			String dbConfig = null;
	            			String readOnly = null;
	            			String enableCache = null;
	            			String cacheId = null;
	            			String registryRoot = null;
	            			
	            			if(remoteChildItem.getNodeName() == "id") {
	            				id = remoteChildItem.getTextContent();
	            				
	                            if (idList.contains(id)) {
	                                String msg = "Two remote instances can't have the same id.";
	                                log.error(msg);
	                                throw new RepositoryConfigurationException(msg);
	                            }
	                            
	                            idList.add(id);
	            			} else if(remoteChildItem.getNodeName() == "username") {
	            				trustedUser = remoteChildItem.getTextContent();
	            			} else if(remoteChildItem.getNodeName() == "password") {
	            				trustedPwd = remoteChildItem.getTextContent();
	            			} else if(remoteChildItem.getNodeName() == "type") {
	            				type = remoteChildItem.getTextContent();
	            			} else if(remoteChildItem.getNodeName() == "dbConfig") {
	            				dbConfig = remoteChildItem.getTextContent();
	            			} else if(remoteChildItem.getNodeName() == "readOnly") {
	            				readOnly = remoteChildItem.getTextContent();
	            			} else if(remoteChildItem.getNodeName() == "enableCache") {
	            				enableCache = remoteChildItem.getTextContent();
	            			} else if(remoteChildItem.getNodeName() == "cacheId") {
	            				cacheId = remoteChildItem.getTextContent();
	            			} else if(remoteChildItem.getNodeName() == "registryRoot") {
	            				registryRoot = remoteChildItem.getTextContent();
	            			}
	            			
	                        RemoteConfiguration remoteConfiguration = new RemoteConfiguration();
	                        remoteConfiguration.setPasswordManager(secretResolver);
	                        remoteConfiguration.setId(id);
	                        remoteConfiguration.setUrl(url);
	                        remoteConfiguration.setTrustedUser(trustedUser);
	                        remoteConfiguration.setTrustedPwd(trustedPwd);
	                        remoteConfiguration.setType(type);
	                        remoteConfiguration.setDbConfig(dbConfig);
	                        remoteConfiguration.setReadOnly(readOnly);
	                        remoteConfiguration.setCacheEnabled(enableCache);
	                        remoteConfiguration.setCacheId(cacheId);
	                        remoteConfiguration.setRegistryRoot(registryRoot);
	
	                        registryContext.getRemoteInstances().add(remoteConfiguration);
            			}
            		}
            	}
            }
        } catch (Exception e) {
            String msg =
                    "Could not read remote instance configuration. Caused by: " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryConfigurationException(msg, e);
        }

    }

    // read mounts from configuration
    private static void readMounts(Element configElement,
                                   RegistryContext registryContext) throws RepositoryException {
        try {
            NodeList mounts = configElement.getElementsByTagName("mount");
            List<String> pathList = new ArrayList<String>();

            for( int mountItem = 0 ; mountItem < mounts.getLength() ; mountItem++ ) {
            	Node mountNode = mounts.item(mountItem);
            	
            	if(mountNode != null && mountNode.getNodeType() == Node.ELEMENT_NODE) {
            		Element mountElement = (Element) mountNode ;
            		
            		String path = mountElement.getAttribute("path");
            		
                    if (path == null) {
                        String msg = "The path attribute was not specified for remote mount. " +
                                "Skipping creation of remote mount. " +
                                "Element: " + mountElement.toString();
                        log.warn(msg);
                        continue;    
                    }
                    if (pathList.contains(path)) {
                        String msg = "Two remote instances can't have the same path.";
                        log.error(msg);
                        throw new RepositoryConfigurationException(msg);
                    }
                    
                    NodeList mountChildren = mountElement.getChildNodes() ;
                    
                    String instanceId = null ;
                    String targetPath = null ;
                    
                    for( int mountIndex = 0 ; mountIndex < mountChildren.getLength() ; mountIndex++ ) {
                    	Node mountChild = mountChildren.item(mountIndex);
                    	
                    	if(mountChild != null && mountChild.getNodeType() == Node.ELEMENT_NODE) {
	                    	if(mountChild.getNodeName() == "instanceid") {
	                    		instanceId = mountChild.getTextContent();
	                    	} else if(mountChild.getNodeName() == "targetPath") {
	                    		targetPath = mountChild.getTextContent();
	                    	} else {
	                            String msg = "The instance identifier or targetPath is not specified for the mount: " + path;
	                            log.warn(msg);
	                            continue;
	                    	}
                    	}
                    }
                    
                    pathList.add(path);
                    
                    String overwriteStr = mountElement.getAttribute("overwrite");
                    boolean overwrite = false;
                    boolean virtual = false;
                    if (overwriteStr != null) {
                        overwrite = Boolean.toString(true).equalsIgnoreCase(overwriteStr);
                        if (!overwrite) {
                            virtual = "virtual".equalsIgnoreCase(overwriteStr);
                        }
                    }
                    
                    Mount mount = new Mount();
                    mount.setPath(path);
                    mount.setOverwrite(overwrite);
                    mount.setVirtual(virtual);
                    mount.setInstanceId(instanceId);
                    mount.setTargetPath(targetPath);

                    registryContext.getMounts().add(mount);
            	}
            }
        } catch (Exception e) {
            String msg =
                    "Could not read remote instance configuration. Caused by: " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryConfigurationException(msg, e);
        }

    }

    // utility method to get setter name for a given property.
    private static String getSetterName(String varName) {

        String setterName;

        if (varName.length() == 1) {
            setterName = "set" + varName.substring(0, 1).toUpperCase();
        } else {
            setterName = "set" +
                    varName.substring(0, 1).toUpperCase() + varName.substring(1, varName.length());
        }

        return setterName;
    }

    /**
     * Object to store a handler definition
     */
    public static class HandlerDefinitionObject {

        private CustomEditManager customEditManager;
        private Element handlerConfigElement;
        private List<String> methods;
        private Handler handler;
        private Filter filter;
        private int tenantId;

        /**
         * Constructor accepting a handler configuration and the custom edit manager to use.
         *
         * @param customEditManager    the custom edit manager to use.
         * @param handlerConfigElement the handler configuration element.
         */
        public HandlerDefinitionObject(CustomEditManager customEditManager,
                                       Element handlerConfigElement) {
            this.customEditManager = customEditManager;
            this.handlerConfigElement = handlerConfigElement;
        }

        /**
         * Constructor accepting a handler configuration.
         *
         * @param handlerConfigElement the handler configuration element.
         */
        public HandlerDefinitionObject(Element handlerConfigElement) {
            this.customEditManager = null;
            this.handlerConfigElement = handlerConfigElement;
        }

        /**
         * Get methods to which this handler is engaged.
         *
         * @return array of methods
         */
        public String[] getMethods() {
            if (methods == null) {
                return null;
            }
            return methods.toArray(new String[methods.size()]);
        }

        /**
         * Gets the handler instance.
         *
         * @return the handler instance.
         */
        public Handler getHandler() {
            return handler;
        }

        /**
         * Gets the tenant identifier
         *
         * @return tenant id
         */
        public int getTenantId() {
            return tenantId;
        }

        /**
         * Gets the filter instance.
         *
         * @return the filter instance.
         */
        public Filter getFilter() {
            return filter;
        }

        /**
         * Builds a handler definition object from XML configuration
         *
         * @return the definition object
         * @throws InstantiationException    for errors in creating classes
         * @throws IllegalAccessException    for exceptions due to invisibility of methods
         * @throws NoSuchMethodException     for errors due to accessing non-existing methods.
         * @throws InvocationTargetException for errors in invoking methods or constructors.
         * @throws UserStoreException        if an error occurs in user management related
         *                                   operations.
         */
        public HandlerDefinitionObject invoke()
                throws InstantiationException, IllegalAccessException,
                NoSuchMethodException, InvocationTargetException, UserStoreException {        	
            String handlerClassName = handlerConfigElement.getAttribute("class");
            String methodsValue = handlerConfigElement.getAttribute("methods");
            String tenantIdString = handlerConfigElement.getAttribute("tenant");
        	
            tenantId = MultitenantConstants.INVALID_TENANT_ID;
            int tempTenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            // if the tenant id was found from the carbon context, it will be greater than -1. If not, it will be equal
            // to -1. Therefore, we need to check whether the carbon context had a tenant id and use it if it did.
            if (tempTenantId != MultitenantConstants.INVALID_TENANT_ID) {
                tenantId = tempTenantId;
            } else if (tenantIdString != null && !tenantIdString.isEmpty()) {
                try {
                    tenantId = Integer.parseInt(tenantIdString);
                } catch (NumberFormatException ignore) {
                    RegistryContext context = RegistryContext.getBaseInstance();
                    if (context != null && context.getRealmService() != null) {
                        try {
                            tenantId = context.getRealmService().getTenantManager().getTenantId(
                                    tenantIdString);
                        } catch (org.wso2.carbon.user.api.UserStoreException e) {
                            throw new UserStoreException(e);
                        }
                    }
                }
            }

            String[] methods;
            if (methodsValue != null && !methodsValue.isEmpty()) {
                methods = methodsValue.split(",");
                for (int i = 0; i < methods.length; i++) {
                    methods[i] = methods[i].trim();
                }
                this.methods = Arrays.asList(methods);
            }

            Class handlerClass;
            try {
            	handlerClass = Class.forName(handlerClassName); 
            } catch (ClassNotFoundException e) {
                String msg = "Could not find the handler class " + handlerClassName +
                        ". This handler will not be registered. All handler and " +
                        "filter classes should be in the class path of the Registry.";
                log.warn(msg);
                return this;
            }
            handler = (Handler) handlerClass.newInstance();

            // set configured properties of the handler object
          
            NodeList handlerProps = handlerConfigElement.getElementsByTagName("property");
            
            for( int index = 0 ; index < handlerProps.getLength() ; index++ ) {
            	Node handlerPropNode = handlerProps.item(index);
            	
            	if(handlerPropNode.getParentNode().getNodeName() == "handler") {
	            	if(handlerPropNode != null && handlerPropNode.getNodeType() == Node.ELEMENT_NODE) {
	            		Element propElement = (Element) handlerPropNode ;
	            		
	                    String propName = propElement.getAttribute("name");
	                    String propType = propElement.getAttribute("type");
	                    
	                    try {
		                    if (propType != null && "xml".equals(propType)) {
		                        String setterName = getSetterName(propName);
		                        Method setter = handlerClass.getMethod(setterName, Element.class);
		                        setter.invoke(handler, propElement);
		                    } else {
		                        String setterName = getSetterName(propName);
		                        Method setter = handlerClass.getMethod(setterName, String.class);
		                        String propValue = propElement.getTextContent();
		                        setter.invoke(handler, propValue);
		                    }
	                    } catch(NoSuchMethodException ex) {
	                    	continue ;
	                    }
	            	}
            	}
            }

            NodeList filterElements = handlerConfigElement.getElementsByTagName("filter");
            
            String filterClassName = null ;
            Element filterElement = null ;
            
            if(filterElements != null && filterElements.getLength() > 0) {
            	Node filterNode = filterElements.item(0) ;
            	if(filterNode != null && filterNode.getNodeType() == Node.ELEMENT_NODE) {
            		filterElement = (Element) filterNode ;
            		filterClassName = filterElement.getAttribute("class");
            	}
            }

            Class filterClass;
            try {
            	filterClass = Class.forName(filterClassName);
            } catch (ClassNotFoundException e) {
                String msg = "Could not find the filter class " +
                        filterClassName + ". " + handlerClassName +
                        " will not be registered. All configured handler, filter and " +
                        "edit processor classes should be in the class " +
                        "path of the Registry.";
                log.warn(msg);
                return this;
            }
            filter = (Filter) filterClass.newInstance();
            
            NodeList filterProps = filterElement.getElementsByTagName("property");
            
            for( int index = 0 ; index < filterProps.getLength() ; index++ ) {
            	Node filterPropNode = filterProps.item(index);
            	
            	if(filterPropNode.getParentNode().getNodeName() == "filter") {
	            	if(filterPropNode != null && filterPropNode.getNodeType() == Node.ELEMENT_NODE) {
	            		Element propElement = (Element) filterPropNode ;
	            		
	                    String propName = propElement.getAttribute("name");
	                    String propValue = propElement.getTextContent();
	
	                    String setterName = getSetterName(propName);
	                    
	                    try {
		                    Method setter = filterClass.getMethod(setterName, String.class);
		                    setter.invoke(filter, propValue);
	                    } catch(NoSuchMethodException ex) {
	                    	continue ;
	                    }
	            	}
            	}
            }

            if (customEditManager != null) {
            	NodeList editElements = handlerConfigElement.getElementsByTagName("edit");
            	
            	if(editElements != null && editElements.getLength() > 0) {
            		Node editNode = editElements.item(0);
            		if(editNode != null && editNode.getNodeType() == Node.ELEMENT_NODE) {
            			Element editElement = (Element) editNode ;
            			
                        String processorKey = editElement.getAttribute("processor");
                        String processorClassName = editElement.getTextContent();

                        Class editProcessorClass;
                        try {
                        	editProcessorClass = Class.forName(processorClassName); 
                        } catch (ClassNotFoundException e) {
                            String msg = "Could not find the edit processor class " +
                                    processorClassName + ". " + handlerClassName +
                                    " will not be registered. All configured handler, filter and " +
                                    "edit processor classes should be in the class " +
                                    "path of the Registry.";
                            log.warn(msg);
                            return this;
                        }
                        EditProcessor editProcessor = (EditProcessor) editProcessorClass.newInstance();

                        customEditManager.addProcessor(processorKey, editProcessor);
            		}
            	}
            }
            return this;
        }
    }
    
    // Following methods are deprecated and eventually move out of the code ---------------------------------------------------------
    
    /**
     * Obtains the registry configuration as XML element.
     *
     * @param registryContext the Registry Context used by this registry instance.
     *
     * @return AXIOM element containing registry configuration.
     */
    /*
    public static OMElement getRegistryConfigAsXML(RegistryContext registryContext) {

        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement root = factory.createOMElement("wso2registry", null);

        if (registryContext.getDefaultDataBaseConfiguration() != null) {
            OMElement currentConfigElement = factory.createOMElement("currentConfig", null);
            currentConfigElement
                    .setText(registryContext.getDefaultDataBaseConfiguration().getConfigName());
            root.addChild(currentConfigElement);
        }

        Iterator values = registryContext.getDBConfigNames();
        while (values.hasNext()) {
            DataBaseConfiguration dataBaseConfiguration = (DataBaseConfiguration) values.next();
            OMElement config = factory.createOMElement("dbConfig", null);
            OMElement url = factory.createOMElement("url", null);
            url.setText(dataBaseConfiguration.getDbUrl());
            config.addChild(url);
            OMElement userName = factory.createOMElement("userName", null);
            userName.setText(dataBaseConfiguration.getUserName());
            config.addChild(userName);
            OMElement password = factory.createOMElement("password", null);
            password.setText(dataBaseConfiguration.getResolvedPassword());
            config.addChild(password);
            OMElement driverName = factory.createOMElement("driverName", null);
            driverName.setText(dataBaseConfiguration.getDriverName());
            config.addChild(driverName);
            config.addAttribute("name", dataBaseConfiguration.getConfigName(), null);
            root.addChild(config);
        }
        return root;
    }
    */

    /**
     * Creates and initializes an aspect.
     *
     * @param configElement   the aspect configuration element.
     * @param registryContext the Registry Context used by this registry instance.
     *
     * @throws RepositoryException if anything goes wrong.
     */
    public static void initializeAspects(Element configElement, RegistryContext registryContext)
            throws RepositoryException {
    	NodeList aspectElements = configElement.getElementsByTagName("aspect");
    	
    	for( int index = 0 ; index < aspectElements.getLength() ; index++ ) {
    		Node aspectNode = aspectElements.item(index);
    		if(aspectNode != null && aspectNode.getNodeType() == Node.ELEMENT_NODE) {
    			Element aspectElement = (Element) aspectNode ;
                String name = aspectElement.getAttribute("name");
              registryContext.addAspect(name, buildAspect(aspectElement, name), MultitenantConstants.SUPER_TENANT_ID);
    		}
    	}
    }

    // common method to build an aspect
    private static Aspect buildAspect(Element aspect, String name) throws RepositoryException {
        String clazz = aspect.getAttribute("class");
        Aspect aspectInstance = null;
        try {
            if (name == null || clazz == null) {
                throw new RepositoryConfigurationException("Invalid aspect element , required " +
                        "values are missing " + aspect.toString());
            }
            Class handlerClass = Class.forName(clazz); 
            
            NodeList aspectChilderen = aspect.getChildNodes() ;
            
            for( int index = 0 ; index < aspectChilderen.getLength() ; index++ ) {
            	Node aspectNode = aspectChilderen.item(index);
            	
            	if(aspectNode != null && aspectNode.getNodeType() == Node.ELEMENT_NODE) {
	                try {
	                    Constructor constructor =
	                            handlerClass.getConstructor(Element.class);
	                    try {
	                        aspectInstance = (Aspect) constructor.newInstance(aspect);
	                    } catch (Exception e) {
	                        throw new RepositoryInitException("Couldn't instantiate", e);
	                    }
	                } catch (NoSuchMethodException e) {
	                    // Throw error because the specified config won't be used?
	                }
            	}
            }

            if (aspectInstance == null) {
                aspectInstance = (Aspect) handlerClass.newInstance();
            }
            return aspectInstance;
        } catch (Exception e) {
            String msg = "Could not initialize custom aspects. Caused by: " + e.getMessage();
            log.error(msg, e);
            throw new RepositoryInitException(msg, e);
        }
    }

    /**
     * Updates an aspect based on given configuration.
     *
     * @param configElement the aspect configuration element.
     *
     * @return Created aspectupdateAspects(dummy)
     * @throws RepositoryException if anything goes wrong.
     */
    public static Aspect updateAspects(Element configElement) throws RepositoryException {     
        NodeList aspectElements = configElement.getElementsByTagName("aspect");
        
        if(aspectElements != null && aspectElements.getLength() > 0) {
        	Node aspectNode = aspectElements.item(0);
        	if(aspectNode != null && aspectNode.getNodeType() == Node.ELEMENT_NODE) {
        		Element aspect = (Element) aspectNode ;
        		String name = aspect.getAttribute("name");
        		return buildAspect(aspect, name);
        	}
        }        
        return null;
    }
}
