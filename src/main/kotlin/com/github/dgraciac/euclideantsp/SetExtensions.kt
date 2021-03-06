package com.github.dgraciac.euclideantsp

fun <E> Set<E>.permute(): List<List<E>> {
    require(this.isNotEmpty())
    return when (this.size) {
        1 -> listOf(this.toList())
        else -> this.map { element: E ->
            this.minusElement(element).permute().map { subPermutation: List<E> ->
                listOf(element).plus(subPermutation)
            }
        }.reduce { first, second -> first.plus(second) }
    }
}
