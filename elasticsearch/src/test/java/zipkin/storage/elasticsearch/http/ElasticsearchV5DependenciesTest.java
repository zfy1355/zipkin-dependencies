/**
 * Copyright 2016-2018 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin.storage.elasticsearch.http;

import org.junit.ClassRule;

public class ElasticsearchV5DependenciesTest extends ElasticsearchDependenciesTest {

  @ClassRule public static LazyElasticsearchHttpStorage storage =
      new LazyElasticsearchHttpStorage("openzipkin/zipkin-elasticsearch5:2.5.0");

  @Override protected ElasticsearchHttpStorage esStorage() {
    return storage.get();
  }

  @Override protected String esNodes() {
    return storage.esNodes();
  }
}
