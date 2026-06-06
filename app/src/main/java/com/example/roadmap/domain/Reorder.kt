package com.example.roadmap.domain

/** A reorder request is valid iff it is a permutation of the existing sibling ids (spec §7.4). */
fun isValidReorder(current: List<Long>, requested: List<Long>): Boolean =
    current.size == requested.size && current.toSet() == requested.toSet()

/** Maps each id to its new 0-based contiguous position. */
fun positionsFor(orderedIds: List<Long>): Map<Long, Int> =
    orderedIds.withIndex().associate { (index, id) -> id to index }
