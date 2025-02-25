package io.tolgee.dtos.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserUpdateRequestDto(
  @field:NotBlank
  var name: String = "",

  @field:NotBlank
  var email: String = "",

  @field:Size(max = 50)
  var currentPassword: String? = null,

  @Schema(
    description = "Callback url for link sent in e-mail." +
      " This may be omitted, when server has set frontEndUrl in properties."
  )
  var callbackUrl: String? = null
)
