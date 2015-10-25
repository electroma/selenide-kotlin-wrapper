package electroma.selenide.kotlin.wrapper

import com.google.common.truth.TestVerb
import com.google.common.truth.Truth

/**
 * Simple check with message
 * Example: verify ("Another message") that (summary.details["Client"]) isEqualTo "Google"
 */
fun verify(message: String) = Truth.assertWithMessage(message)

/**
 * Multiple step check
 * Example:
 * verify("Correct channel name") {
 *   that (summary.details["client"]) isEqualTo "Google"
 *   that (summary.requestName) isEqualTo expectedName
 * }
 */
fun verify(message: String, checks: TestVerb.() -> Unit) = (Truth.assertWithMessage(message)).checks()

