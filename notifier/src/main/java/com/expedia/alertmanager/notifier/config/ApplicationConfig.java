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
package com.expedia.alertmanager.notifier.config;

import com.expedia.alertmanager.model.Alert;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.web.client.RestTemplate;

import java.util.Properties;

@Configuration
public class ApplicationConfig {

    @Value("${kafka.consumer.bootstrap.servers}")
    private String bootstrapServer;
    @Value("${kafka.consumer.group.id}")
    private String groupId;
    @Value("${kafka.consumer.auto.offset.reset}")
    private String autoOffsetReset;
    @Value("${kafka.consumer.session.timeout.ms}")
    private String sessionTimeout;
    @Value("${kafka.consumer.heartbeat.interval.ms}")
    private String heartBeatInterval;
    @Value("${kafka.consumer.request.timeout.ms}")
    private String reqTimeout;

    /**
     * Kafka Configs.
     *
     * @return properties
     */
    public Properties getKafkaConsumerConfig() {
        final Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.bootstrapServer);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, this.groupId);
        //TODO - move deserializers to a config
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, this.autoOffsetReset);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, this.sessionTimeout);
        properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, this.heartBeatInterval);
        properties.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, this.reqTimeout);
        return properties;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Value("${subscription-search.url}")
    @Getter
    private String subscriptionSerUrl;

    @Bean
    public ConsumerFactory<String, Alert> consumerFactory() {
        return new DefaultKafkaConsumerFactory(getKafkaConsumerConfig(), new StringDeserializer(),
            new JsonDeserializer<>(Alert.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Alert> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Alert> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Value("${mail.from}")
    @Getter
    private String fromEmail;

    @Value("${mail.type}")
    @Getter
    private String mailType;

    @Value("${smtp.host}")
    @Getter
    private String smtpHost;

    @Value("${smtp.port}")
    @Getter
    private String smtpPort;

    @Value("${smtp.username}")
    @Getter
    private String smtpUsername;

    @Value("${smtp.password}")
    @Getter
    private String smtpPassword;

    @Bean("freemarkerConfig")
    public FreeMarkerConfigurationFactoryBean getFreeMarkerConfiguration() {
        FreeMarkerConfigurationFactoryBean bean = new FreeMarkerConfigurationFactoryBean();
        bean.setTemplateLoaderPath("classpath:/templates/");
        return bean;
    }

    @Value("${slack.url}")
    @Getter
    private String slackUrl;

    @Value("${slack.token}")
    @Getter
    private String slackToken;

    @Value("${rate-limit.enabled:false}")
    @Getter
    private boolean rateLimitEnabled;

    @Value("${rate-limit.value:0}")
    @Getter
    private long rateLimit;

    //FIXME - This is a temp config.
    //AM-notifier app shouldn't directly invoke alert store instead it should use AM-service to query alerts
    @Value("${alert-store-es.url}")
    @Getter
    private String alertStoreEsUrl;
}
