/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.state.benchmark;

import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.VoidNamespace;
import org.apache.flink.runtime.state.VoidNamespaceSerializer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.flink.state.benchmark.StateBenchmarkConstants.listValueCount;
import static org.apache.flink.state.benchmark.StateBenchmarkConstants.setupKeyCount;

/**
 * Implementation for list state benchmark testing.
 */
public class ListStateBenchmark extends StateBenchmarkBase {
    private ListState<Long> listState;
    private List<Long> dummyLists;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .verbosity(VerboseMode.NORMAL)
                .include(".*" + ListStateBenchmark.class.getSimpleName() + ".*")
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void setUp() throws Exception {
        keyedStateBackend = createKeyedStateBackend();
        listState = keyedStateBackend.getPartitionedState(
                VoidNamespace.INSTANCE,
                VoidNamespaceSerializer.INSTANCE,
                new ListStateDescriptor<>("listState", Long.class));
        dummyLists = new ArrayList<>(listValueCount);
        for (int i = 0; i < listValueCount; ++i) {
            dummyLists.add(random.nextLong());
        }
        for (int i = 0; i < setupKeyCount; ++i) {
            keyedStateBackend.setCurrentKey((long) i);
            listState.add(random.nextLong());
        }
        keyIndex = new AtomicInteger();
    }

    @Benchmark
    public void listUpdate(KeyValue keyValue) throws Exception {
        keyedStateBackend.setCurrentKey(keyValue.setUpKey);
        listState.update(keyValue.listValue);
    }

    @Benchmark
    public void listAdd(KeyValue keyValue) throws Exception {
        keyedStateBackend.setCurrentKey(keyValue.newKey);
        listState.update(keyValue.listValue);
    }

    @Benchmark
    public Iterable<Long> listGet(KeyValue keyValue) throws Exception {
        keyedStateBackend.setCurrentKey(keyValue.setUpKey);
        return listState.get();
    }

    @Benchmark
    public void listGetAndIterate(KeyValue keyValue, Blackhole bh) throws Exception {
        keyedStateBackend.setCurrentKey(keyValue.setUpKey);
        Iterable<Long> iterable = listState.get();
        for (Long value : iterable) {
            bh.consume(value);
        }
    }

    @Benchmark
    public void listAddAll(KeyValue keyValue) throws Exception {
        keyedStateBackend.setCurrentKey(keyValue.setUpKey);
        listState.addAll(dummyLists);
    }
}