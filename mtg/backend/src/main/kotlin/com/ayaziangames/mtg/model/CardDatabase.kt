package com.ayaziangames.mtg.model

class CardDefinition(
    val name: String,
    val superTypes: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val subTypes: List<String> = emptyList(),
    val manaCost: List<ManaSymbol> = emptyList(),
    val power: Int? = null,
    val toughness: Int? = null
) {
    fun serialize(): String =
        buildString {
            appendLine("cardDefinition {")
            appendLine("    name = ${name.serializeString()}")
            appendListAssignment("superTypes", superTypes)
            appendListAssignment("types", types)
            appendListAssignment("subTypes", subTypes)
            appendManaCostAssignment(manaCost)
            if (power != null) {
                appendLine("    power = $power")
            }
            if (toughness != null) {
                appendLine("    toughness = $toughness")
            }
            append("}")
        }

    fun update(
        name: String = this.name,
        superTypes: List<String> = this.superTypes,
        types: List<String> = this.types,
        subTypes: List<String> = this.subTypes,
        manaCost: List<ManaSymbol> = this.manaCost,
        power: Int? = this.power,
        toughness: Int? = this.toughness
    ): CardDefinition =
        if (
            name === this.name &&
            superTypes === this.superTypes &&
            types === this.types &&
            subTypes === this.subTypes &&
            manaCost === this.manaCost &&
            power == this.power &&
            toughness == this.toughness
        ) {
            this
        } else {
            CardDefinition(
                name = name,
                superTypes = superTypes,
                types = types,
                subTypes = subTypes,
                manaCost = manaCost,
                power = power,
                toughness = toughness
            )
        }
}

class CardDatabase(
    val name: String,
    val cardsByName: Map<String, CardDefinition>
) {
    fun serialize(): String =
        buildString {
            appendLine("val $name = cardDefinitions {")
            appendLine("    name = ${name.serializeString()}")
            append(cardsByName.values.joinToString("\n") { it.serialize().indentBy(4) })
            appendLine()
            append("}")
        }

    fun cardNamed(name: String): CardDefinition =
        cardsByName[name] ?: error("Unknown card definition: $name.")

    fun update(
        name: String = this.name,
        cardsByName: Map<String, CardDefinition> = this.cardsByName
    ): CardDatabase =
        if (
            name === this.name &&
            cardsByName === this.cardsByName
        ) {
            this
        } else {
            CardDatabase(
                name = name,
                cardsByName = cardsByName
            )
        }
}

internal fun String.serializeString(): String =
    buildString {
        append('"')
        for (character in this@serializeString) {
            append(
                when (character) {
                    '\\' -> "\\\\"
                    '"' -> "\\\""
                    '\n' -> "\\n"
                    '\r' -> "\\r"
                    '\t' -> "\\t"
                    else -> character
                }
            )
        }
        append('"')
    }

internal fun String.indentBy(spaces: Int): String {
    val indentation = " ".repeat(spaces)
    return lineSequence().joinToString("\n") { line ->
        if (line.isEmpty()) line else indentation + line
    }
}

private fun StringBuilder.appendListAssignment(name: String, values: List<String>) {
    if (values.isNotEmpty()) {
        appendLine("    $name = ${values.serializeStringList()}")
    }
}

private fun StringBuilder.appendManaCostAssignment(values: List<ManaSymbol>) {
    if (values.isNotEmpty()) {
        appendLine("    manaCost = listOf(${values.joinToString { it.name }})")
    }
}

private fun List<String>.serializeStringList(): String =
    joinToString(prefix = "listOf(", postfix = ")") { it.serializeString() }
