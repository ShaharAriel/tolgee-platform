package io.tolgee.api.v2.hateoas.user_account

import io.tolgee.api.v2.controllers.V2UserController
import io.tolgee.model.UserAccount
import io.tolgee.service.AvatarService
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class UserAccountModelAssembler(
  private val avatarService: AvatarService
) : RepresentationModelAssemblerSupport<UserAccount, UserAccountModel>(
  V2UserController::class.java, UserAccountModel::class.java
) {
  override fun toModel(entity: UserAccount): UserAccountModel {
    val avatar = avatarService.getAvatarLinks(entity.avatarHash)

    return UserAccountModel(
      id = entity.id,
      username = entity.username,
      name = entity.name,
      emailAwaitingVerification = entity.emailVerification?.newEmail,
      avatar = avatar,
      globalServerRole = entity.role ?: UserAccount.Role.USER,
      deleted = entity.deletedAt != null
    )
  }
}
