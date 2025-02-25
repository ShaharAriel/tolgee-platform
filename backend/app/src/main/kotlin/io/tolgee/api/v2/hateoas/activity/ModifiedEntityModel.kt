package io.tolgee.api.v2.hateoas.activity

import io.tolgee.activity.data.ExistenceEntityDescription
import io.tolgee.activity.data.PropertyModification

data class ModifiedEntityModel(
  val entityId: Long,
  val description: Map<String, Any?>? = null,
  var modifications: Map<String, PropertyModification>? = null,
  var relations: Map<String, ExistenceEntityDescription>? = null,
  val exists: Boolean? = null
)
