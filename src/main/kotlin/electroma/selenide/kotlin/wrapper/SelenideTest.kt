package electroma.selenide.kotlin.wrapper

import com.codeborne.selenide.*
import com.codeborne.selenide.impl.ScreenShotLaboratory
import com.codeborne.selenide.junit.ScreenShooter
import com.google.common.base.Function
import com.google.common.base.Throwables
import com.google.common.truth.TestVerb
import org.junit.Rule
import org.openqa.selenium.Dimension
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import java.util.concurrent.TimeUnit
import kotlin.text.Regex

open public class SelenideTest {

    init {
        mapOf(
                "selenide.browser" to "firefox",
                "selenide.timeout" to "10000").forEach {
            if (System.getProperty(it.key) == null) {
                System.setProperty(it.key, it.value)
            }
        }

        System.getProperty("selenide.resolution")?.split("x")?.let {
            WebDriverRunner.getWebDriver().manage().window().size = Dimension(it[0].toInt(), it[1].toInt())
        }
    }

    @Rule
    fun getScreenShot(): ScreenShooter {
        // configure filename escape to work with nifty kotlin naming
        Screenshots.screenshots = EscapingScreenShotLaboratory();
        return ScreenShooter.failedTests()
    }

    public fun <T : Page, R> goto(page: T, steps: T.() -> R): R {
        page.goTo()
        return at(page, steps)
    }

    public fun <T : Page, R> at(page: T, steps: T.() -> R): R {
        // verify
        at(page)
        return page.steps()
    }

    public fun <T : Page> at(page: T): Unit {
        page.isAt()
    }
}

/**
 * wait for Truth condition to become true
 */
fun TestVerb.waitForCheck(checkToBeDone: TestVerb.() -> Unit) {
    wait({
        this.checkToBeDone()
        true
    }, Configuration.timeout, TimeUnit.MILLISECONDS)
}

/**
 * Wait for generic condition
 */
fun TestVerb.waitForCondition(checkToBeDone: TestVerb.() -> Boolean) {
    wait(checkToBeDone, Configuration.timeout, TimeUnit.MILLISECONDS)
}

fun <T> waitFor(supplier: () -> T): T = Selenide.Wait()
        .ignoring(StaleElementReferenceException::class.java)
        .until(object : Function<WebDriver, T> {
            override fun apply(input: WebDriver): T {
                return supplier()
            }
        })

fun SelenideElement?.trimmedText() = this?.innerText()?.trim()
fun SelenideElement.title() = this.getAttribute("title")

/**
 * We should distinguish short and long waiting. Short - just only for UI update and rendering,
 * while the long - for waiting timer interval updates
 */
internal fun TestVerb.wait(checks: TestVerb.() -> Boolean, timeout: Long, timeoutUnit: TimeUnit) {
    try {
        Selenide.Wait()
                .ignoring(AssertionError::class.java)
                .withTimeout(timeout, timeoutUnit)
                .until({
                    this.checks()
                })
    } catch(e: TimeoutException) {
        Throwables.propagate(e.cause ?: e)
    }
}

internal class EscapingScreenShotLaboratory : ScreenShotLaboratory() {
    override fun generateScreenshotFileName(): String? {
        return super.generateScreenshotFileName().replace(Regex("[^a-zA-Z0-9-_\\.\\\\/\\.]"), "_")
    }
}