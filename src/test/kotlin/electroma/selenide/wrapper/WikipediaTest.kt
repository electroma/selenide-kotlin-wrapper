package electroma.selenide.wrapper

import com.codeborne.selenide.SelenideElement
import electroma.selenide.kotlin.wrapper.*
import org.junit.Test
import org.openqa.selenium.Keys
import kotlin.text.Regex

open class WikipediaTest : SelenideTest() {

    init {
        System.getProperty("target.baseUrl", "https://en.wikipedia.org")
                ?.let { System.setProperty("selenide.baseUrl", it) }
    }

    @Test fun `wikipedia search and navigate`() {
        goto(HomePage()) {
            searchFor("Kotlin")
            verify("Suggestions appear and contain Island").waitForCheck {
                that(suggestions).containsAllOf("Kotlin", "Kotlin Island")
            }
            submitSearch()
        }
        at(WikiMultiPage("Kotlin")) {
            verify("There are several results, including island and language")
                    .that(results.topics)
                    .containsAllOf("Kotlin Island", "Kotlin (programming language)")
            results["Kotlin (programming language)"]!!.click()
        }
        at(WikiSinglePage("Kotlin (programming language)")) {
            verify("There is summary info about Developer and License") {
                that(summary["Designed by"]).isEqualTo("JetBrains")
                that(summary["License"]).isEqualTo("Apache 2")
            }
        }
    }
}

abstract class WikipediaPage(override val url: String) : Page(url) {
    private val search by eq { css("#searchInput") }
    private val suggestionsBox by eq { css(".suggestions-results") }

    val suggestions by eq { suggestionsBox.findAll(".suggestions-result").map { it.innerText() } }

    fun searchFor(query: String) {
        search.sendKeys(query)
    }

    fun submitSearch() = search.sendKeys(Keys.ENTER)


}

class HomePage : WikipediaPage("/")

abstract class WikiArticlePage(open val topic: String) : WikipediaPage("wiki/${topic.escapeSearchTopic()}") {

}

class WikiSinglePage(override val topic: String) : WikiArticlePage(topic) {
    val summary by SummaryTable::class on { css(".infobox") }
}

class SummaryTable(override val rootSupplier: () -> SelenideElement) : Module {
    operator fun get(key: String): String? {
        val foundSummary = root.findAll("th")
                .find({ it.innerText() == key })
        return foundSummary?.let { it.parent().find("td").innerText() }
    }

}

class WikiMultiPage(override val topic: String) : WikiArticlePage(topic) {

    override fun isAt() {
        super.isAt()
        verify("Search results should be shown")
                .that(css("#mw-content-text > p:nth-child(1)").trimmedText())
                .startsWith("$topic may refer to:")
    }

    val results by SearchResults::class on { css("#mw-content-text > ul") }

}

internal fun String.escapeSearchTopic() = this.trim().replace(Regex("\\s"), "_")

class SearchResults(override val rootSupplier: () -> SelenideElement) : Module {

    private val resultLinks by eq { root.findAll("a") }

    val topics by eq { resultLinks.map { it.title() } }

    operator fun get(title: String) = resultLinks.find { it.title() == title }
}
