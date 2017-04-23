case class Reader[A, B](run: A => B) {
  def flatMap[C](f: B => Reader[A, C]): Reader[A, C] = Reader(a => f(run(a)).run(a))
  def map[C](f: B => C): Reader[A, C] = Reader((a: A) => f(run(a)))
}

val r = for {
  x <- Reader((_: Int) * 2)
  y <- Reader((_: Int) + 10)
} yield x + y

r.run(3)

val s = Reader((_: Int) * 2).flatMap(x => Reader((_: Int) + 10).map(y => x + y))
s.run(3)




