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

package com.expedia.alertmanager.store.backend;

import com.expedia.alertmanager.model.Alert;
import com.expedia.alertmanager.model.store.AlertWithId;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.easymock.EasyMock.*;

public class WriterUnitTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(Reader.class);
    private RestHighLevelClient mockClient;
    private Capture<BulkRequest> capturedRequest;
    private Capture<ActionListener<BulkResponse>> capturedListener;
    private Writer writer;

    @Before
    public void beforeTest() {
        this.mockClient = EasyMock.mock(RestHighLevelClient.class);
        this.capturedRequest = newCapture();
        this.capturedListener = newCapture();
        mockClient.bulkAsync(capture(capturedRequest),  capture(capturedListener));

        final Map<String, Object> config = Collections.singletonMap("max.retries", 1);
        this.writer = new Writer(mockClient, config, "alerts", LOGGER);
    }

    @After
    public void afterTest() {
        verify(this.mockClient);
    }

    @Test
    public void writeWithRetryTest() throws IOException {
        EasyMock.expectLastCall().andAnswer(() -> {
            final Writer.BulkActionListener l = (Writer.BulkActionListener)capturedListener.getValue();

            //throw exception for the first time and then send successful response
            if (l.getRetryCount() == 0) {
                l.onFailure(new RuntimeException("fail to index"));
            } else {
                l.onResponse(buildBulkResponse());
            }
            return null;
        }).times(2);

        EasyMock.replay(mockClient);

        final Boolean[] expectWriteCallback = new Boolean[] { false };
        writer.write(Collections.singletonList(createAlertWithId()), ex -> {
            expectWriteCallback[0] = true;
            if (ex != null) {
                Assert.fail(ex.getMessage());
            }
        });

        applyAsserts(expectWriteCallback[0]);
    }

    @Test
    public void writeWithFailureTest() throws IOException {
        EasyMock.expectLastCall().andAnswer(() -> {
            capturedListener.getValue().onFailure(new RuntimeException("fail to index"));
            return null;
        }).times(2);

        EasyMock.replay(mockClient);

        final Boolean[] expectWriteCallback = new Boolean[] { false };
        writer.write(Collections.singletonList(createAlertWithId()), ex -> {
            expectWriteCallback[0] = true;
            if (ex == null) {
                Assert.fail("runtime indexing exception is expected");
            } else {
                Assert.assertEquals("expected exception", ex.getMessage(), "fail to index");
            }
        });

        applyAsserts(expectWriteCallback[0]);
    }

    @Test
    public void writeWithSuccessTest() throws IOException {
        EasyMock.expectLastCall().andAnswer(() -> {
            capturedListener.getValue().onResponse(buildBulkResponse());
            return null;
        });
        EasyMock.replay(mockClient);

        final Boolean[] expectWriteCallback = new Boolean[] { false };
        writer.write(Collections.singletonList(createAlertWithId()), ex -> {
            expectWriteCallback[0] = true;
            if (ex != null) {
                Assert.fail(ex.getMessage());
            }
        });

        applyAsserts(expectWriteCallback[0]);
    }

    private void applyAsserts(boolean expectWriteCallback) {
        Assert.assertEquals(expectWriteCallback, true);
        final List<DocWriteRequest> requests = capturedRequest.getValue().requests();
        Assert.assertEquals(requests.size(), 1);
        final IndexRequest request = (IndexRequest)requests.get(0);
        Assert.assertEquals("id should match", "alerts-0-1", request.id());
        Assert.assertEquals("index name should match", expectedIndexName(), request.index());

        final Map<String, Object> indexBody = request.sourceAsMap();
        Assert.assertEquals("name should match", indexBody.get(ElasticSearchStore.NAME), "a1");
        Assert.assertEquals("name should match", indexBody.get(ElasticSearchStore.OBSERVED_VALUE), "5");
        Assert.assertEquals("name should match", indexBody.get(ElasticSearchStore.EXPECTED_VALUE), "10");
        Assert.assertEquals("name should match", ((Map<String, String>)indexBody.get(ElasticSearchStore.LABELS)).get("service"), "svc1");
        Assert.assertEquals("name should match", ((Map<String, String>)indexBody.get(ElasticSearchStore.ANNOTATIONS)).get("annotated_key"), "annotated_value");
    }

    private String expectedIndexName() {
        return "alerts-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    private AlertWithId createAlertWithId() {
        final Alert alert = new Alert();
        alert.setName("a1");
        alert.setStartTime(System.currentTimeMillis());
        alert.setObservedValue("5");
        alert.setExpectedValue("10");

        final Map<String, String> labels = Collections.singletonMap("service", "svc1");
        alert.setLabels(labels);

        final Map<String, String> annotations = Collections.singletonMap("annotated_key", "annotated_value");
        alert.setAnnotations(annotations);

        AlertWithId aId = new AlertWithId();
        aId.setAlert(alert);
        aId.setId("alerts-0-1");
        return aId;
    }

    private BulkResponse buildBulkResponse() {
        final BulkItemResponse itemResponse = new BulkItemResponse(1, DocWriteRequest.OpType.CREATE, (BulkItemResponse.Failure)null);
        return new BulkResponse(new BulkItemResponse[] {itemResponse}, 1000);
    }
}
