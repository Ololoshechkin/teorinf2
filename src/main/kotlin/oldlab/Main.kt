package oldlab

fun encode(data: Data): Data {
    val (s, i) = oldlab.barrows.encode(data)
    val m = oldlab.mtf.encode(s)
    return oldlab.monotone.encode(listOf(i) + m)
}

fun decode(data: Data): Data {
    val d = oldlab.monotone.decode(data)
    val i = d[0]
    val m = d.drop(1)
    val s = oldlab.mtf.decode(m)
    return oldlab.barrows.decode(s, i)
}