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
import com.expedia.alertmanager.entity.SubscriptionMetricDetectorMapping;
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
    private SubscriptionMetricDetectorMappingRepository subscriptionMetricDetectorMappingRepo;

    @Test
    public void whenFindByMetricIdAndModelId_thenReturnSubscriptions() {

        // given
        Subscription subscription = new Subscription(Subscription.EMAIL_TYPE, "email@email.com");
        SubscriptionMetricDetectorMapping subscriptionMetricDetectorMapping = new SubscriptionMetricDetectorMapping(
            "metricId", "detectorId", subscription);
        entityManager.persist(subscription);
        entityManager.persist(subscriptionMetricDetectorMapping);
        entityManager.flush();

        // when
        SubscriptionMetricDetectorMapping found =
            subscriptionMetricDetectorMappingRepo.findByMetricIdAndDetectorId(
                "metricId", "detectorId").get(0);

        // then
        assertThat(found.getDetectorId())
            .isEqualTo(subscriptionMetricDetectorMapping.getDetectorId());
        assertThat(found.getMetricId())
            .isEqualTo(subscriptionMetricDetectorMapping.getMetricId());
    }
}
