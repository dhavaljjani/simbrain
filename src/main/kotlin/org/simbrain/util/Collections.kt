package org.simbrain.util

/**
 * Consider sets A and B.  "Left complement" is in A but not B.  "Right complement" is in B but not A.
 */
data class SetDifference<T>(val leftComp: Set<T>, val rightComp: Set<T>) {
    fun isIdentical() = leftComp.isEmpty() && rightComp.isEmpty()
}

/**
 * Custom infix relative complementation operator.
 */
infix fun <T> Set<T>.complement(other: Set<T>) = SetDifference(this - other, other - this)

/**
 * A.k.a list of all to all pairs.
 */
infix fun <T> Iterable<T>.cartesianProduct(other: Iterable<T>) = this.flatMap { a -> other.map { b -> a to b }}