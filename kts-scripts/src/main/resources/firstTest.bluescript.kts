import kotlin.math.pow
import kotlin.random.Random

val rng = Random(id).nextInt(0, Int.MAX_VALUE)
val rng2 = localRandom.nextInt(0, Int.MAX_VALUE)

var changes = 0
onSizeChange { from, to ->
//    println("Listener $id observed the size change from $from to $to (total changes observed: ${++changes})")
//    logger.info { "Listener $id observed the size change from $from to $to (total changes observed: ${++changes})" }
    from.toDouble().pow(to)
}

doSomething()
logger.info { "Hello world ! $rng $rng2" }

if (rng % 2 == 0) grow()
else harvest()

rng
