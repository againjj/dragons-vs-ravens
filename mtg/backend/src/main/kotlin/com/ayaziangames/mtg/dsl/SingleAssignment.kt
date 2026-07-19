package com.ayaziangames.mtg.dsl

internal class SingleAssignment<T>(
    private val name: String,
    initialValue: T
) {
    var value: T = initialValue
        private set
    private var assigned = false

    fun set(newValue: T) {
        require(!assigned) { "Property $name can only be assigned once." }
        value = newValue
        assigned = true
    }
}
