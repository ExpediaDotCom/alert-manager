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
package com.expedia.alertmanager.service.conf;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.google.common.base.Supplier;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import lombok.Data;
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

@Configuration
@Data
public class ElasticSearchConfig {

    public static final String SERVICE_NAME = "es";
    @Value("${es.index.name}")
    private String indexName;
    @Value("${es.create.index.if.not.found:true}")
    private boolean createIndexIfNotFound;
    @Value("${es.doctype}")
    private String docType;
    @Value("${es.urls}")
    private String urls;
    @Value("${es.connection.timeout}")
    private int connectionTimeout;
    @Value("${es.max.connection.idletime}")
    private int maxConnectionIdleTime;
    @Value("${es.max.total.connection}")
    private int maxTotalConnection;
    @Value("${es.read.timeout}")
    private int readTimeout;
    @Value("${es.request.compression:false}")
    private boolean requestCompression;
    @Value("${es.username:@null}")
    private String username;
    @Value("${es.password:@null}")
    private String password;
    @Value("${es.aws-iam-auth-required:false}")
    private boolean needsAWSIAMAuth;
    @Value("${es.aws-region:@null}")
    private String awsRegion;

    @Bean
    JestClientFactory clientFactory(ElasticSearchConfig elasticSearchConfig) {
        Optional<AWSSigningRequestInterceptor> requestInterceptor =
            needsAWSIAMAuth ? getAWSRequestSignerInterceptor() : Optional.empty();

        final JestClientFactory factory = new JestClientFactory() {
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

        HttpClientConfig.Builder builder =
            new HttpClientConfig.Builder(elasticSearchConfig.getUrls())
                .multiThreaded(true)
                .discoveryEnabled(false)
                .connTimeout(elasticSearchConfig.getConnectionTimeout())
                .maxConnectionIdleTime(elasticSearchConfig.getMaxConnectionIdleTime(),
                    TimeUnit.SECONDS)
                .maxTotalConnection(elasticSearchConfig.getMaxTotalConnection())
                .readTimeout(elasticSearchConfig.getReadTimeout())
                .requestCompressionEnabled(elasticSearchConfig.isRequestCompression())
                .discoveryFrequency(1L, TimeUnit.MINUTES);

        if (elasticSearchConfig.getUsername() != null
            && elasticSearchConfig.getPassword() != null) {
            builder.defaultCredentials(elasticSearchConfig.getUsername(), elasticSearchConfig.getPassword());
        }

        factory.setHttpClientConfig(builder.build());
        return factory;
    }

    private Optional<AWSSigningRequestInterceptor> getAWSRequestSignerInterceptor() {
        final Supplier<LocalDateTime> clock = () -> LocalDateTime.now(ZoneOffset.UTC);
        AWSCredentialsProvider credentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AWSSigner awsSigner = new AWSSigner(credentialsProvider, awsRegion, SERVICE_NAME, clock);
        return Optional.of(new AWSSigningRequestInterceptor(awsSigner));
    }

}
