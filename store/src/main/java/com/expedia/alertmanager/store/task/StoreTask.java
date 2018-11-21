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

package com.expedia.alertmanager.store.task;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.store.AlertWithId;
import com.expedia.alertmanager.model.store.Store;
import com.expedia.alertmanager.model.store.WriteCallback;
import com.expedia.alertmanager.store.config.KafkaConfig;
import com.expedia.alertmanager.store.serde.AlertDeserializer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreTask implements Runnable, Closeable {
    private final static Logger LOGGER = LoggerFactory.getLogger(StoreTask.class);

    private final int taskId;
    private final KafkaConfig cfg;
    private final Store store;

    private final KafkaConsumer<String, Alert> consumer;
    private final ScheduledExecutorService wakeupScheduler;
    private final AtomicBoolean shutdownRequested;
    private final ArrayBlockingQueue<StoreWriteCallback> requestedCallbacks;
    private int wakeups = 0;
    private List<TaskStateListener> stateListeners;
    private TaskStateListener.State state;
    private long lastCommitTime;
    private volatile Map<TopicPartition, OffsetAndMetadata> committableOffsets = Collections.emptyMap();

    private class RebalanceListener implements ConsumerRebalanceListener {
        /**
         * close the running processors for the revoked partitions
         * @param revokedPartitions revoked partitions
         */
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
            LOGGER.info("Partitions {} revoked at the beginning of consumer rebalance for taskId={}", revokedPartitions, taskId);
        }

        /**
         * create processors for newly assigned partitions
         * @param assignedPartitions newly assigned partitions
         */
        public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
            LOGGER.info("Partitions {} assigned at the beginning of consumer rebalance for taskId={}", assignedPartitions, taskId);
        }
    }

    public StoreTask(final int taskId,
                     final KafkaConfig cfg,
                     final Store store,
                     final int parallelWrites) throws InterruptedException {
        this.taskId = taskId;
        this.cfg = cfg;
        this.store = store;
        this.stateListeners = new ArrayList<>();
        this.wakeupScheduler = Executors.newScheduledThreadPool(1);
        this.shutdownRequested = new AtomicBoolean(false);
        this.state = TaskStateListener.State.NOT_RUNNING;
        this.lastCommitTime = System.currentTimeMillis();
        this.requestedCallbacks = new ArrayBlockingQueue<>(parallelWrites);

        this.consumer = createConsumer(taskId, cfg);
        consumer.subscribe(Collections.singletonList(cfg.getTopic()), new RebalanceListener());
    }

    @Override
    public void run() {
        LOGGER.info("Starting stream processing thread with id={}", taskId);
        try {
            updateStateAndNotify(TaskStateListener.State.RUNNING);
            runLoop();
        } catch(InterruptedException ie) {
            LOGGER.error("This stream task with taskId={} has been interrupted", taskId, ie);
        } catch(Exception ex) {
            if (!shutdownRequested.get()) updateStateAndNotify(TaskStateListener.State.FAILED);
            // may be logging the exception again for kafka specific exceptions, but it is ok.
            LOGGER.error("Stream application faced an exception during processing for taskId={}: ", taskId, ex);
        } finally {
            consumer.close(cfg.getCloseTimeoutMillis(), TimeUnit.MILLISECONDS);
            updateStateAndNotify(TaskStateListener.State.CLOSED);
        }
    }

    @Override
    public void close() {
        consumer.close(cfg.getCloseTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    public void setStateListener(final TaskStateListener listener) {
        this.stateListeners.add(listener);
    }

    /**
     * run the consumer loop till the shutdown is requested or any exception is thrown
     */
    private void runLoop() throws InterruptedException, IOException {
        while(!shutdownRequested.get()) {
            final Optional<ConsumerRecords<String, Alert>> mayBeRecords = poll();
            if (mayBeRecords.isPresent()) {
                final ConsumerRecords<String, Alert> records = mayBeRecords.get();
                final Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
                final List<AlertWithId> saveAlerts = new ArrayList<>();

                for (final ConsumerRecord<String, Alert> record : records) {
                    saveAlerts.add(transform(record));
                    updateOffset(offsets, record);
                }

                final StoreWriteCallback callback = new StoreWriteCallback(offsets);
                synchronized (requestedCallbacks) {
                    requestedCallbacks.put(callback);
                }
                store.write(saveAlerts, callback);

                // commit offsets
                commit(committableOffsets, 0);
            }
        }
    }

    private class StoreWriteCallback implements WriteCallback {
        private final Map<TopicPartition, OffsetAndMetadata> offsets;
        private volatile boolean callbackReceived = false;

        StoreWriteCallback(Map<TopicPartition, OffsetAndMetadata> committableOffsets) {
            this.offsets = committableOffsets;
        }

        @Override
        public void onComplete(Exception ex) {
            if (ex != null) {
                // dont commit anything if exception happens
                LOGGER.error("Fail to write to elastic search after all retries with error", ex);
                updateStateAndNotify(TaskStateListener.State.FAILED);
            } else {
                // commit offsets
                synchronized (requestedCallbacks) {
                    StoreWriteCallback committableCallback = null;
                    for (final StoreWriteCallback cbk : requestedCallbacks) {
                        if (cbk == this) {
                            cbk.callbackReceived = true;
                        }
                    }
                    while(true) {
                        final StoreWriteCallback cbk = requestedCallbacks.peek();
                        if (cbk != null && cbk.callbackReceived) {
                            committableCallback = requestedCallbacks.poll();
                        } else {
                            break;
                        }
                    }
                    if (committableCallback != null) {
                        committableOffsets = committableCallback.offsets;
                    }
                }
            }
        }
    }
    private void updateOffset(final Map<TopicPartition, OffsetAndMetadata> committableOffsets,
                                         final ConsumerRecord<String, Alert> record) {
        final TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        final OffsetAndMetadata offset = committableOffsets.get(topicPartition);
        if (offset == null) {
            committableOffsets.put(topicPartition, new OffsetAndMetadata(record.offset()));
        } else {
            if (offset.offset() < record.offset()) {
                committableOffsets.put(topicPartition, new OffsetAndMetadata(record.offset()));
            }
        }
    }

    private AlertWithId transform(ConsumerRecord<String, Alert> record) {
        final AlertWithId aId = new AlertWithId();
        aId.setId(record.topic() + "-" + record.partition() + "-" + record.offset());
        aId.setAlert(record.value());
        aId.getAlert().setStartTime(truncate(aId.getAlert().getStartTime()));
        return aId;
    }

    private List<TaskStateListener> getStateListeners() {
        return stateListeners;
    }

    private void updateStateAndNotify(final TaskStateListener.State newState) {
        if (state != newState) {
            state = newState;

            // invoke listeners for any state change
            getStateListeners().forEach(listener -> listener.onChange(state));
        }
    }

    /**
     * before requesting consumer.poll(), schedule a wakeup call as poll() may hang due to network errors in kafka
     * if the poll() doesnt return after a timeout, then wakeup the consumer.
     *
     * @return consumer records from kafka
     */
    private Optional<ConsumerRecords<String, Alert>> poll() {
        final ScheduledFuture wakeupCall = scheduleWakeup();

        try {
            final ConsumerRecords<String, Alert> records = consumer.poll(cfg.getPollTimeoutMillis());
            wakeups = 0;
            return Optional.of(records);
        } catch (WakeupException we) {
            handleWakeupError(we);
            return Optional.empty();
        } finally {
            try {
                wakeupCall.cancel(true);
            } catch (Exception ex) {
                LOGGER.error("kafka consumer poll has failed with error", ex);
            }
        }
    }

    /**
     * commit the offset to kafka with a retry logic
     * @param offsets map of offsets for each topic partition
     * @param retryAttempt current retry attempt
     */
    private void commit(final Map<TopicPartition, OffsetAndMetadata> offsets, int retryAttempt) throws InterruptedException {
        try {
            final Long currentTime = System.currentTimeMillis();
            if (currentTime - lastCommitTime >= cfg.getCommitIntervalMillis() &&
                    !offsets.isEmpty() &&
                    retryAttempt <= cfg.getMaxCommitRetries()) {
                LOGGER.info("committing the offsets now for taskId {}", taskId);
                consumer.commitSync(offsets);
                lastCommitTime = currentTime;
            }
        } catch(final CommitFailedException  cfe) {
            LOGGER.error("Fail to commit offset to kafka with error", cfe);
            Thread.sleep(cfg.getCommitIntervalMillis());
            // retry offset again
            commit(offsets, retryAttempt + 1);
        } catch (final Exception ex) {
            LOGGER.error("Fail to commit the offsets with exception", ex);
        }
    }

    private ScheduledFuture scheduleWakeup() {
        return wakeupScheduler.schedule(consumer::wakeup, cfg.getWakeupTimeoutInMillis(), TimeUnit.MILLISECONDS);
    }

    private void handleWakeupError(final WakeupException we) {
        if (we == null) {
            return;
        }
        // if in shutdown phase, then do not swallow the exception, throw it to upstream
        if (shutdownRequested.get()) throw we;
        wakeups = wakeups + 1;
        if (wakeups == cfg.getMaxWakeups()) {
            LOGGER.error("WakeupException limit exceeded, throwing up wakeup exception for taskId={}.", taskId, we);
            throw we;
        } else {
            LOGGER.error("Consumer poll took more than {} ms for taskId={}, wakeup attempt={}!. Will try poll again!",
                    cfg.getWakeupTimeoutInMillis(), wakeups, taskId);
        }
    }

    private static KafkaConsumer<String, Alert> createConsumer(final Integer taskId, final KafkaConfig cfg) {
        final Properties props = new Properties();
        cfg.getConsumer().forEach((key, value) -> props.setProperty(key.toString(), value.toString()));
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, taskId.toString());
        return new KafkaConsumer<>(props, new StringDeserializer(), new AlertDeserializer());
    }

    private long truncate(long startTime) {
        return (startTime/1000) * 1000;
    }
}
