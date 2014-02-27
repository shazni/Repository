/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.repository.core.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;
import org.wso2.carbon.repository.core.exceptions.RepositoryConfigurationException;

import javax.xml.parsers.DocumentBuilder;

/**
 * This class is to initialize, validate the registry related configurations in carbon.xml. This
 * method of storing registry configuration is deprecated from WSO2 Carbon 2.0.2
 */
public class RepositoryConfiguration {

    private static final Log log = LogFactory.getLog(RepositoryConfiguration.class);

    private Map<String, String> registryConfiguration = new HashMap<String, String>();

    /**
     * The registry configuration root element name.
     */
    public static final String REGISTRY_CONFIG = "Registry";

    /**
     * The element name to store registry type. Possible values are "remote" and "embedded".
     */
    public static final String TYPE = "Type";

    /**
     * The element name to store the registry username. Requires if the "Type" is selected as
     * "remote".
     */
    public static final String USERNAME = "Username";

    /**
     * The element name to store the registry password. Requires if the "Type" is selected as
     * "remote".
     */
    public static final String PASSWORD = "Password";

    /**
     * The element name to store the registry url. Requires if the "Type" is selected as "remote".
     */
    public static final String URL = "Url";

    /**
     * The element name to store the registry root. The registry chroot to be mounted.
     */
    public static final String REGISTRY_ROOT = "registryRoot";

    /**
     * The element name to store whether the . The registry chroot to be mounted.
     */
    public static final String READ_ONLY = "ReadOnly";

    /**
     * The system username of the registry.
     */
    public static final String SYSTEM_USER_NAME = "RegistrySystemUserName";

    /**
     * The value of the registry type field to mount a remote registry.
     */
    public static final String REMOTE_REGISTRY = "remote";

    /**
     * The value of the registry type field to use the embedded registry.
     */
    public static final String EMBEDDED_REGISTRY = "embedded";

    /**
     * Initialize the repository configuration with carbon.xml provided the carbon.xml path as
     * an argument.
     *
     * @param carbonXMLPath the path of the carbon.xml
     *
     * @throws RepositoryException throws if the construction failed
     */
    public RepositoryConfiguration(String carbonXMLPath) throws RepositoryException {
        try {
            File carbonXML = new File(carbonXMLPath);
            
            String carbonXMLNS = "http://wso2.org/projects/carbon/carbon.xml" ;
            
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(carbonXML);
            
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagNameNS(carbonXMLNS, REGISTRY_CONFIG);
            
            int numberOfElements;
			if((numberOfElements = nodeList.getLength()) > 0) {
            	if(numberOfElements == 1) {
            		Node node = nodeList.item(0);
            		if(node.getNodeType() == Node.ELEMENT_NODE) {
            			Element element = (Element) node ;
            			registryConfiguration.put(element.getNodeName(), element.getNodeValue());
            		}
            	} else {
            		throw new RepositoryConfigurationException("Only one registry element should exist in carbon.xml");
            	}
            }

            if (registryConfiguration.get(TYPE) == null) {
                registryConfiguration.put(TYPE, EMBEDDED_REGISTRY);
            }

            validate();
        } catch (Exception e) {
            throw new RepositoryConfigurationException("Error occurred while initialization on reading carbon.xml", e);
        }
    }

    /**
     * Validate the registry configuration.
     *
     * @throws RepositoryException throws if the validation failed.
     */
    private void validate() throws RepositoryException {
        registryConfiguration.put(SYSTEM_USER_NAME, CarbonConstants.REGISTRY_SYSTEM_USERNAME);

        String type = registryConfiguration.get(TYPE);
        
        if (type == null || type.trim().length() == 0) {
            type = EMBEDDED_REGISTRY;
        }
        
        if (REMOTE_REGISTRY.equals(type)) {
            if (registryConfiguration.get(URL) == null) {
                throw new RepositoryConfigurationException("URL not given");
            }
            
            if (registryConfiguration.get(REGISTRY_ROOT) != null && !registryConfiguration.get(REGISTRY_ROOT).startsWith("/")) {
                log.error("Invalid Registry Configuration : CHROOT must start with a /");
                throw new RepositoryConfigurationException("Invalid Registry Configuration : CHROOT must start with a /");
            }
        } else if (!EMBEDDED_REGISTRY.equals(type)) {
            throw new RepositoryConfigurationException("Unkown type");
        }
    }

    /**
     * Get a configuration value.
     *
     * @param key the configuration key.
     *
     * @return the configuration value.
     */
    public String getValue(String key) {
        return registryConfiguration.get(key);
    }

    /**
     * Get the registry type.
     *
     * @return the registry type. possible values "embedded", "remote"
     */
    public String getRegistryType() {
        return registryConfiguration.get(RepositoryConfiguration.TYPE);
    }
}
