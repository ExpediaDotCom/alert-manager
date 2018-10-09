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
package com.expedia.alertmanager.dao;

import com.expedia.alertmanager.entity.Subscription;
import com.expedia.alertmanager.entity.SubscriptionType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RunWith(SpringRunner.class)
@DataJpaTest
public class SubscriptionRepositoryTests {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    public void whenFindByMetricIdAndModelId_thenReturnSubscriptions() {

        //init
        SubscriptionType email = new SubscriptionType("email");
        entityManager.persist(email);

        // given
        Subscription subscription = new Subscription("metricId", "modelId",
            email, "email@email.com");
        entityManager.persist(subscription);
        entityManager.flush();

        // when
        Subscription found =
            subscriptionRepository.findByMetricIdAndModelId("metricId", "modelId").get(0);

        // then
        assertThat(found.getModelId())
            .isEqualTo(subscription.getModelId());
        assertThat(found.getMetricId())
            .isEqualTo(subscription.getMetricId());
    }
}
