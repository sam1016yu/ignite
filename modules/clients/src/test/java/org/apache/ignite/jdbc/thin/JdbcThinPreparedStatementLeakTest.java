/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.jdbc.thin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Arrays;
import java.util.List;
import org.apache.ignite.IgniteJdbcThinDriver;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.util.GridDebug;
import org.junit.Test;

/**
 * Prepared statement leaks test.
 */
@SuppressWarnings({"ThrowableNotThrown","StatementWithEmptyBody"})
public class JdbcThinPreparedStatementLeakTest extends JdbcThinAbstractSelfTest {
    /** URL. */
    private static final String URL = "jdbc:ignite:thin://127.0.0.1/";

    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String igniteInstanceName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(igniteInstanceName);

        return cfg;
    }

      /** {@inheritDoc} */
    @Override protected List<String> additionalRemoteJvmArgs() {
        return Arrays.asList("-Xmx2048m", "-Xms2048m");
    }

    /**
     * @throws Exception If failed.
     */
    @Test
    public void test() throws Exception {

        for (int iters = 10000; iters <= 50000;iters+=10000){
            startGrid();
            try (Connection conn = new IgniteJdbcThinDriver().connect(URL, new Properties())) {
                System.gc();
                long mem1 = memoryUsage();
                
                createClosePreparedStatement(conn, iters);

                long mem2 = memoryUsage();

                log.warning("####Iters:"+iters);
                log.warning("####Mem:"+(mem2-mem1));

            }
            stopAllGrids();
        }

    }

    /**
     * @param conn JDBC Connection.
     * @param iters Iterations count.
     * @throws SQLException On error.
     */
    private void createClosePreparedStatement(Connection conn, int iters) throws SQLException {
        for (int i = 0; i < iters; ++i) {
            try (PreparedStatement st = conn.prepareStatement("select 1")) {
                ResultSet rs = st.executeQuery();

                while (rs.next()) {
                    // No-op.
                }

                rs.close();
            }
        }
    }

    private long memoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();     
    }
}