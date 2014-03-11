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

package org.wso2.carbon.registry.core.test.performance;

import org.wso2.carbon.repository.api.Repository;
import org.wso2.carbon.repository.api.Resource;
import org.wso2.carbon.repository.api.exceptions.RepositoryException;

public class Worker4 extends Worker {

    public Worker4(String threadName, int iterations, Repository registry) {
        super(threadName, iterations, registry);
    }

    public void run() {
        // do many registry operations in a loop
        long time1 = System.nanoTime();

        int i = 0;
        
        try {
            long timePerThread = 0;
            for (i = 0; i < iterations; i++) {
                long start = System.nanoTime();

                String rPath = basePath + "/original/r" + i;
                Resource r = registry.newResource();
                String content = "this is the content of first test resource.";
                r.setContent(content.getBytes());
                r.setProperty("p1", "v1");
                r.setProperty("p2", "v2");
                registry.put(rPath, r);
                Thread.yield();

                // read and update the resource

                registry.resourceExists(rPath);
                Resource rr = registry.get(rPath);
                String content2 = "this is the modified content.";
                rr.setContent(content2);
                rr.setProperty("p1", "vvv1");
                registry.put(rPath, rr);
                Thread.yield();

                // copy the resource
                String pathToCopy = basePath + "/copy/r" + i;
                registry.copy(rPath, pathToCopy);
                Thread.yield();
//
                String[] versions = registry.getVersions(rPath);
                
                if (versions.length > 0) {
                    Resource rVersion = registry.get(versions[0]);
                    registry.restoreVersion(versions[0]);
                }
                
                Thread.yield();

                registry.delete(rPath);
                Thread.yield();
                long end = System.nanoTime();
                timePerThread += (end - start);

                long averageTime = (end - start) / ((i + 1) * 1000000);
                System.out.println("CSV," + threadName + "," + "iteration," + i + 1 + "," + averageTime);
            }
            
            long averageTime = timePerThread / (iterations * 1000000);
            System.out.println("CSV-avg-time-per-thread," + threadName + "," + averageTime);
        } catch (RepositoryException e) {
            System.out.println("Failed to perform registry operations. Thread " + threadName + " failed at iteration " + i + ". " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Failed the thread " + threadName + " at iteration " + i + ". " + e.getMessage());
            e.printStackTrace();
        }

        long time2 = System.nanoTime();
        long elapsedTime = (time2 - time1) / (1000000 * iterations);
        System.out.println("AVG-TIME-PER-THREAD," + threadName + "," + elapsedTime);

        System.out.println("=================== COMPLETED THE THREAD " + threadName + "===================");
    }
}
