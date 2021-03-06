/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.api.scr;

import java.util.Map;

public interface Configurer {

    /**
     * Configures the specified instance with the provided configuration.
     * @param configuration The configuration.
     * @param target        The target that will receive the configuration.
     * @param <T>
     */
    <T> void configure(Map<String, ?> configuration, T target) throws Exception;
}
