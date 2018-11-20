/*
 * Copyright 2018 Expedia Group, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.expedia.alertmanager.store;

import com.expedia.alertmanager.model.store.Store;
import com.expedia.alertmanager.store.config.KafkaConfig;
import com.expedia.alertmanager.store.task.StoreTask;
import com.expedia.alertmanager.store.task.TaskStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

class StorePipeline implements TaskStateListener, Closeable {
    private final static Logger LOGGER = LoggerFactory.getLogger(StorePipeline.class);

    private final KafkaConfig config;
    private final Store store;
    private final ExecutorService streamThreadExecutor;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private List<StoreTask> tasks = new ArrayList<>();

    StorePipeline(final KafkaConfig config,
                  final Store store) {
        this.config = config;
        this.store = store;
        this.streamThreadExecutor = Executors.newFixedThreadPool(config.getStreamThreads());
    }

    void start() throws InterruptedException {
        LOGGER.info("Starting the span indexing stream..");
        for(int streamId = 0; streamId < config.getStreamThreads(); streamId++) {
            final StoreTask task = new StoreTask(streamId, config, store);
            task.setStateListener(this);
            tasks.add(task);
            streamThreadExecutor.execute(task);
        }

        isStarted.set(true);
    }

    @Override
    public void onChange(final State state) {
        if(state == TaskStateListener.State.FAILED) {
            LOGGER.error("Thread state has changed to 'FAILED'");
        } else {
            LOGGER.info("Task state has changed to {}", state);
        }
    }

    @Override
    public void close() {
        if(isStarted.getAndSet(false)) {
            final Thread shutdownThread = new Thread(() -> tasks.forEach(task -> {
                try {
                    task.close();
                } catch (Exception e) {
                    LOGGER.error("Fail to close the task with error", e);
                }
            }));
            shutdownThread.setDaemon(true);
            shutdownThread.run();
        }
    }
}
