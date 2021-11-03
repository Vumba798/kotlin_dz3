import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import java.io.File
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
    @SerializedName("min")
    val min: Float,
    @SerializedName("max")
    val max: Float,
    @SerializedName("avg")
    val avg: Float,
    @SerializedName("growing")
    val growing: List<Rate>,
    @SerializedName("rates")
    val rates: List<Rate>
)

suspend fun readFromFileAnalyzes(path: String): AnalyzeResult = coroutineScope {
    withContext(Dispatchers.IO) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val jsonString = File(path).bufferedReader().use {
            it.readText()
        }
        gson.fromJson(jsonString, AnalyzeResult::class.java)
    }
}

class RateAnalyzer {
    private var rates = mutableListOf<Rate>()
    private var growing: List<Rate> = mutableListOf()
    private var min: Float = 0F
    private var max: Float = 0F
    private var avg: Float = 0F
    private var jsonString = ""

    private fun getDateMonthAgo() = LocalDate.now(ZoneId.systemDefault()).minusMonths(1)

    suspend fun getAnalyzeResult(): AnalyzeResult = coroutineScope {
        rates = mutableListOf()
        val deferredLatest: Deferred<JsonObject> = async(Dispatchers.IO) {
            val url = URL("https://openexchangerates.org/api/latest.json?app_id=d1290d44145b4d62b6760da7db9446e8")
            val apiLatestString = with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                inputStream.bufferedReader().use {
                    it.readText()
                }
            }
            JsonParser.parseString(apiLatestString).asJsonObject["rates"].asJsonObject
        }
        val deferredMonthAgo: Deferred<JsonObject> = async(Dispatchers.IO) {
            val dateMonthAgo = getDateMonthAgo().toString()
            val url =
                URL("https://openexchangerates.org/api/historical/$dateMonthAgo.json?app_id=d1290d44145b4d62b6760da7db9446e8")
            val apiHistoricalString = with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"
                inputStream.bufferedReader().use {
                    it.readText()
                }
            }
            JsonParser.parseString(apiHistoricalString).asJsonObject["rates"].asJsonObject
        }
        val latestRates = deferredLatest.await()
        val charCodes = latestRates.keySet()

        val monthAgoRates = deferredMonthAgo.await()
        for (charCode in charCodes) {
            rates.add(Rate(charCode, monthAgoRates[charCode].asFloat, latestRates[charCode].asFloat))
        }
        updateVars()
        AnalyzeResult(min, max, avg, growing, rates)
    }

    private fun updateVars() {
        growing = rates
            .filter { it.enhancement > 0 }
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

        val gson = GsonBuilder().setPrettyPrinting().create()
        jsonString = gson.toJson(AnalyzeResult(min, max, avg, growing, rates))
    }

    fun getJson(): String {
        return jsonString
    }
    suspend fun saveToFile(path: String) = withContext(Dispatchers.IO){
        File(path).writeText(jsonString)
    }
    suspend fun print() = withContext(Dispatchers.Main) {
        println(jsonString)
    }
}
