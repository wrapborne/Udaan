package com.viplove.licadvisornative.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.viplove.licadvisornative.model.NavData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException

object NavScraper {

    private const val TAG = "NavScraper"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    private const val BASE_REFERER = "https.licindia.in/"

    sealed class ScrapeResult {
        data class Success(val data: List<NavData>) : ScrapeResult()
        data class Error(val message: String) : ScrapeResult()
    }

    /**
     * Data class to parse the JSON response from the new URL (getNavValue)
     */
    data class NewNavResponse(
        val sfin: String?,
        val fund_name: String?,
        val nav: String?,
        val p_date: String?, // This is the launch date
        val plan_name: String?
    )

    /**
     * Fetches NAV data from all provided URLs and combines them.
     */
    suspend fun fetchAllNavData(publicUrl: String, dataUrls: List<String>): ScrapeResult {
        return withContext(Dispatchers.IO) {
            try {
                // --- Step 1: Get Cookies ---
                Log.d(TAG, "Step 1: Fetching cookies from $publicUrl")
                val cookieResponse: Connection.Response = Jsoup.connect(publicUrl)
                    .userAgent(USER_AGENT)
                    .referrer(BASE_REFERER)
                    .execute()

                val cookies = cookieResponse.cookies()
                if (cookies.isEmpty()) {
                    Log.e(TAG, "Failed to get session cookie from public URL.")
                    return@withContext ScrapeResult.Error("Failed to initiate secure session.")
                }
                Log.d(TAG, "Step 1: Cookies received successfully.")

                // --- Step 2: Fetch from all URLs ---
                val allNavs = mutableListOf<NavData>()
                for (url in dataUrls) {
                    try {
                        if (url.contains("getNavValue")) {
                            // This is the new JSON URL (for Index Plus)
                            allNavs.addAll(fetchNewJsonFormat(url, publicUrl, cookies))
                        } else {
                            // This is the old HTML table URL (for SIIP, etc.)
                            allNavs.addAll(fetchOldHtmlFormat(url, publicUrl, cookies))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to fetch or parse URL: $url", e)
                        // Don't fail all, just skip this URL
                    }
                }

                if (allNavs.isEmpty()) {
                    Log.e(TAG, "Could not fetch any NAV data from any source.")
                    return@withContext ScrapeResult.Error("Failed to fetch NAV data.")
                }

                Log.d(TAG, "Successfully parsed ${allNavs.size} total NAV entries from ${dataUrls.size} sources.")
                // Remove duplicates just in case both pages list the same fund
                ScrapeResult.Success(allNavs.distinctBy { it.sfin })

            } catch (e: Exception) {
                Log.e(TAG, "General exception in fetchAllNavData", e)
                ScrapeResult.Error("A network or parsing error occurred: ${e.message}")
            }
        }
    }

    /**
     * Fetches and parses the NEW JSON format (e.g., for Index Plus)
     */
    private fun fetchNewJsonFormat(dataUrl: String, referer: String, cookies: Map<String, String>): List<NavData> {
        val navList = mutableListOf<NavData>()
        try {
            val jsonResponse = Jsoup.connect(dataUrl)
                .userAgent(USER_AGENT)
                .cookies(cookies)
                .referrer(referer)
                .ignoreContentType(true) // Important for JSON
                .execute()
                .body()

            val listType = object : TypeToken<List<NewNavResponse>>() {}.type
            val responseList: List<NewNavResponse> = Gson().fromJson(jsonResponse, listType)

            for (item in responseList) {
                val nav = item.nav?.toDoubleOrNull()
                if (nav != null && !item.sfin.isNullOrBlank() && !item.fund_name.isNullOrBlank()) {
                    navList.add(
                        NavData(
                            planName = item.plan_name ?: "Unknown Plan",
                            fundName = item.fund_name,
                            sfin = item.sfin,
                            nav = nav,
                            launchDate = item.p_date ?: "" // p_date is the launch date
                        )
                    )
                }
            }
            Log.d(TAG, "Parsed ${navList.size} entries from NEW JSON format (getNavValue)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch or parse NEW JSON format", e)
        }
        return navList
    }

    /**
     * Fetches and parses the OLD HTML table format (e.g., for SIIP, Endowment Plus)
     */
    private fun fetchOldHtmlFormat(dataUrl: String, referer: String, cookies: Map<String, String>): List<NavData> {
        val navList = mutableListOf<NavData>()
        try {
            val document = Jsoup.connect(dataUrl)
                .userAgent(USER_AGENT)
                .cookies(cookies)
                .timeout(30000)
                .referrer(referer)
                .get()

            val tables = document.select("table.commonTableBorder")
            if (tables.isEmpty()) {
                Log.e(TAG, "Could not find table 'commonTableBorder' in OLD format")
                return navList
            }

            val navTable = tables.first()
            val rows = navTable!!.select("tr")
            var currentPlanName = ""
            var currentLaunchDate = ""

            for (row in rows) {
                val columns = row.select("td")
                when (columns.size) {
                    // Plan Name row
                    2 -> {
                        currentPlanName = columns.getOrNull(0)?.text()?.trim() ?: ""
                        val rawDateText = columns.getOrNull(1)?.text()?.trim() ?: ""
                        currentLaunchDate = rawDateText.removePrefix("Launch Date:").trim()
                    }
                    // Fund Data row
                    6 -> {
                        val fundType = columns.getOrNull(0)?.text()?.trim() ?: ""
                        val sfin = columns.getOrNull(1)?.text()?.trim() ?: ""
                        val navStr = columns.getOrNull(3)?.text()?.trim() ?: ""
                        val nav = navStr.toDoubleOrNull()

                        if (nav != null && sfin.isNotEmpty() && fundType.isNotEmpty() && !fundType.contains("Face Value")) {
                            navList.add(
                                NavData(
                                    planName = currentPlanName,
                                    fundName = fundType,
                                    sfin = sfin,
                                    nav = nav,
                                    launchDate = currentLaunchDate
                                )
                            )
                        }
                    }
                }
            }
            Log.d(TAG, "Parsed ${navList.size} entries from OLD HTML format (CurrentNAVDayController.jpf)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch or parse OLD HTML format", e)
        }
        return navList
    }
}