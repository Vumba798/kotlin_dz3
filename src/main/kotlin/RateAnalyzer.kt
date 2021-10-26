import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId

data class Rate(
    val charCode: String,
    val costMonthAgo: Float,
    val cost: Float,
    val enhancement: Float = cost / costMonthAgo * 100 - 100,
)

data class AnalyzeResult(
    val rates: List<Rate>,
    val growing: List<Rate>,
    val min: Float,
    val max: Float,
    val avg: Float
)

class RateAnalyzer {
    private var rates = mutableListOf<Rate>()
    private var growing: List<Rate> = mutableListOf()
    private var min: Float = 0F
    private var max: Float = 0F
    private var avg: Float = 0F

    private fun getDateMonthAgo() = LocalDate.now(ZoneId.systemDefault()).minusMonths(1)

    fun getAnalyzeResult(): AnalyzeResult {
        rates = mutableListOf()
        var url = URL("https://openexchangerates.org/api/latest.json?app_id=d1290d44145b4d62b6760da7db9446e8")
        val apiLatestString = with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            inputStream.bufferedReader().use {
                it.readText()
            }
        }

        val dateMonthAgo = getDateMonthAgo().toString()
        url =
            URL("https://openexchangerates.org/api/historical/$dateMonthAgo.json?app_id=d1290d44145b4d62b6760da7db9446e8")
        val apiHistoricalString = with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            inputStream.bufferedReader().use {
                it.readText()
            }
        }
        val latestRates = JsonParser.parseString(apiLatestString).asJsonObject["rates"].asJsonObject
        val monthAgoRates = JsonParser.parseString(apiHistoricalString).asJsonObject["rates"].asJsonObject

        val charCodes = latestRates.keySet()
        for (charCode in charCodes) {
            rates.add(Rate(charCode, monthAgoRates[charCode].asFloat, latestRates[charCode].asFloat))
        }
        updateVars()
        return AnalyzeResult(rates, growing, min, max, avg)
    }

    private fun updateVars() {
        growing = rates
            .filter { it.enhancement > 0}
            .map { it }
        min = rates
            .minOf { it.enhancement }
        max = rates
            .maxOf { it.enhancement }
        avg = rates
            .sumOf {
                it.enhancement.toDouble()
            }.toFloat()
        avg /= rates.size
    }

}