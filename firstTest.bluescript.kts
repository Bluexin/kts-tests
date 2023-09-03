import kotlin.random.Random

val rng = Random.nextInt(0, Int.MAX_VALUE)
val rng2 = localRandom.nextInt(0, Int.MAX_VALUE)

onSizeChange { from, to ->
    println("Listener $id observed the size change from $from to $to")
}

doSomething()
println("Hello world ! $rng $rng2")

if (rng2 % 2 == 0) grow()
else harvest()

rng
