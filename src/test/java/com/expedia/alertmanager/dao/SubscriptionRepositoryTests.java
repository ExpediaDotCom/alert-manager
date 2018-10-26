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
    private SubscriptionRepository subscriptionRepo;

    @Test
    public void whenFindByDetectorIdAndMetricId_thenReturnSubscriptions() {

        // given
        Subscription subscription = new Subscription("metricId", "detectorId", "Booking Alert",
            "change in trend", Subscription.EMAIL_TYPE, "email@email.com", "user");
        entityManager.persist(subscription);
        entityManager.flush();

        // when
        Subscription found =
            subscriptionRepo.findByDetectorIdAndMetricId(
                "detectorId","metricId").get(0);

        // then
        assertThat(found.getDetectorId())
            .isEqualTo(subscription.getDetectorId());
        assertThat(found.getMetricId())
            .isEqualTo(subscription.getMetricId());
    }

    @Test
    public void whenFindByDetectorId_thenReturnSubscriptions() {

        // given
        Subscription subscription = new Subscription(null, "detectorId", "Booking Alert",
            "change in trend", Subscription.EMAIL_TYPE, "email@email.com", "user");
        entityManager.persist(subscription);
        entityManager.flush();

        // when
        Subscription found =
            subscriptionRepo.findByDetectorId(
                "detectorId").get(0);

        // then
        assertThat(found.getDetectorId())
            .isEqualTo(subscription.getDetectorId());
        assertThat(found.getMetricId())
            .isEqualTo(subscription.getMetricId());
    }

    @Test
    public void whenFindByOwner_thenReturnSubscriptions() {

        // given
        Subscription subscription = new Subscription(null, "detectorId", "Booking Alert",
            "change in trend", Subscription.EMAIL_TYPE, "email@email.com", "user");
        entityManager.persist(subscription);
        entityManager.flush();

        // when
        Subscription found =
            subscriptionRepo.findByOwner(
                "user").get(0);

        // then
        assertThat(found.getDetectorId())
            .isEqualTo(subscription.getDetectorId());
        assertThat(found.getMetricId())
            .isEqualTo(subscription.getMetricId());
    }
}
