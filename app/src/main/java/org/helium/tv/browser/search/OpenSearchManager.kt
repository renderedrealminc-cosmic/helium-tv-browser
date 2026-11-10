package org.helium.tv.browser.search

import android.content.Context
import android.net.Uri
import android.util.Log
import org.helium.tv.browser.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.StringReader

data class SearchEngine(
    val shortName: String,
    val description: String,
    val urlTemplate: String
)

class OpenSearchManager(private val context: Context) {

    companion object {
        private const val TAG = "OpenSearchManager"
        private const val PREFS_NAME = "helium_search_prefs"
        private const val KEY_ACTIVE_ENGINE = "active_search_engine"
    }

    private val engines = mutableMapOf<String, SearchEngine>()
    var activeEngine: SearchEngine

    init {
        // Load default engines from raw resources
        loadFromRawResource(R.raw.opensearch_google)
        loadFromRawResource(R.raw.opensearch_user)
        loadFromRawResource(R.raw.opensearch_bing)

        // Default to Google OpenSearch engine
        activeEngine = engines["Google"]
            ?: engines["MyBrowserSearch"]
            ?: SearchEngine("Google", "Search with Google", "https://www.google.com/search?q={searchTerms}")
    }

    fun loadFromRawResource(resId: Int) {
        try {
            val inputStream: InputStream = context.resources.openRawResource(resId)
            val engine = parseOpenSearchXml(inputStream)
            if (engine != null) {
                engines[engine.shortName] = engine
                Log.d(TAG, "Loaded OpenSearch engine: ${engine.shortName} -> ${engine.urlTemplate}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading raw OpenSearch XML: $resId", e)
        }
    }

    fun loadFromXmlString(xmlContent: String): SearchEngine? {
        val engine = parseOpenSearchString(xmlContent)
        if (engine != null) {
            engines[engine.shortName] = engine
            activeEngine = engine
        }
        return engine
    }

    fun buildSearchUrl(query: String): String {
        val encodedQuery = Uri.encode(query.trim())
        val template = activeEngine.urlTemplate

        return when {
            // Handle template: https://google.com{searchTerms} -> convert to proper Google query
            template.contains("google.com{searchTerms}") -> {
                "https://www.google.com/search?q=$encodedQuery"
            }
            template.contains("{searchTerms}") -> {
                template.replace("{searchTerms}", encodedQuery)
            }
            else -> {
                "https://www.google.com/search?q=$encodedQuery"
            }
        }
    }

    fun getAvailableEngines(): List<SearchEngine> = engines.values.toList()

    private fun parseOpenSearchXml(inputStream: InputStream): SearchEngine? {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")
        return parse(parser)
    }

    private fun parseOpenSearchString(xmlContent: String): SearchEngine? {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))
        return parse(parser)
    }

    private fun parse(parser: XmlPullParser): SearchEngine? {
        var eventType = parser.eventType
        var shortName = ""
        var description = ""
        var urlTemplate = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name?.lowercase()) {
                        "shortname" -> shortName = parser.nextText()
                        "description" -> description = parser.nextText()
                        "url" -> {
                            val template = parser.getAttributeValue(null, "template")
                            if (!template.isNullOrEmpty()) {
                                urlTemplate = template
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return if (shortName.isNotEmpty() && urlTemplate.isNotEmpty()) {
            SearchEngine(shortName, description, urlTemplate)
        } else {
            null
        }
    }
}
