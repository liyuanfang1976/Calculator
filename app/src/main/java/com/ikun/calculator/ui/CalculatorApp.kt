package com.ikun.calculator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.ikun.calculator.CalculatorAction
import com.ikun.calculator.CalculatorViewModel
import com.ikun.calculator.CalculatorViewModelFactory
import com.ikun.calculator.R


@Composable
fun CalculatorApp() {
    val viewModel: CalculatorViewModel = viewModel(
        factory = CalculatorViewModelFactory(LocalContext.current)
    )
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = showAbout) {
        showAbout = false
    }

    BackHandler(enabled = showMenu) {
        showMenu = false
    }

    Scaffold { innerPadding ->
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "更多",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (state.expression.isNotEmpty()) {
                    Text(
                        text = state.expression,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    text = state.display,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CalculatorButtons(onAction = viewModel::onAction)

            Spacer(modifier = Modifier.navigationBarsPadding())
            Spacer(modifier = Modifier.height(16.dp))
            }

            AnimatedVisibility(
                visible = showMenu,
                enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showMenu = false }
                        )
                )
            }

            AnimatedVisibility(
                visible = showMenu,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 48.dp, end = 26.dp),
                enter = scaleIn(
                    initialScale = 0.9f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ),
                exit = scaleOut(
                    targetScale = 0.9f,
                    transformOrigin = TransformOrigin(1f, 0f),
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(150, easing = FastOutSlowInEasing)
                )
            ) {
                Surface(
                    modifier = Modifier.width(110.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 6.dp,
                    tonalElevation = 3.dp
                ) {
                    Column {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "关于",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    kotlinx.coroutines.delay(250)
                                    showAbout = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showAbout,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
            ) {
                AboutPage(onBack = { showAbout = false })
            }
        }
    }
}

@Composable
private fun CalculatorButtons(onAction: (CalculatorAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalcIconButton(R.drawable.ic_clear, MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Clear)
            CalcIconButton(R.drawable.ic_divide, MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Operator("÷"))
            CalcIconButton(R.drawable.ic_percent, MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Percent)
            CalcIconButton(R.drawable.ic_delete, MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Delete)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalcTextButton("7", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("7"))
            CalcTextButton("8", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("8"))
            CalcTextButton("9", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("9"))
            CalcIconButton(R.drawable.ic_multiply, MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Operator("×"))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalcTextButton("4", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("4"))
            CalcTextButton("5", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("5"))
            CalcTextButton("6", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("6"))
            CalcIconButton(R.drawable.ic_minus, MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Operator("-"))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalcTextButton("1", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("1"))
            CalcTextButton("2", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("2"))
            CalcTextButton("3", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("3"))
            CalcIconButton(R.drawable.ic_plus, MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Operator("+"))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CalcTextButton("+/-", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.PlusMinus)
            CalcTextButton("0", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Number("0"))
            CalcTextButton(".", MaterialTheme.colorScheme.surfaceVariant, onAction, CalculatorAction.Decimal)
            CalcIconButton(R.drawable.ic_equal, MaterialTheme.colorScheme.primary, onAction, CalculatorAction.Equals, MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun RowScope.CalcIconButton(
    iconRes: Int,
    color: Color,
    onAction: (CalculatorAction) -> Unit,
    action: CalculatorAction,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Button(
        onClick = { onAction(action) },
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun RowScope.CalcTextButton(
    text: String,
    color: Color,
    onAction: (CalculatorAction) -> Unit,
    action: CalculatorAction,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Button(
        onClick = { onAction(action) },
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
