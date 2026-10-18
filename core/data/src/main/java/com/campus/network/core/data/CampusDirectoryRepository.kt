package com.campus.network.core.data

import android.content.Context
import com.campus.network.core.model.CountryDirectoryEntry
import com.campus.network.core.model.UniversityDirectoryEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline directory loader for countries, administrative regions (counties/states/provinces),
 * and global universities (10,241 institutions from Hipo dataset + curated featured universities).
 */
class CampusDirectoryRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    @Volatile
    private var cachedCountries: List<CountryDirectoryEntry>? = null

    @Volatile
    private var cachedRegionsByCountry: Map<String, List<String>>? = null

    @Volatile
    private var cachedUniversities: List<UniversityDirectoryEntry>? = null

    suspend fun getCountries(): List<CountryDirectoryEntry> = withContext(ioDispatcher) {
        cachedCountries?.let { return@withContext it }
        val raw = context.assets.open("directory/countries.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(raw)
        val result = ArrayList<CountryDirectoryEntry>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                CountryDirectoryEntry(
                    code = obj.optString("code", ""),
                    name = obj.optString("name", ""),
                    currencyCode = obj.optString("cur", "USD"),
                    isFeatured = obj.optInt("feat", 0) == 1
                )
            )
        }
        cachedCountries = result
        result
    }

    suspend fun getRegionsForCountry(countryCode: String): List<String> = withContext(ioDispatcher) {
        val map = cachedRegionsByCountry ?: run {
            val raw = context.assets.open("directory/regions.json").bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(raw)
            val parsed = HashMap<String, List<String>>()
            val keys = jsonObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = jsonObj.getJSONArray(key)
                val list = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                parsed[key] = list
            }
            cachedRegionsByCountry = parsed
            parsed
        }
        map[countryCode].orEmpty()
    }

    suspend fun getAllUniversities(): List<UniversityDirectoryEntry> = withContext(ioDispatcher) {
        cachedUniversities?.let { return@withContext it }
        val raw = context.assets.open("directory/universities.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(raw)
        val result = ArrayList<UniversityDirectoryEntry>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                UniversityDirectoryEntry(
                    name = obj.optString("n", ""),
                    countryCode = obj.optString("c", ""),
                    region = obj.optString("r", ""),
                    domain = obj.optString("d", ""),
                    featuredOrder = obj.optInt("f", 0)
                )
            )
        }
        cachedUniversities = result
        result
    }

    suspend fun getUniversitiesForCountry(
        countryCode: String,
        regionName: String? = null,
        searchQuery: String = ""
    ): List<UniversityDirectoryEntry> = withContext(ioDispatcher) {
        val allForCountry = getAllUniversities().filter { it.countryCode.equals(countryCode, ignoreCase = true) }
        val query = searchQuery.trim().lowercase()

        val filteredByQuery = if (query.isEmpty()) {
            allForCountry
        } else {
            allForCountry.filter { uni ->
                uni.name.lowercase().contains(query) ||
                    uni.domain.lowercase().contains(query) ||
                    uni.region.lowercase().contains(query)
            }
        }

        val normalizedRegion = regionName?.trim().orEmpty()
        if (normalizedRegion.isEmpty()) {
            filteredByQuery
        } else {
            // Sort universities matching the selected County/State first, then featured, then alphabetical
            filteredByQuery.sortedWith(
                compareByDescending<UniversityDirectoryEntry> {
                    it.region.equals(normalizedRegion, ignoreCase = true)
                }.thenBy {
                    if (it.featuredOrder > 0) 0 else 1
                }.thenBy {
                    if (it.featuredOrder > 0) it.featuredOrder else Int.MAX_VALUE
                }.thenBy {
                    it.name
                }
            )
        }
    }

    /**
     * Resolves a university entry by matching an email address domain (e.g. student@strathmore.edu
     * or student@students.uonbi.ac.ke) against the university directory.
     */
    suspend fun resolveUniversityFromEmail(email: String): UniversityDirectoryEntry? = withContext(ioDispatcher) {
        val cleanEmail = email.trim().lowercase()
        val atIndex = cleanEmail.indexOf('@')
        if (atIndex < 0 || atIndex == cleanEmail.lastIndex) return@withContext null
        val emailDomain = cleanEmail.substring(atIndex + 1)
        if (emailDomain.isBlank()) return@withContext null

        val all = getAllUniversities()
        all.firstOrNull { uni ->
            val d = uni.domain.lowercase()
            d.isNotEmpty() && (emailDomain == d || emailDomain.endsWith(".$d"))
        }
    }

    fun getRegionLabelForCountry(countryCode: String, languageCode: String): String {
        val isSw = languageCode == "sw"
        return when (countryCode.uppercase()) {
            "KE" -> if (isSw) "Kaunti" else "County"
            "US", "AU", "IN", "NG", "DE" -> if (isSw) "Jimbo" else "State"
            "CA", "ZA", "RW" -> if (isSw) "Mkoa" else "Province"
            "GB" -> if (isSw) "Taifa / Eneo" else "Nation / Region"
            else -> if (isSw) "Mkoa / Eneo" else "Region / State"
        }
    }
}
