package io.github.dot166.otwarchive

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.dot166.otwarchive.ui.screens.ChapterScreen
import io.github.dot166.otwarchive.ui.screens.HomeScreen
import io.github.dot166.otwarchive.ui.screens.WorkScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

object Routes {
    @Serializable
    object Home

    @Serializable
    data class Work(val workId: Long)

    @Serializable
    data class Chapter(val workId: Long, val chapterNumber: Int)

    @Composable
    fun AppNavHost(
        modifier: Modifier,
        navController: NavHostController,
        startDestination: KClass<*>,
        viewModel: MainViewModel,
    ) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                slideIn { IntOffset(it.width, 0) } + fadeIn()
            },
            exitTransition = {
                slideOut { IntOffset(-it.width, 0) } + fadeOut()
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                scaleOut(
                    targetScale = 0.85F,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.8f, pivotFractionY = 0.5f)
                ) + fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
            predictivePopEnterTransition = {
                EnterTransition.None
            },
            predictivePopExitTransition = {
                scaleOut(
                    targetScale = 0.85F,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.8f, pivotFractionY = 0.5f)
                ) + fadeOut(spring(stiffness = Spring.StiffnessMedium))
            },
        ) {
            composable(Home::class) { HomeScreen(viewModel) }
            composable(Work::class) { navBackStack ->
                val payload = navBackStack.toRoute<Work>()
                WorkScreen(viewModel, payload.workId)
            }
            composable(Chapter::class) { navBackStack ->
                val payload = navBackStack.toRoute<Chapter>()
                ChapterScreen(viewModel, payload.workId, payload.chapterNumber)
            }
        }
    }
}