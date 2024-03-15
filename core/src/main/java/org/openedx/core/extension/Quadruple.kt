package org.openedx.core.extension

/**
 * This class is used to create a Quadruple object which can hold 4 different types of objects.
 */
class Quadruple<A1, A2, A3, A4>(val a1: A1, val a2: A2, val a3: A3, val a4: A4) {
    operator fun component1(): A1 = a1
    operator fun component2(): A2 = a2
    operator fun component3(): A3 = a3
    operator fun component4(): A4 = a4
}
