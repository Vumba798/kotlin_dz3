fun main() {
    try {
        val analyzer = RateAnalyzer()
        println("growing are:")
        for (element in analyzer.growing) {
            println(element.toString())
        }
        println("\nmin = ${analyzer.min}%")
        println("max = ${analyzer.max}%")
        println("avg = ${analyzer.avg}%")
    } catch (exception: Exception) {
        println(exception.message)
    }
}