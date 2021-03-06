/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.clouddriver.tasks.servergroup

import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus
import com.netflix.spinnaker.orca.clouddriver.KatoService
import com.netflix.spinnaker.orca.clouddriver.model.TaskId
import spock.lang.Specification
import spock.lang.Subject
import static com.netflix.spinnaker.orca.test.model.ExecutionBuilder.stage

class UpsertServerGroupTagsTaskSpec extends Specification {

  @Subject task = new UpsertServerGroupTagsTask()
  def stage = stage()
  def taskId = new TaskId(UUID.randomUUID().toString())

  def upsertServerGroupTagsConfig = [
      serverGroupName: "test-server-group",
      regions        : ["us-central1"],
      zone           : "us-central1-f",
      credentials    : "fzlem",
      tags           : ["a-tag-1", "a-tag-2", "a-tag-3"], // Appears to be GCE style only.
      cloudProvider  : "abc"
  ]

  def setup() {
    stage.context.putAll(upsertServerGroupTagsConfig)
  }

  def "creates an upsert google server group tags task based on job parameters"() {
    given:
      def operations
      task.kato = Mock(KatoService) {
        1 * requestOperations("abc", _) >> {
          operations = it[1]
          taskId
        }
      }

    when:
    task.execute(stage)

    then:
      operations.size() == 1
      with(operations[0].upsertServerGroupTags) {
        it instanceof Map
        serverGroupName == this.upsertServerGroupTagsConfig.serverGroupName
        regions == this.upsertServerGroupTagsConfig.regions
        zone == this.upsertServerGroupTagsConfig.zone
        credentials == this.upsertServerGroupTagsConfig.credentials
        tags == this.upsertServerGroupTagsConfig.tags
      }
  }

  def "returns a success status with the kato task id"() {
    given:
      task.kato = Stub(KatoService) {
        requestOperations(*_) >> taskId
      }

    when:
    def result = task.execute(stage)

    then:
      result.status == ExecutionStatus.SUCCEEDED
    result.context."kato.last.task.id" == taskId
    result.context."deploy.account.name" == upsertServerGroupTagsConfig.credentials
    result.context."deploy.server.groups" == [
          (upsertServerGroupTagsConfig.regions[0]): [upsertServerGroupTagsConfig.serverGroupName]
      ]
  }
}
