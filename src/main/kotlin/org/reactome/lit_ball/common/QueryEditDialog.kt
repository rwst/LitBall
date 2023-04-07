@file:Suppress("FunctionName")

package org.reactome.lit_ball.common

import androidx.compose.runtime.Composable
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1

@Composable
fun QueryEditDialog(
    item: Query,
    onCloseClicked: KFunction0<Unit>,
    onTextChanged: KFunction1<String, Unit>,
    onDoneChanged: () -> Unit
) {
}