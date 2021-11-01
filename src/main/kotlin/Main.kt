suspend fun main() {
    try {
        val analyzes = RateAnalyzer().getAnalyzeResult()
        println("growing are:")
        for (element in analyzes.growing) {
            println(element.toString())
        }
        println("\nmin = ${analyzes.min}%")
        println("max = ${analyzes.max}%")
        println("avg = ${analyzes.avg}%")
    } catch (exception: Exception) {
        println(exception.message)
    }
}