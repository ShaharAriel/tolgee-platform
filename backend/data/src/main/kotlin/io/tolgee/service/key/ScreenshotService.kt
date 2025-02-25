/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.key

import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.CreateScreenshotResult
import io.tolgee.dtos.request.ScreenshotInfoDto
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Screenshot
import io.tolgee.model.UploadedImage
import io.tolgee.model.key.Key
import io.tolgee.model.key.screenshotReference.KeyInScreenshotPosition
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.repository.KeyScreenshotReferenceRepository
import io.tolgee.repository.ScreenshotRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.ImageUploadService.Companion.UPLOADED_IMAGES_STORAGE_FOLDER_NAME
import io.tolgee.util.ImageConverter
import org.springframework.core.io.InputStreamSource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Dimension
import javax.persistence.EntityManager
import kotlin.math.roundToInt

@Service
class ScreenshotService(
  private val screenshotRepository: ScreenshotRepository,
  private val fileStorage: FileStorage,
  private val tolgeeProperties: TolgeeProperties,
  private val imageUploadService: ImageUploadService,
  private val authenticationFacade: AuthenticationFacade,
  private val entityManager: EntityManager,
  private val keyScreenshotReferenceRepository: KeyScreenshotReferenceRepository
) {
  companion object {
    const val SCREENSHOTS_STORAGE_FOLDER_NAME = "screenshots"
  }

  @Transactional
  fun store(screenshotImage: InputStreamSource, key: Key, info: ScreenshotInfoDto?): Screenshot {
    if (getScreenshotsCountForKey(key) >= tolgeeProperties.maxScreenshotsPerKey) {
      throw BadRequestException(
        io.tolgee.constants.Message.MAX_SCREENSHOTS_EXCEEDED,
        listOf(tolgeeProperties.maxScreenshotsPerKey)
      )
    }
    val converter = ImageConverter(screenshotImage.inputStream)
    val image = converter.getImage()
    val thumbnail = converter.getThumbnail()

    val screenshot = saveScreenshot(
      image.toByteArray(),
      thumbnail.toByteArray(),
      info?.location,
      converter.targetDimension
    )

    return addReference(
      key = key,
      screenshot = screenshot,
      info = info,
      originalDimension = converter.originalDimension,
      targetDimension = converter.targetDimension
    )
  }

  fun addReference(
    key: Key,
    screenshot: Screenshot,
    info: ScreenshotInfoDto?,
    originalDimension: Dimension?,
    targetDimension: Dimension?
  ): Screenshot {

    val reference = KeyScreenshotReference()
    reference.key = key
    reference.screenshot = screenshot
    screenshot.keyScreenshotReferences.add(reference)
    key.keyScreenshotReferences.add(reference)
    reference.setInfo(info, originalDimension, targetDimension)
    entityManager.persist(reference)
    return screenshot
  }

  private fun KeyScreenshotReference.setInfo(
    info: ScreenshotInfoDto?,
    originalDimension: Dimension?,
    newDimension: Dimension?
  ) {
    info?.let {
      this.originalText = info.text
      it.positions?.forEach { positionDto ->
        val xRatio = newDimension?.width?.toDouble()
          ?.div(originalDimension?.width?.toDouble() ?: 1.0) ?: 1.0
        val yRatio = newDimension?.height?.toDouble()
          ?.div(originalDimension?.height?.toDouble() ?: 1.0) ?: 1.0
        positions = positions ?: mutableListOf()
        positions!!.add(
          KeyInScreenshotPosition(
            positionDto.x.adjustByRation(xRatio),
            positionDto.y.adjustByRation(yRatio),
            positionDto.width.adjustByRation(xRatio),
            positionDto.height.adjustByRation(yRatio),
          )
        )
      }
    }
  }

  fun Int.adjustByRation(ratio: Double): Int {
    return (this * ratio).roundToInt()
  }

  @Transactional
  fun saveUploadedImages(uploadedImageIds: Collection<Long>, key: Key): Map<Long, Screenshot> {
    val screenshots = uploadedImageIds.map {
      KeyScreenshotDto().apply { uploadedImageId = it }
    }
    return saveUploadedImages(screenshots, key)
  }

  /**
   * @return Map of uploaded image id and screenshot
   */
  fun saveUploadedImages(screenshots: List<KeyScreenshotDto>, key: Key): Map<Long, Screenshot> {
    val imageIds = screenshots.map { it.uploadedImageId }
    val images = imageUploadService.find(imageIds).associateBy { it.id }
    return screenshots.map { screenshotInfo ->
      val image = images[screenshotInfo.uploadedImageId]
        ?: throw NotFoundException(io.tolgee.constants.Message.ONE_OR_MORE_IMAGES_NOT_FOUND)

      if (authenticationFacade.userAccount.id != image.userAccount.id) {
        throw PermissionException()
      }

      val info = screenshotInfo.let {
        ScreenshotInfoDto(it.text, it.positions)
      }

      val (screenshot, originalDimension, targetDimension) = saveScreenshot(image)

      addReference(key, screenshot, info, originalDimension, targetDimension)

      screenshotInfo.uploadedImageId to screenshot
    }.toMap()
  }

  /**
   * Creates and saves screenshot entity and the corresponding file
   */
  fun saveScreenshot(image: UploadedImage): CreateScreenshotResult {
    val img = fileStorage
      .readFile(
        UPLOADED_IMAGES_STORAGE_FOLDER_NAME + "/" + image.filenameWithExtension
      )
    val thumbnail = fileStorage
      .readFile(
        UPLOADED_IMAGES_STORAGE_FOLDER_NAME + "/" + image.thumbnailFilenameWithExtension
      )
    val screenshot = saveScreenshot(img, thumbnail, image.location, Dimension(image.width, image.height))
    imageUploadService.delete(image)
    return CreateScreenshotResult(
      screenshot = screenshot,
      originalDimension = Dimension(image.originalWidth, image.originalHeight),
      targetDimension = Dimension(image.width, image.height)
    )
  }

  /**
   * Creates and saves screenshot entity and the corresponding file
   */
  fun saveScreenshot(
    image: ByteArray,
    thumbnail: ByteArray,
    location: String?,
    dimension: Dimension
  ): Screenshot {
    val screenshot = Screenshot()
    screenshot.extension = "png"
    screenshot.location = location
    screenshot.width = dimension.width
    screenshot.height = dimension.height
    screenshotRepository.save(screenshot)
    fileStorage.storeFile(screenshot.getThumbnailPath(), thumbnail)
    fileStorage.storeFile(screenshot.getFilePath(), image)
    return screenshot
  }

  fun findAll(key: Key): List<Screenshot> {
    return screenshotRepository.findAllByKey(key)
  }

  @Transactional
  fun delete(screenshots: Collection<Screenshot>) {
    screenshots.forEach {
      delete(it)
    }
  }

  @Transactional
  fun delete(screenshot: Screenshot) {
    screenshotRepository.delete(screenshot)
    deleteFile(screenshot)
  }

  fun removeScreenshotReference(key: Key, screenshot: Screenshot) {
    removeScreenshotReferences(key, listOf(screenshot))
  }

  fun removeScreenshotReferences(key: Key, screenshots: List<Screenshot>) {
    removeScreenshotReferencesById(key, screenshots.map { it.id })
  }

  fun removeScreenshotReferencesById(key: Key, screenshotIds: List<Long>?) {
    screenshotIds ?: return
    val references = keyScreenshotReferenceRepository.findAll(key, screenshotIds)
    keyScreenshotReferenceRepository.deleteAll(references)
    val screenshotReferences = keyScreenshotReferenceRepository
      .findAll(screenshotIds)
      .groupBy { it.screenshot.id }
    screenshotIds.forEach {
      if (screenshotReferences[it] == null) {
        delete(it)
      }
    }
  }

  fun removeScreenshotReferences(references: List<KeyScreenshotReference>) {
    val screenshotIds = references.map { it.screenshot.id }.toSet()
    keyScreenshotReferenceRepository.deleteAll(references)
    val screenshotReferences = keyScreenshotReferenceRepository
      .findAll(screenshotIds)
      .groupBy { it.screenshot.id }
    screenshotIds.forEach {
      if (screenshotReferences[it].isNullOrEmpty()) {
        delete(it)
      }
    }
  }

  private fun delete(it: Long) {
    screenshotRepository.deleteById(it)
  }

  fun findByIdIn(ids: Collection<Long>): List<Screenshot> {
    return screenshotRepository.findAllById(ids)
  }

  fun find(id: Long): Screenshot? {
    return screenshotRepository.findById(id).orElse(null)
  }

  fun deleteAllByProject(projectId: Long) {
    val all = screenshotRepository.getAllByKeyProjectId(projectId)
    all.forEach { this.deleteFile(it) }
    screenshotRepository.deleteAll(all)
  }

  fun deleteAllByKeyId(keyId: Long) {
    deleteAllByKeyId(listOf(keyId))
  }

  fun deleteAllByKeyId(keyIds: Collection<Long>) {
    val all = keyScreenshotReferenceRepository.getAllByKeyIdIn(keyIds)
    removeScreenshotReferences(all)
  }

  private fun deleteFile(screenshot: Screenshot) {
    fileStorage.deleteFile(screenshot.getFilePath())
  }

  private fun Screenshot.getFilePath(): String {
    return "$SCREENSHOTS_STORAGE_FOLDER_NAME/${this.filename}"
  }

  private fun Screenshot.getThumbnailPath(): String {
    return "$SCREENSHOTS_STORAGE_FOLDER_NAME/${this.thumbnailFilename}"
  }

  fun saveAll(screenshots: List<Screenshot>) {
    screenshotRepository.saveAll(screenshots)
  }

  fun getScreenshotsCountForKey(key: Key): Long {
    return screenshotRepository.countByKey(key)
  }

  fun getKeysWithScreenshots(ids: Collection<Long>): List<Key> {
    return screenshotRepository.getKeysWithScreenshots(ids)
  }

  fun getScreenshotReferences(screenshots: Collection<Screenshot>): List<KeyScreenshotReference> {
    return screenshotRepository.getScreenshotReferences(screenshots)
  }

  fun saveAllReferences(data: List<KeyScreenshotReference>) {
    keyScreenshotReferenceRepository.saveAll(data)
  }

  fun getScreenshotsForKeys(keyIds: Collection<Long>): Map<Long, List<Screenshot>> {
    val keys = this.getKeysWithScreenshots(keyIds).toSet()

    val allScreenshots = keys
      .flatMap { key ->
        key.keyScreenshotReferences.map { scr -> scr.screenshot }
      }
      .toSet() // remove dupes
      .let {
        screenshotRepository.getScreenshotsWithReferences(it)
      }.toSet()

    val keyIdScreenshotsMap = allScreenshots
      .flatMap { it.keyScreenshotReferences }
      .groupBy { it.key.id }

    return keys.associate {
      it.id to (keyIdScreenshotsMap[it.id]?.map { it.screenshot } ?: emptyList())
    }
  }

  fun getKeyScreenshotReferences(importedKeys: List<Key>, locations: List<String?>): List<KeyScreenshotReference> {
    return screenshotRepository.getKeyScreenshotReferences(importedKeys, locations)
  }
}
