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

package com.expedia.alertmanager.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class HealthController {

    private final String statusFile;

    HealthController(final String statusFile) {
        this.statusFile = statusFile;
    }

    void setUnhealthy() {
        write("unhealthy");
    }

    void setHealthy() {
        write("healthy");
    }

    private void write(final String status) {
        try {
            Files.write(Paths.get(statusFile), status.getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
