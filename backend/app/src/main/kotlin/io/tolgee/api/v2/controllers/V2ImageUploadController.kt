/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Encoding
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.hateoas.uploadedImage.UploadedImageModel
import io.tolgee.api.v2.hateoas.uploadedImage.UploadedImageModelAssembler
import io.tolgee.dtos.request.ImageUploadInfoDto
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.UploadedImage
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.apiKeyAuth.AccessWithApiKey
import io.tolgee.service.ImageUploadService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Suppress("MVCPathVariableInspection")
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = ["/v2/image-upload"]
)
@Tag(name = "Image upload")
class V2ImageUploadController(
  private val uploadedImageModelAssembler: UploadedImageModelAssembler,
  private val imageUploadService: ImageUploadService,
  private val authenticationFacade: AuthenticationFacade
) {
  @PostMapping("", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Uploads an image for later use")
  @AccessWithApiKey
  @ResponseStatus(HttpStatus.CREATED)
  @RequestBody(content = [Content(encoding = [Encoding(name = "info", contentType = "application/json")])])
  fun upload(
    @RequestParam("image") image: MultipartFile,
    @RequestPart("info", required = false) info: ImageUploadInfoDto?
  ): ResponseEntity<UploadedImageModel> {
    imageUploadService.validateIsImage(image)
    val imageEntity = imageUploadService.store(image, authenticationFacade.userAccountEntity, info)
    return ResponseEntity(imageEntity.model, HttpStatus.CREATED)
  }

  @DeleteMapping("/{ids}")
  @Operation(summary = "Deletes uploaded images")
  @AccessWithApiKey
  fun delete(@PathVariable ids: Set<Long>) {
    val images = imageUploadService.find(ids)
    images.forEach {
      if (it.userAccount.id != authenticationFacade.userAccount.id) {
        throw PermissionException()
      }
      imageUploadService.delete(it)
    }
  }

  private val UploadedImage.model: UploadedImageModel
    get() = uploadedImageModelAssembler.toModel(this)
}
