import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun main(): Unit = coroutineScope {
    try {
        val analyzer = RateAnalyzer()
        val analyzes = analyzer.getAnalyzeResult()
        println("growing are:")
        for (element in analyzes.growing) {
            println(element.toString())
        }
        println("\nmin = ${analyzes.min}%")
        println("max = ${analyzes.max}%")
        println("avg = ${analyzes.avg}%")
        analyzer.saveToFile("savedResults.json")

        val analyzesFromFile = readFromFileAnalyzes("savedResults.json")
        println(analyzes == analyzesFromFile)
    } catch (exception: Exception) {
        println(exception.message)
    }
}
