package com.ikun.calculator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ikun.calculator.data.CalculatorDatabase
import com.ikun.calculator.data.HistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext

data class CalculatorState(
    val display: String = "0",
    val expression: String = "",
    val isResult: Boolean = false
)

class CalculatorViewModel(private val database: CalculatorDatabase) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = database.historyDao()
        .getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var current = "0"
    private var accumulator: Double? = null
    private var operator: String? = null
    private var expectingOperand = true
    private var lastOperator: String? = null
    private var lastOperand: Double? = null
    private var operatorJustPressed = false
    private var percentDisplay: String? = null
    private var afterPercent = false

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Number -> inputNumber(action.value)
            is CalculatorAction.Operator -> inputOperator(action.value)
            CalculatorAction.Equals -> calculate()
            CalculatorAction.Clear -> clear()
            CalculatorAction.Delete -> delete()
            CalculatorAction.Decimal -> inputDecimal()
            CalculatorAction.Percent -> inputPercent()
            CalculatorAction.PlusMinus -> toggleSign()
        }
    }

    private fun inputNumber(number: String) {
        percentDisplay = null
        afterPercent = false
        val sign = if (current.startsWith("-")) "-" else ""
        when {
            expectingOperand -> {
                current = sign + number
                expectingOperand = false
            }
            current == "0" || current == "-0" -> {
                current = sign + number
            }
            else -> {
                current += number
            }
        }
        operatorJustPressed = false
        updateState()
    }

    private fun inputDecimal() {
        percentDisplay = null
        afterPercent = false
        val sign = if (current.startsWith("-")) "-" else ""
        if (expectingOperand) {
            current = sign + "0."
            expectingOperand = false
        } else if (!current.contains(".")) {
            current += "."
        }
        operatorJustPressed = false
        updateState()
    }

    private fun inputOperator(op: String) {
        when {
            operator != null && !expectingOperand -> {
                val right = current.toDoubleOrNull() ?: return
                val result = compute(accumulator!!, operator!!, right)
                accumulator = result
                current = format(result)
            }
            operator != null && afterPercent -> {
                val right = current.toDoubleOrNull() ?: return
                val result = compute(accumulator!!, operator!!, right)
                accumulator = result
                current = format(result)
            }
            operator == null -> {
                accumulator = current.toDoubleOrNull() ?: return
            }
        }
        percentDisplay = null
        afterPercent = false
        operator = op
        expectingOperand = true
        current = "0"
        operatorJustPressed = true
        updateState()
    }

    private fun inputPercent() {
        val value = current.toDoubleOrNull() ?: return
        val result = when (operator) {
            "+", "-" -> (accumulator ?: 0.0) * value / 100.0
            else -> value / 100.0
        }
        percentDisplay = current
        afterPercent = true
        current = format(result)
        expectingOperand = true
        operatorJustPressed = false
        updateState()
    }

    private fun toggleSign() {
        if (current.toDoubleOrNull() == null) return
        percentDisplay = null
        afterPercent = false
        current = if (current.startsWith("-")) current.removePrefix("-") else "-$current"
        operatorJustPressed = false
        updateState()
    }

    private fun calculate() {
        val left: Double
        val right: Double
        val op: String

        if (operator != null) {
            left = accumulator ?: return
            op = operator!!
            right = if (operatorJustPressed) left else (current.toDoubleOrNull() ?: return)
            lastOperator = op
            lastOperand = right
        } else if (lastOperator != null && lastOperand != null) {
            left = current.toDoubleOrNull() ?: return
            op = lastOperator!!
            right = lastOperand!!
        } else {
            return
        }

        val result = compute(left, op, right)
        val expressionStr = if (afterPercent && percentDisplay != null) {
            val pctStr = percentDisplay!!
            val rightStr = if (pctStr.startsWith("-") && pctStr != "-0") "($pctStr%)" else "$pctStr%"
            "${formatForExpr(left)} $op $rightStr"
        } else {
            "${formatForExpr(left)} $op ${formatForExpr(right)}"
        }
        val resultStr = format(result)
        val isError = resultStr == "Error"

        current = resultStr
        accumulator = null
        operator = null
        expectingOperand = true
        operatorJustPressed = false
        percentDisplay = null
        afterPercent = false

        _state.value = CalculatorState(
            display = resultStr,
            expression = expressionStr,
            isResult = true
        )

        if (!isError) {
            viewModelScope.launch {
                database.historyDao().insert(
                    HistoryEntity(expression = expressionStr, result = resultStr.toString())
                )
            }
        }
    }

    private fun compute(left: Double, op: String, right: Double): Double {
        return when (op) {
            "+" -> left + right
            "-" -> left - right
            "×" -> left * right
            "÷" -> if (right != 0.0) left / right else Double.NaN
            else -> Double.NaN
        }
    }

    private fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val bd = BigDecimal(value).round(MathContext(15)).stripTrailingZeros()
        val plain = bd.toPlainString()
        return if (plain.length <= 15) plain else bd.toString()
    }

    private fun formatForExpr(value: Double): String {
        val s = format(value)
        return if (s.startsWith("-")) "($s)" else s
    }

    private fun clear() {
        current = "0"
        accumulator = null
        operator = null
        expectingOperand = true
        lastOperator = null
        lastOperand = null
        operatorJustPressed = false
        percentDisplay = null
        afterPercent = false
        updateState()
    }

    private fun delete() {
        percentDisplay = null
        afterPercent = false
        if (expectingOperand || current == "Error") {
            current = "0"
            if (operator != null && accumulator != null) {
                operatorJustPressed = true
            }
            updateState()
            return
        }
        val sign = if (current.startsWith("-")) "-" else ""
        val body = current.removePrefix("-")
        val newBody = if (body.length > 1) body.dropLast(1) else "0"
        current = if (newBody == "0") "0" else sign + newBody
        updateState()
    }

    private fun updateState() {
        val expr = if (operator != null && accumulator != null) {
            val leftStr = formatForExpr(accumulator!!)
            when {
                operatorJustPressed || current == "-0" -> "$leftStr $operator"
                afterPercent && percentDisplay != null -> {
                    val pctStr = percentDisplay!!
                    val rightStr = if (pctStr.startsWith("-") && pctStr != "-0") "($pctStr%)" else "$pctStr%"
                    "$leftStr $operator $rightStr"
                }
                else -> {
                    val rightStr = if (current.startsWith("-")) "($current)" else current
                    "$leftStr $operator $rightStr"
                }
            }
        } else {
            ""
        }
        val display = when {
            current == "-0" -> "-"
            operatorJustPressed -> format(accumulator!!)
            else -> current
        }
        _state.value = CalculatorState(
            display = display,
            expression = expr,
            isResult = false
        )
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.historyDao().clearAll()
        }
    }
}

sealed class CalculatorAction {
    data class Number(val value: String) : CalculatorAction()
    data class Operator(val value: String) : CalculatorAction()
    object Equals : CalculatorAction()
    object Clear : CalculatorAction()
    object Delete : CalculatorAction()
    object Decimal : CalculatorAction()
    object Percent : CalculatorAction()
    object PlusMinus : CalculatorAction()
}

class CalculatorViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(CalculatorDatabase.getInstance(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
