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
    private Map<TopicPartition, PartitionProcessor> processors;
    private final AtomicBoolean shutdownRequested;
    private int wakeups = 0;
    private List<TaskStateListener> stateListeners;
    private TaskStateListener.State state;
    private long lastCommitTime;

    private class RebalanceListener implements ConsumerRebalanceListener {
        /**
         * close the running processors for the revoked partitions
         * @param revokedPartitions revoked partitions
         */
        @Override
        public void onPartitionsRevoked(Collection<TopicPartition> revokedPartitions) {
            LOGGER.info("Partitions {} revoked at the beginning of consumer rebalance for taskId={}", revokedPartitions, taskId);
            for (final TopicPartition topicPartition : revokedPartitions) {
                processors.remove(topicPartition);
            }
        }

        /**
         * create processors for newly assigned partitions
         * @param assignedPartitions newly assigned partitions
         */
        public void onPartitionsAssigned(Collection<TopicPartition> assignedPartitions) {
            LOGGER.info("Partitions {} assigned at the beginning of consumer rebalance for taskId={}", assignedPartitions, taskId);

            for (final TopicPartition topicPartition : assignedPartitions) {
                final PartitionProcessor processor = new PartitionProcessor();
                processors.putIfAbsent(topicPartition, processor);
            }
        }
    }

    /**
     * run the consumer loop till the shutdown is requested or any exception is thrown
     */
    private void runLoop() throws InterruptedException {
        while(!shutdownRequested.get()) {
            final Optional<ConsumerRecords<String, Alert>> mayBeRecords = poll();
            if (mayBeRecords.isPresent()) {
                final ConsumerRecords<String, Alert> records = mayBeRecords.get();
                if (!records.isEmpty() && !processors.isEmpty()) {
                    final Map<TopicPartition, OffsetAndMetadata> committableOffsets = new HashMap<>();
                    final Map<Integer, List<ConsumerRecord<String, Alert>>> partitionedRecords = new HashMap<>();

                    for (final ConsumerRecord<String, Alert> record : records) {
                        final List<ConsumerRecord<String, Alert>> recs =
                                partitionedRecords.computeIfAbsent(record.partition(), k -> new ArrayList<>());
                        recs.add(record);
                    }
                    partitionedRecords.forEach((partition, pRecords) -> invokeProcessor(partition, pRecords, committableOffsets));

                    // commit offsets
                    commit(committableOffsets, 0);
                }
            }
        }
    }

    public StoreTask(final int taskId,
              final KafkaConfig cfg,
              final Store store) throws InterruptedException {
        this.taskId = taskId;
        this.cfg = cfg;
        this.store = store;
        this.stateListeners = new ArrayList<>();
        this.processors = new ConcurrentHashMap<>();
        this.wakeupScheduler = Executors.newScheduledThreadPool(1);
        this.shutdownRequested = new AtomicBoolean(false);
        this.state = TaskStateListener.State.NOT_RUNNING;
        this.lastCommitTime = System.currentTimeMillis();
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

    private void invokeProcessor(final Integer partition,
                                 final List<ConsumerRecord<String, Alert>> records,
                                 final Map<TopicPartition, OffsetAndMetadata> committableOffsets) {
        final TopicPartition topicPartition = new TopicPartition(cfg.getTopic(), partition);
        final PartitionProcessor processor = processors.get(topicPartition);

        try {
            if (processor != null) {
                final Optional<OffsetAndMetadata> offsetMetadata = processor.process(records);
                offsetMetadata.ifPresent(offset -> committableOffsets.put(topicPartition, offset));
            }
        } catch (Exception ex) {
            LOGGER.error("Fail to process the alert records with error", ex);
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
                consumer.commitSync(offsets);
                lastCommitTime = currentTime;
            }
        } catch(final CommitFailedException  cfe) {
            Thread.sleep(cfg.getCommitIntervalMillis());
            // retry offset again
            commit(offsets, retryAttempt + 1);
        } catch (final Exception ex){
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

    private final class PartitionProcessor {
        long commitableOffset = -1L;
        long expectedMinOffsetInCallback = -1L;

        Optional<OffsetAndMetadata> process(final List<ConsumerRecord<String, Alert>> records) throws IOException {
            final List<AlertWithId> alerts = new ArrayList<>();
            final Long[] offset = new Long[1];
            offset[0] = -1L;

            for (final ConsumerRecord<String, Alert> record : records) {
                if (record.value() != null) {
                    final AlertWithId aId = new AlertWithId();
                    aId.setId(record.topic() + "-" + record.partition() + "-" + record.offset());
                    aId.setAlert(record.value());
                    aId.getAlert().setStartTime(truncate(aId.getAlert().getStartTime()));
                    alerts.add(aId);
                    offset[0] = Long.max(record.offset(), offset[0]);
                }
            }

            synchronized (this) {
                if (expectedMinOffsetInCallback == -1L) {
                    expectedMinOffsetInCallback = offset[0];
                }
            }

            store.write(alerts, ex -> {
                if (ex == null) {
                    synchronized (this) {
                        if (offset[0] == expectedMinOffsetInCallback) {
                            commitableOffset = expectedMinOffsetInCallback;
                        }
                    }
                } else {
                    LOGGER.error("Fail to write the alerts to store with error", ex);
                }
            });

            synchronized (this) {
                if (commitableOffset >= 0) {
                    final Optional<OffsetAndMetadata> offsetAndMetadata = Optional.of(new OffsetAndMetadata(commitableOffset));
                    commitableOffset = -1L;
                    expectedMinOffsetInCallback = -1L;
                    return offsetAndMetadata;
                } else {
                    return Optional.empty();
                }
            }
        }

        private long truncate(long startTime) {
            return startTime - (startTime % 1000);
        }
    }
}
