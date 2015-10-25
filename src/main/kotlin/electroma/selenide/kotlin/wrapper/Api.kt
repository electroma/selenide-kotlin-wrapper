package electroma.selenide.kotlin.wrapper

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide
import com.codeborne.selenide.Selenide.`$`
import com.codeborne.selenide.Selenide.open
import com.codeborne.selenide.SelenideElement
import com.codeborne.selenide.WebDriverRunner.url
import com.google.common.base.Function
import com.google.common.base.Optional
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebDriver
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

public interface SelenideWrapper {

    val rootSupplier: () -> SelenideElement

    val root: SelenideElement
        get() = rootSupplier()

    fun checkExist(elem: SelenideElement) = elem.should(Condition.exist)

    fun css(cssSelector: String): SelenideElement = checkExist(root.find(cssSelector))

    fun css(cssSelector: String, predicate: (SelenideElement) -> Boolean): SelenideElement =
            checkExist(root.findAll(cssSelector).firstOrNull(predicate) ?:
                    throw AssertionError("Oh no, no such element $cssSelector and predicate"))
}

public interface Module : SelenideWrapper {
    open fun click() {
        root.click()
    }

}

public abstract class Page(open val url: String) : SelenideWrapper {

    override val rootSupplier = { `$`("body") }

    fun goTo() = open(url)

    open fun isAt() {
        //TODO: better check for url
        verify("Current URL").that(url()).contains(url)
    }

}

infix fun <T : Module> KClass<T>.on(root: () -> SelenideElement): ModuleGenerator<T> {

    val constructor = this.constructors.find { it.parameters.size == 1 }
    // module should have constructor with one parameter SelenideElement
    requireNotNull(constructor) { "Module should have constructor with one parameter of type () -> SelenideElement" }
    return ModuleGenerator {
        constructor!!.call(root)
    }
}

public class ModuleGenerator<T : Module>(val supplier: () -> T) {

    operator fun getValue(root: SelenideWrapper, propertyMetadata: KProperty<*>): T {
        return supplier()
    }
}

fun <T> eq(valueSupplier: () -> T) = ValueGenerator(valueSupplier)

public class ValueGenerator<T : Any?>(val supplier: () -> T) {
    operator fun getValue(root: SelenideWrapper, propertyMetadata: KProperty<*>): T {
        // Optional used to allow .until return null values
        return Selenide.Wait()
                .ignoring(StaleElementReferenceException::class.java)
                //TODO do we need ignore ElementNotFound here? mainly, when ElementNotFound.getCause() is StaleElementReferenceException
                //.ignoring(ElementNotFound::class.java)
                .until(object : Function<WebDriver, Optional<T>> {
                    override fun apply(input: WebDriver): Optional<T> {
                        return Optional.fromNullable(supplier())
                    }
                }).orNull()
    }
}

