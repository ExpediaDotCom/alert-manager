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
package com.expedia.alertmanager.notifier.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.google.common.base.Supplier;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import lombok.Getter;
import lombok.val;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vc.inreach.aws.request.AWSSigner;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

//FIXME - Temporarily we want to leverage elastic search based alert store to do rate limiting on alert notifications.
//All these configs we will be removed or refactored once we decide on a better solution.
@Deprecated
@Configuration
public class ElasticSearchConfig {
    public static final String SERVICE_NAME = "es";

    @Value("${alert-store-es.doctype:alerts}")
    @Getter
    private String docType;
    @Value("${alert-store-es.url}")
    private String urls;
    @Value("${alert-store-es.connection.timeout:3000}")
    private int connectionTimeout;
    @Value("${alert-store-es.max.connection.idletime:1000}")
    private int maxConnectionIdleTime;
    @Value("${alert-store-es.max.total.connection:1000}")
    private int maxTotalConnection;
    @Value("${alert-store-es.read.timeout:3000}")
    private int readTimeout;
    @Value("${alert-store-es.request.compression:false}")
    private boolean requestCompression;
    @Value("${alert-store-es.username:@null}")
    private String username;
    @Value("${alert-store-es.password:@null}")
    private String password;
    @Value("${alert-store-es.aws-iam-auth-required:false}")
    private boolean needsAWSIAMAuth;
    @Value("${alert-store-es.aws-region:@null}")
    private String awsRegion;

    @Bean
    JestClientFactory clientFactory() {
        Optional<AWSSigningRequestInterceptor> requestInterceptor =
            needsAWSIAMAuth ? getAWSRequestSignerInterceptor() : Optional.empty();

        val factory = new JestClientFactory() {
            @Override
            protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
                requestInterceptor.ifPresent(interceptor -> builder.addInterceptorLast(interceptor));
                return builder;
            }
            @Override
            protected HttpAsyncClientBuilder configureHttpClient(HttpAsyncClientBuilder builder) {
                requestInterceptor.ifPresent(interceptor -> builder.addInterceptorLast(interceptor));
                return builder;
            }
        };
        val builder =
            new HttpClientConfig.Builder(urls)
                .multiThreaded(true)
                .discoveryEnabled(false)
                .connTimeout(connectionTimeout)
                .maxConnectionIdleTime(maxConnectionIdleTime,
                    TimeUnit.SECONDS)
                .maxTotalConnection(maxTotalConnection)
                .readTimeout(readTimeout)
                .requestCompressionEnabled(requestCompression)
                .discoveryFrequency(1L, TimeUnit.MINUTES);
        if (username != null
            && password != null) {
            builder.defaultCredentials(username, password);
        }
        factory.setHttpClientConfig(builder.build());
        return factory;
    }

    private Optional<AWSSigningRequestInterceptor> getAWSRequestSignerInterceptor() {
        final Supplier<LocalDateTime> clock = () -> LocalDateTime.now(ZoneOffset.UTC);
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
        val awsSigner = new AWSSigner(credentialsProvider, awsRegion, SERVICE_NAME, clock);
        return Optional.of(new AWSSigningRequestInterceptor(awsSigner));
    }
}
