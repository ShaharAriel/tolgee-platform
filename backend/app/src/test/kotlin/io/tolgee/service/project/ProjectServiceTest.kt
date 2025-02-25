/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.service.project

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.MtSettingsTestData
import io.tolgee.development.testDataBuilder.data.TagsTestData
import io.tolgee.fixtures.generateUniqueString
import io.tolgee.model.Permission
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProjectServiceTest : AbstractSpringTest() {

  @Test
  fun testFindAllPermitted() {
    executeInNewTransaction {
      val users = dbPopulator.createUsersAndOrganizations()
      dbPopulator.createBase("Test", users[3].username)
      val projects = projectService.findAllPermitted(users[3])
      assertThat(projects).hasSize(10)
    }
  }

  @Test
  fun testFindAllEmpty() {
    executeInNewTransaction {
      dbPopulator.createUsersAndOrganizations() // create some data
      val user = dbPopulator.createUserIfNotExists("user")
      val projects = projectService.findAllPermitted(user)
      assertThat(projects).hasSize(0)
    }
  }

  @Test
  fun testFindAllSingleProject() {
    executeInNewTransaction {
      dbPopulator.createUsersAndOrganizations() // create some data
      val base = dbPopulator.createBase("Hello world", generateUniqueString())
      val projects = projectService.findAllPermitted(base.userAccount)
      assertThat(projects).hasSize(1)
      assertThat(projects[0].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    }
  }

  @Test
  fun testFindMultiple() {
    executeInNewTransaction {
      val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("helga") // create some data
      val base = dbPopulator.createBase("Hello world")
      val organization = usersWithOrganizations[0].organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization!!, OrganizationRoleType.MEMBER)

      val user3 = userAccountService.get(usersWithOrganizations[3].id)

      val organization2 = user3.organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization2!!, OrganizationRoleType.OWNER)
      val projects = projectService.findAllPermitted(base.userAccount)
      assertThat(projects).hasSize(7)
      assertThat(projects[6].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
      assertThat(projects[1].permissionType).isEqualTo(Permission.ProjectPermissionType.VIEW)
      assertThat(projects[5].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    }
  }

  @Test
  fun testFindMultiplePermissions() {
    executeInNewTransaction(platformTransactionManager) {
      val usersWithOrganizations = dbPopulator.createUsersAndOrganizations("agnes") // create some data
      val base = dbPopulator.createBase("Hello world")
      val organization = usersWithOrganizations[0].organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization!!, OrganizationRoleType.MEMBER)

      val user3 = userAccountService.get(usersWithOrganizations[3].id) // entityManager.merge(usersWithOrganizations[3])

      val organization2 = user3.organizationRoles[0].organization
      organizationRoleService.grantRoleToUser(base.userAccount, organization2!!, OrganizationRoleType.OWNER)

      val customPermissionProject = usersWithOrganizations[0].organizationRoles[0].organization!!.projects[2]
      val customPermissionProject2 = user3.organizationRoles[0].organization!!.projects[2]
      permissionService.create(
        Permission(
          user = base.userAccount,
          project = customPermissionProject,
          type = Permission.ProjectPermissionType.TRANSLATE
        )
      )
      permissionService.create(
        Permission(
          user = base.userAccount,
          project = customPermissionProject2,
          type = Permission.ProjectPermissionType.TRANSLATE
        )
      )

      val projects = projectService.findAllPermitted(base.userAccount)
      assertThat(projects).hasSize(7)
      assertThat(projects[6].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
      assertThat(projects[2].permissionType).isEqualTo(Permission.ProjectPermissionType.TRANSLATE)
      assertThat(projects[1].permissionType).isEqualTo(Permission.ProjectPermissionType.VIEW)
      assertThat(projects[5].permissionType).isEqualTo(Permission.ProjectPermissionType.MANAGE)
    }
  }

  @Test
  fun testDeleteProjectWithTags() {
    executeInNewTransaction(platformTransactionManager) {
      val testData = TagsTestData()
      testData.generateVeryLotOfData()
      testDataService.saveTestData(testData.root)
      val start = System.currentTimeMillis()
      projectService.deleteProject(testData.projectBuilder.self.id)
      entityManager.flush()
      entityManager.clear()
      val time = System.currentTimeMillis() - start
      println(time)
      assertThat(time).isLessThan(20000)
      assertThat(tagService.find(testData.existingTag.id)).isNull()
    }
  }

  @Test
  fun `deletes project with MT Settings`() {
    val testData = executeInNewTransaction {
      val testData = MtSettingsTestData()
      testDataService.saveTestData(testData.root)
      return@executeInNewTransaction testData
    }
    executeInNewTransaction(platformTransactionManager) {
      projectService.deleteProject(testData.projectBuilder.self.id)
    }
  }
}
