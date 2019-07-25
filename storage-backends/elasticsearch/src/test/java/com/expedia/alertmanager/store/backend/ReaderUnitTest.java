/*
 * Copyright 2018-2019 Expedia Group, Inc.
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

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static org.easymock.EasyMock.*;

public class ReaderUnitTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(Reader.class);
    private RestHighLevelClient mockClient;
    private Capture<SearchRequest> capturedRequest;
    private Capture<ActionListener<SearchResponse>> capturedListener;
    private Reader reader;
    private final long timestamp = 1542882971288L;

    @Before
    public void beforeTest() {
        this.mockClient = EasyMock.mock(RestHighLevelClient.class);
        this.capturedRequest = newCapture();
        this.capturedListener = newCapture();

        mockClient.searchAsync(capture(capturedRequest),  capture(capturedListener));
        this.reader = new Reader(mockClient, Collections.emptyMap(), "alerts", LOGGER);
    }

    @After
    public void afterTest() {
        verify(this.mockClient);
    }

    @Test
    public void readFailureTest() {
        EasyMock.expectLastCall().andAnswer(() -> {
            capturedListener.getValue().onFailure(new RuntimeException("fail to read from elastic!"));
            return null;
        });

        EasyMock.replay(mockClient);
        final Boolean[] expectWriteCallback = new Boolean[] { false };
        final Map<String, String> labels = Collections.singletonMap("service", "svc1");
        this.reader.read(labels, timestamp - 10000, timestamp, 100, (alerts, ex) -> {
            if(ex.getMessage() == "None") {
                Assert.fail("no exception is expected in searching alerts");
            }
            expectWriteCallback[0] = true;
        });
        applyAsserts(expectWriteCallback[0]);
    }

    @Test
    public void readAlertsTest() {
        EasyMock.expectLastCall().andAnswer(() -> {
            capturedListener.getValue().onResponse(buildSearchResponse());
            return null;
        });
        EasyMock.replay(mockClient);

        final Boolean[] expectWriteCallback = new Boolean[] { false };

        final Map<String, String> labels = Collections.singletonMap("service", "svc1");
        this.reader.read(labels, timestamp - 10000, timestamp, 100, (alerts, ex) -> {
            if(ex.getMessage() != "None") {
                Assert.fail("no exception is expected in searching alerts");
            }
            Assert.assertEquals(alerts.size(), 1);
            expectWriteCallback[0] = true;
        });
        applyAsserts(expectWriteCallback[0]);
    }

    private void applyAsserts(final boolean expectWriteCallback) {
        Assert.assertEquals(expectWriteCallback, true);
        final SearchRequest req = capturedRequest.getValue();
        Assert.assertEquals("search indices should match", "alerts*", req.indices()[0]);
        Assert.assertEquals("request query should match", "{\n" +
                "  \"size\" : 100,\n" +
                "  \"timeout\" : \"15000ms\",\n" +
                "  \"query\" : {\n" +
                "    \"bool\" : {\n" +
                "      \"must\" : [\n" +
                "        {\n" +
                "          \"match\" : {\n" +
                "            \"labels.service\" : {\n" +
                "              \"query\" : \"svc1\",\n" +
                "              \"operator\" : \"OR\",\n" +
                "              \"prefix_length\" : 0,\n" +
                "              \"max_expansions\" : 50,\n" +
                "              \"fuzzy_transpositions\" : true,\n" +
                "              \"lenient\" : false,\n" +
                "              \"zero_terms_query\" : \"NONE\",\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"range\" : {\n" +
                "            \"startTime\" : {\n" +
                "              \"from\" : 1542882961288,\n" +
                "              \"to\" : 1542882971288,\n" +
                "              \"include_lower\" : false,\n" +
                "              \"include_upper\" : false,\n" +
                "              \"boost\" : 1.0\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ],\n" +
                "      \"adjust_pure_negative\" : true,\n" +
                "      \"boost\" : 1.0\n" +
                "    }\n" +
                "  }\n" +
                "}", req.source().toString());
    }

    private SearchResponse buildSearchResponse() {
        return new SearchResponse(new SearchResponseSections(
                new SearchHits(new SearchHit[]{new SearchHit(1)}, 0L, 0),
                null,null, false, false, null, 0), "", 0, 0, 0, 0L, null);
    }
}
