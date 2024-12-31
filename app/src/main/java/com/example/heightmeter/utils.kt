package com.example.heightmeter

class DecimalFormatter {

    private val decimalSeparator = '.'

    fun cleanup(input: String): String {
        if (input.matches("\\D".toRegex())) return ""
        if (input.matches("0+".toRegex())) return "0"

        val stringBuilder = StringBuilder()
        var hasDecimalSep = false

        for (char in input) {
            if (char.isDigit()) {
                stringBuilder.append(char)
                continue
            }
            if (char == decimalSeparator && !hasDecimalSep && stringBuilder.isNotEmpty()) {
                stringBuilder.append(char)
                hasDecimalSep = true
            }
        }
        return stringBuilder.toString()
    }
}
