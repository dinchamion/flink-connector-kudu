/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.connector.kudu.table;

import org.apache.flink.connector.kudu.connector.KuduTableInfo;
import org.apache.flink.connector.kudu.connector.KuduTestBase;
import org.apache.flink.connector.kudu.connector.reader.KuduReaderConfig;
import org.apache.flink.connector.kudu.table.function.lookup.KuduRowDataLookupFunction;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Collector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Unit Tests for {@link KuduRowDataLookupFunction}. */
public class KuduRowDataLookupFunctionTest extends KuduTestBase {
    public static final String INPUT_TABLE = "books";
    public static KuduTableInfo tableInfo;

    @BeforeEach
    public void init() {
        tableInfo = booksTableInfo(INPUT_TABLE, true);
        setUpDatabase(tableInfo);
    }

    @AfterEach
    public void clean() {
        KuduTableInfo tableInfo = booksTableInfo(INPUT_TABLE, true);
        cleanDatabase(tableInfo);
    }

    @Test
    public void testLookup() throws Exception {
        KuduRowDataLookupFunction lookupFunction = buildRowDataLookupFunction(new String[] {"id"});

        ListOutputCollector collector = new ListOutputCollector();
        lookupFunction.setCollector(collector);

        lookupFunction.open(null);

        lookupFunction.eval(1001);

        lookupFunction.eval(1002);

        lookupFunction.eval(1003);

        List<String> result =
                new ArrayList<>(collector.getOutputs())
                        .stream().map(RowData::toString).sorted().collect(Collectors.toList());

        assertNotNull(result);
    }

    private KuduRowDataLookupFunction buildRowDataLookupFunction(String[] keyNames) {
        KuduReaderConfig config =
                KuduReaderConfig.Builder.setMasters(getMasterAddress()).setRowLimit(10).build();
        return new KuduRowDataLookupFunction(
                keyNames,
                tableInfo,
                config,
                Arrays.asList("id", "title", "author", "price", "quantity"));
    }

    private static final class ListOutputCollector implements Collector<RowData> {

        private final List<RowData> output = new ArrayList<>();

        @Override
        public void collect(RowData row) {
            this.output.add(row);
        }

        @Override
        public void close() {}

        public List<RowData> getOutputs() {
            return output;
        }
    }
}
