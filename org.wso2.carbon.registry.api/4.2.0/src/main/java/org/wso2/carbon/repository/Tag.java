/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
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
package org.wso2.carbon.repository;

/**
 * @deprecated
 * 
 * This is a social aspect and should not be in repository. Therefore, will be removed 
 * from kernel
 * 
 * Represents a tag and its meta-data. Instances of this class is returned from the Registry
 * interface, when tags for a given resource path is queried.
 */
@Deprecated
public class Tag {

    /**
     * Name of the tag. This may contain spaces.
     */
    protected String tagName;
	
	private static final int DEFAULT_CATEGORY = 1;

    private static final int LIMIT_ONE = 2;
    private static final int LIMIT_TWO = 8;
    private static final int LIMIT_THREE = 20;
    private static final int LIMIT_FOUR = 50;
	
	/**
     * Tags are categorized according to the tag count. Then the category indicates the validity
     * of the tag. the {@link #setTagCount} method explains how category is calculated based on
     * the tag count. This is used in the WSO2 Registry web UI to generate the tag cloud.
     */
    private int category = DEFAULT_CATEGORY;

    /**
     * Number of taggings done using this tag. If a Tag object is returned as a result of a
     * Registry.getTags(String resourcePath) method, then this contains the number of users who
     * tagged the given resource using this tag.
     */
    protected long tagCount;

    /**
     * Get the tag name.
     *
     * @return the tag name.
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Set the tag name.
     *
     * @param tagName the tag name.
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * Set the the number of times the same tag has been used.
     *
     * @param tagCount the number of times the same tag has been used.
     */
    public void setTagCount(long tagCount) {
        this.tagCount = tagCount;
		
		category = 1;
        if (tagCount > LIMIT_ONE) {
            category++;
        }
        if (tagCount > LIMIT_TWO) {
            category++;
        }
        if (tagCount > LIMIT_THREE) {
            category++;
        }
        if (tagCount > LIMIT_FOUR) {
            category++;
        }
    }

    /**
     * Get the tag count.
     *
     * @return the tag count.
     */
    public long getTagCount() {
        return tagCount;
    }
	
	    /**
     * Get the category.
     *
     * @return the tag category.
     */
    public int getCategory() {
        return category;
    }

    /**
     * Set the category.
     *
     * @param category the category.
     */
    public void setCategory(int category) {
        this.category = category;
    }
}
