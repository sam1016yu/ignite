/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.ignite.internal.processors.cache.mvcc;

import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.GridTopic;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.mvcc.msg.MvccRecoveryFinishedMessage;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.GridTestUtils;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;
import org.junit.Test;

/**
 * Test checks a collecting unused MVCC structure, that will be able to create GC pressure.
 */
public class MvccStructuresOverheadTest extends GridCommonAbstractTest {
    //TODO: need to better understand client restart logic and find place to measure memory usage
    
    /** {@inheritDoc} */
    @Override protected List<String> additionalRemoteJvmArgs() {
        return Arrays.asList("-Xmx10240m", "-Xms10240m");
    }

    private long memoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();     
    }

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        return super.getConfiguration(igniteInstanceName)
            .setCacheConfiguration(new CacheConfiguration(DEFAULT_CACHE_NAME)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC));
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        super.beforeTest();

        stopAllGrids();
    }

    /**
     * Starts grid with ATOMIC cache.
     *
     * @throws Exception If failed.
     */
    @Test
    public void testWithoutMvcc() throws Exception {
        for (int i = 10; i <= 100; i+=10){
            log.warning("####clents restarts :" + i);
            System.gc();
            long mem1 = memoryUsage();
            restartClients(i);
            long mem2 = memoryUsage();
            log.warning("####Memory usage: " + (mem2 - mem1));
            stopAllGrids();
        }
           
    }



    /**
     * Starts cluster and restarts several clients over it.
     *
     * @throws Exception If failed.
     */
    private void restartClients(int client_restarts) throws Exception {
        IgniteEx ignite = startGrid(0);

        AtomicBoolean mvccMessageTranslated = new AtomicBoolean();

        ignite.context().io().addMessageListener(GridTopic.TOPIC_CACHE_COORDINATOR, (nodeId, msg, plc) -> {
            if (msg instanceof MvccRecoveryFinishedMessage)
                mvccMessageTranslated.set(true);
        });


        for (int i = 0; i < client_restarts; i++) {
            
            IgniteEx client = startClientGrid(1);

            IgniteCache cache = client.cache(DEFAULT_CACHE_NAME);

            cache.put(i, i);

            client.close();

            GridTestUtils.waitForCondition(mvccMessageTranslated::get, 10_000);
            mvccMessageTranslated.compareAndSet(true, false);

        }
    }
}
