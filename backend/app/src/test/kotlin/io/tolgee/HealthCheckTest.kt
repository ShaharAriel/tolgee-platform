/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee

import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.ContextRecreatingTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
@ContextRecreatingTest
class HealthCheckTest : AbstractControllerTest() {

//  @Autowired
//  @MockBean(answer = Answers.CALLS_REAL_METHODS)
//  private lateinit var ds: DataSource

  @Test
  fun `health check works`() {
    performGet("/actuator/health").andIsOk
  }
}
