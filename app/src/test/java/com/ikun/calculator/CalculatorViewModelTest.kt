package com.ikun.calculator

import com.ikun.calculator.data.CalculatorDatabase
import com.ikun.calculator.data.HistoryDao
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private lateinit var database: CalculatorDatabase
    private lateinit var dao: HistoryDao
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        dao = mockk(relaxed = true)
        every { dao.getAll() } returns emptyFlow()
        database = mockk(relaxed = true)
        every { database.historyDao() } returns dao
        viewModel = CalculatorViewModel(database)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun click(vararg actions: CalculatorAction) {
        actions.forEach { viewModel.onAction(it) }
    }

    private fun num(v: String) = CalculatorAction.Number(v)
    private fun op(v: String) = CalculatorAction.Operator(v)

    private fun assertState(display: String, expression: String = "", isResult: Boolean? = null) {
        val s = viewModel.state.value
        assertEquals(display, s.display)
        assertEquals(expression, s.expression)
        if (isResult != null) assertEquals(isResult, s.isResult)
    }

    // === 初始状态 ===

    @Test
    fun initialState_isZero() {
        assertState("0", "", false)
    }

    // === 数字输入 ===

    @Test
    fun inputNumber_replacesLeadingZero() {
        click(num("5"))
        assertState("5")
    }

    @Test
    fun inputNumber_appendsDigits() {
        click(num("5"), num("5"), num("5"))
        assertState("555")
    }

    @Test
    fun inputNumber_zeroThenDigit_stripsLeadingZero() {
        click(num("0"), num("5"))
        assertState("5")
    }

    @Test
    fun inputNumber_afterOperator_startsNewOperand() {
        click(num("5"), op("+"), num("3"))
        assertState("3", "5 + 3")
    }

    @Test
    fun inputNumber_negativePlaceholder_becomesNegative() {
        click(CalculatorAction.PlusMinus, num("5"))
        assertState("-5")
    }

    @Test
    fun inputNumber_manyLeadingZeros() {
        click(num("0"), num("0"), num("0"), num("7"))
        assertState("7")
    }

    // === 小数点 ===

    @Test
    fun decimal_onFreshState_showsZeroPoint() {
        click(CalculatorAction.Decimal)
        assertState("0.")
    }

    @Test
    fun decimal_appendsToInteger() {
        click(num("5"), CalculatorAction.Decimal)
        assertState("5.")
    }

    @Test
    fun decimal_secondPoint_isIgnored() {
        click(num("5"), CalculatorAction.Decimal, num("5"), CalculatorAction.Decimal)
        assertState("5.5")
    }

    @Test
    fun decimal_afterOperator_startsWithZeroPoint() {
        click(num("5"), op("+"), CalculatorAction.Decimal)
        assertState("0.", "5 + 0.")
    }

    // === 运算符 ===

    @Test
    fun operator_displaysLeftOperand() {
        click(num("5"), op("+"))
        assertState("5", "5 +")
    }

    @Test
    fun operator_replacesPreviousOperator() {
        click(num("5"), op("+"), op("×"))
        assertState("5", "5 ×")
    }

    @Test
    fun operator_chainedComputesAndShowsResult() {
        click(num("5"), op("×"), num("5"), op("+"))
        assertState("25", "25 +")
    }

    @Test
    fun operator_chainedSubtractionKeepsNegativeResult() {
        click(num("3"), op("-"), num("8"), op("+"))
        assertState("-5", "(-5) +")
    }

    @Test
    fun operator_pressedBeforeAnyNumber_ignoresInvalidInput() {
        click(op("+"), num("7"), CalculatorAction.Equals)
        assertState("7", "0 + 7", true)
    }

    // === 等号 ===

    @Test
    fun equals_basicAddition() {
        click(num("2"), op("+"), num("3"), CalculatorAction.Equals)
        assertState("5", "2 + 3", true)
    }

    @Test
    fun equals_subtraction() {
        click(num("9"), op("-"), num("4"), CalculatorAction.Equals)
        assertState("5", "9 - 4", true)
    }

    @Test
    fun equals_multiplication() {
        click(num("6"), op("×"), num("7"), CalculatorAction.Equals)
        assertState("42", "6 × 7", true)
    }

    @Test
    fun equals_division() {
        click(num("8"), op("÷"), num("2"), CalculatorAction.Equals)
        assertState("4", "8 ÷ 2", true)
    }

    @Test
    fun equals_repeatedEquals_repeatsLastOperation() {
        click(num("2"), op("+"), num("3"), CalculatorAction.Equals, CalculatorAction.Equals)
        assertState("8", "5 + 3", true)
    }

    @Test
    fun equals_immediatelyAfterOperator_usesLeftOperand() {
        click(num("2"), op("+"), CalculatorAction.Equals)
        assertState("4", "2 + 2", true)
    }

    @Test
    fun equals_immediatelyAfterMultiply_squaresLeftOperand() {
        click(num("5"), op("×"), CalculatorAction.Equals)
        assertState("25", "5 × 5", true)
    }

    @Test
    fun equals_withoutOperator_doesNothing() {
        click(CalculatorAction.Equals)
        assertState("0", "", false)
    }

    @Test
    fun equals_withoutOperatorAfterNumber_keepsNumber() {
        click(num("5"), CalculatorAction.Equals)
        assertState("5", "", false)
    }

    @Test
    fun equals_afterPercentAddition_usesZeroRightOperand() {
        click(num("2"), num("0"), num("0"), op("+"), num("0"), CalculatorAction.Percent, CalculatorAction.Equals)
        assertState("200", "200 + 0%", true)
    }

    // === 清空 ===

    @Test
    fun clear_resetsCurrentExpression() {
        click(num("5"), op("+"), num("3"), CalculatorAction.Clear)
        assertState("0", "", false)
    }

    @Test
    fun clear_afterResult_resetsAll() {
        click(num("2"), op("+"), num("3"), CalculatorAction.Equals, CalculatorAction.Clear)
        assertState("0", "", false)
    }

    // === 删除 ===

    @Test
    fun delete_removesLastDigit() {
        click(num("1"), num("2"), num("3"), CalculatorAction.Delete)
        assertState("12")
    }

    @Test
    fun delete_singleDigit_becomesZero() {
        click(num("5"), CalculatorAction.Delete)
        assertState("0")
    }

    @Test
    fun delete_errorState_becomesZero() {
        click(num("5"), op("÷"), num("0"), CalculatorAction.Equals, CalculatorAction.Delete)
        assertState("0")
    }

    @Test
    fun delete_negativeNumber_becomesZero() {
        click(num("5"), CalculatorAction.PlusMinus, CalculatorAction.Delete)
        assertState("0")
    }

    @Test
    fun delete_decimal_removesFractionDigit() {
        click(num("5"), CalculatorAction.Decimal, num("5"), CalculatorAction.Delete)
        assertState("5.")
    }

    @Test
    fun delete_immediatelyAfterOperator_keepsLeftOperandDisplayed() {
        click(num("5"), op("+"), CalculatorAction.Delete)
        assertState("5", "5 +")
    }

    // === 百分号 ===

    @Test
    fun percent_withoutOperator_dividesByHundred() {
        click(num("5"), num("0"), CalculatorAction.Percent)
        assertState("0.5")
    }

    @Test
    fun percent_withAddition_isRelativeToAccumulator() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.Percent)
        assertState("20", "200 + 10%")
    }

    @Test
    fun percent_withMultiplication_isDividedByHundred() {
        click(num("2"), num("0"), num("0"), op("×"), num("1"), num("0"), CalculatorAction.Percent)
        assertState("0.1", "200 × 10%")
    }

    @Test
    fun percent_afterAddition_thenEquals_showsPercentInExpression() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.Percent, CalculatorAction.Equals)
        assertState("220", "200 + 10%", true)
    }

    @Test
    fun percent_afterAddition_thenOperator_computesFirst() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.Percent, op("+"))
        assertState("220", "220 +")
    }

    @Test
    fun percent_afterAddition_thenOperator_thenEquals() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.Percent, op("+"), num("5"), CalculatorAction.Equals)
        assertState("225", "220 + 5", true)
    }

    @Test
    fun percent_afterSubtraction_thenEquals() {
        click(num("2"), num("0"), num("0"), op("-"), num("1"), num("0"), CalculatorAction.Percent, CalculatorAction.Equals)
        assertState("180", "200 - 10%", true)
    }

    @Test
    fun percent_thenDelete_clearsToOperatorOnly() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.Percent, CalculatorAction.Delete)
        assertState("200", "200 +")
    }

    @Test
    fun percent_negativeOperand_showsParentheses() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.PlusMinus, CalculatorAction.Percent)
        assertState("-20", "200 + (-10%)")
    }

    @Test
    fun percent_negativeOperand_thenEquals() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.PlusMinus, CalculatorAction.Percent, CalculatorAction.Equals)
        assertState("180", "200 + (-10%)", true)
    }

    @Test
    fun percent_afterMultiply_thenOperator_computesFirst() {
        click(num("2"), num("0"), num("0"), op("×"), num("1"), num("0"), CalculatorAction.Percent, op("+"))
        assertState("20", "20 +")
    }

    @Test
    fun percent_thenNewNumber_overwritesResult() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.Percent, num("5"))
        assertState("5", "200 + 5")
    }

    @Test
    fun percent_thenPlusMinus_negatesResult() {
        click(num("2"), num("0"), num("0"), op("+"), num("1"), num("0"), CalculatorAction.Percent, CalculatorAction.PlusMinus)
        assertState("-20", "200 + (-20)")
    }

    // === 正负号 ===

    @Test
    fun plusMinus_positiveBecomesNegative() {
        click(num("5"), CalculatorAction.PlusMinus)
        assertState("-5")
    }

    @Test
    fun plusMinus_negativeBecomesPositive() {
        click(num("5"), CalculatorAction.PlusMinus, CalculatorAction.PlusMinus)
        assertState("5")
    }

    @Test
    fun plusMinus_onZero_showsMinusPlaceholder() {
        click(CalculatorAction.PlusMinus)
        assertState("-")
    }

    @Test
    fun plusMinus_afterOperator_startsNegativeOperand() {
        click(num("5"), op("+"), CalculatorAction.PlusMinus)
        assertState("-", "5 +")
    }

    // === 除零 / 错误 ===

    @Test
    fun negativeOperand_inExpression_getsParentheses() {
        click(num("1"), CalculatorAction.PlusMinus, op("+"), num("2"), CalculatorAction.PlusMinus)
        assertState("-2", "(-1) + (-2)")
    }

    @Test
    fun negativeOperand_inEqualsExpression_getsParentheses() {
        click(num("1"), CalculatorAction.PlusMinus, op("+"), num("2"), CalculatorAction.PlusMinus, CalculatorAction.Equals)
        assertState("-3", "(-1) + (-2)", true)
    }

    @Test
    fun negativeLeftOperand_inExpression_getsParentheses() {
        click(num("5"), CalculatorAction.PlusMinus, op("×"), num("2"))
        assertState("2", "(-5) × 2")
    }

    @Test
    fun negativeRightOperand_subtraction_getsParentheses() {
        click(num("5"), op("-"), num("3"), CalculatorAction.PlusMinus)
        assertState("-3", "5 - (-3)")
    }

    @Test
    fun negativeRightOperand_subtraction_equals() {
        click(num("5"), op("-"), num("3"), CalculatorAction.PlusMinus, CalculatorAction.Equals)
        assertState("8", "5 - (-3)", true)
    }

    @Test
    fun bothNegative_multiplication_equals() {
        click(num("2"), CalculatorAction.PlusMinus, op("×"), num("3"), CalculatorAction.PlusMinus, CalculatorAction.Equals)
        assertState("6", "(-2) × (-3)", true)
    }

    @Test
    fun repeatedEquals_withNegativeOperand_getsParentheses() {
        click(num("2"), op("+"), num("3"), CalculatorAction.PlusMinus, CalculatorAction.Equals, CalculatorAction.Equals)
        assertState("-4", "(-1) + (-3)", true)
    }

    @Test
    fun divideByZero_showsError() {
        click(num("5"), op("÷"), num("0"), CalculatorAction.Equals)
        assertState("Error", "5 ÷ 0", true)
    }

    @Test
    fun zeroDivideZero_showsError() {
        click(num("0"), op("÷"), num("0"), CalculatorAction.Equals)
        assertState("Error", "0 ÷ 0", true)
    }

    @Test
    fun negativeDivideZero_showsError() {
        click(num("5"), CalculatorAction.PlusMinus, op("÷"), num("0"), CalculatorAction.Equals)
        assertState("Error", "(-5) ÷ 0", true)
    }

    @Test
    fun errorThenNumber_startsFresh() {
        click(num("5"), op("÷"), num("0"), CalculatorAction.Equals, num("9"))
        assertState("9")
    }

    // === 浮点精度 ===

    @Test
    fun floatingPointAddition_isRoundedSensibly() {
        click(num("0"), CalculatorAction.Decimal, num("1"),
            op("+"),
            num("0"), CalculatorAction.Decimal, num("2"),
            CalculatorAction.Equals)
        assertState("0.3", "0.1 + 0.2", true)
    }

    @Test
    fun division_nonTerminating_isRounded() {
        click(num("1"), op("÷"), num("3"), CalculatorAction.Equals)
        assertState("0.333333333333333", "1 ÷ 3", true)
    }

    // === 乱按 / 健壮性冒烟 ===

    @Test
    fun crazySequence_operatorsAndSigns_doesNotCrash() {
        click(op("+"), CalculatorAction.PlusMinus, op("×"), CalculatorAction.Delete,
            CalculatorAction.Equals, CalculatorAction.Percent, CalculatorAction.PlusMinus,
            CalculatorAction.Equals)
        // 只要不抛异常即可，进一步断言状态可读
        val s = viewModel.state.value
        assertTrue(s.display.isNotEmpty())
    }

    @Test
    fun longNumberInput_thenOperator_doesNotCrash() {
        repeat(40) { viewModel.onAction(num("9")) }
        viewModel.onAction(op("×"))
        val s = viewModel.state.value
        assertTrue(s.expression.isNotEmpty())
        assertFalse(s.display.isEmpty())
    }

    @Test
    fun repeatedEquals_manyTimes_doesNotCrash() {
        click(num("1"), op("+"), num("1"), CalculatorAction.Equals)
        repeat(10) { viewModel.onAction(CalculatorAction.Equals) }
        assertTrue(viewModel.state.value.display.isNotEmpty())
    }

    @Test
    fun mixedChaos_fullSequence_doesNotCrash() {
        val actions = listOf(
            num("9"), num("9"), CalculatorAction.Delete, CalculatorAction.Decimal,
            num("5"), CalculatorAction.PlusMinus, op("×"), CalculatorAction.PlusMinus,
            num("2"), CalculatorAction.Percent, op("÷"), CalculatorAction.Delete,
            num("0"), CalculatorAction.Equals, CalculatorAction.Delete,
            num("7"), CalculatorAction.PlusMinus, CalculatorAction.Equals,
            CalculatorAction.Clear
        )
        actions.forEach { viewModel.onAction(it) }
        assertFalse(viewModel.state.value.display.isEmpty())
    }

    // === 历史 ===

    @Test
    fun clearHistory_runsWithoutCrash() {
        viewModel.clearHistory()
        // 无异常即通过；确认状态未被破坏
        assertState("0")
    }

    @Test
    fun equals_withUnknownOperator_showsError() {
        click(num("5"), op("?"), num("3"), CalculatorAction.Equals)
        assertState("Error", "5 ? 3", true)
    }

    @Test
    fun history_startsEmpty() {
        assertTrue(viewModel.history.value.isEmpty())
    }
}