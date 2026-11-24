package com.ntk.callshield.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntk.callshield.ui.theme.BodyFontFamily
import com.ntk.callshield.ui.theme.LocalShieldColors
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ntk.callshield.ui.components.PinLockGateScreen
import com.ntk.callshield.ui.screens.CallsScreen
import com.ntk.callshield.ui.screens.CheckScreen
import com.ntk.callshield.ui.screens.ContactsScreen
import com.ntk.callshield.ui.screens.DuplicateMergerScreen
import com.ntk.callshield.ui.screens.HomeScreen
import com.ntk.callshield.ui.screens.MissionsAndBadgesScreen
import com.ntk.callshield.ui.screens.MyCallerCardScreen
import com.ntk.callshield.ui.screens.OfflineDbManagerScreen
import com.ntk.callshield.ui.screens.OnboardingScreen
import com.ntk.callshield.ui.screens.OverlayCustomizerScreen
import com.ntk.callshield.ui.screens.PrivacyCenterScreen
import com.ntk.callshield.ui.screens.SafeAndBlockListScreen
import com.ntk.callshield.ui.screens.SettingsScreen
import com.ntk.callshield.ui.screens.TestLabScreen
import com.ntk.callshield.viewmodel.CallShieldViewModel

object CallShieldRoutes {
    const val HOME = "home"
    const val CALLS = "calls"
    const val CHECK = "check"
    const val CONTACTS = "contacts"
    const val SETTINGS = "settings"

    const val TEST_LAB = "test_lab"
    const val OFFLINE_DB = "offline_db"
    const val SAFE_BLOCK_LISTS = "safe_block_lists"
    const val DUPLICATE_MERGER = "duplicate_merger"
    const val MY_CALLER_CARD = "my_caller_card"
    const val PRIVACY_CENTER = "privacy_center"
    const val OVERLAY_CUSTOMIZER = "overlay_customizer"
    const val MISSIONS = "missions"
}

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    TopLevelDestination(CallShieldRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    TopLevelDestination(CallShieldRoutes.CALLS, "Calls", Icons.Filled.Call, Icons.Outlined.Call, "nav_calls"),
    TopLevelDestination(CallShieldRoutes.CHECK, "Check", Icons.Filled.Search, Icons.Outlined.Search, "nav_check"),
    TopLevelDestination(CallShieldRoutes.CONTACTS, "Contacts", Icons.Filled.Contacts, Icons.Outlined.Contacts, "nav_contacts"),
    TopLevelDestination(CallShieldRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun NtkCallShieldApp(viewModel: CallShieldViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val snackbarEvent by viewModel.snackbarEvent.collectAsStateWithLifecycle()
    val reopenOnboarding by viewModel.reopenOnboarding.collectAsStateWithLifecycle()

    if (settings.pinLockEnabled && !isUnlocked) {
        PinLockGateScreen(
            biometricAllowed = settings.biometricAllowed,
            errorMessage = pinError,
            onPinComplete = viewModel::verifyPin,
            onBiometricUnlock = viewModel::unlockWithBiometric
        )
        return
    }

    if (!settings.onboardingCompleted || reopenOnboarding) {
        OnboardingScreen(
            viewModel = viewModel,
            canDismiss = reopenOnboarding,
            onDismiss = viewModel::closeSetupWizard
        )
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: CallShieldRoutes.HOME

    var checkInitialSegment by remember { mutableIntStateOf(0) }
    val showPrimaryNav = topLevelDestinations.any { it.route == currentRoute }

    LaunchedEffect(snackbarEvent?.id) {
        val event = snackbarEvent ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = event.message,
            actionLabel = event.undoLabel,
            duration = if (event.undoLabel != null) SnackbarDuration.Short else SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            event.onUndo?.invoke()
        }
        viewModel.clearSnackbar()
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openCallsForNumber(rawNumber: String) {
        viewModel.setCallNumberFilter(rawNumber)
        navigateTopLevel(CallShieldRoutes.CALLS)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp
        val semantic = LocalShieldColors.current

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showPrimaryNav && !useNavigationRail) {
                    Column {
                        HorizontalDivider(thickness = 0.75.dp, color = semantic.cardBorder)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 0.dp
                        ) {
                            topLevelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            maxLines = 1,
                                            softWrap = false,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = BodyFontFamily,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.sp
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = semantic.verified,
                                        selectedTextColor = semantic.verified,
                                        indicatorColor = semantic.verifiedContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (showPrimaryNav && useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = semantic.verified,
                                    selectedTextColor = semantic.verified,
                                    indicatorColor = semantic.verifiedContainer
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    NavHost(
                        navController = navController,
                        startDestination = CallShieldRoutes.HOME,
                        modifier = Modifier.fillMaxSize().widthIn(max = 760.dp)
                    ) {
                        composable(CallShieldRoutes.HOME) {
                            HomeScreen(
                                viewModel = viewModel,
                                onCheckNumberClick = {
                                    checkInitialSegment = 0
                                    navigateTopLevel(CallShieldRoutes.CHECK)
                                },
                                onScanLinkClick = {
                                    checkInitialSegment = 1
                                    navigateTopLevel(CallShieldRoutes.CHECK)
                                },
                                onTestShieldClick = { navController.navigate(CallShieldRoutes.TEST_LAB) },
                                onFinishSetupClick = viewModel::reopenSetupWizard,
                                onOpenCallsTab = { filter ->
                                    viewModel.clearCallNumberFilter()
                                    viewModel.setCallFilter(filter)
                                    navigateTopLevel(CallShieldRoutes.CALLS)
                                }
                            )
                        }
                        composable(CallShieldRoutes.CALLS) {
                            CallsScreen(
                                viewModel = viewModel,
                                onInspectNumberInCheck = { number ->
                                    checkInitialSegment = 0
                                    viewModel.runReverseLookup(number)
                                    navigateTopLevel(CallShieldRoutes.CHECK)
                                },
                                onOpenTestLab = { navController.navigate(CallShieldRoutes.TEST_LAB) }
                            )
                        }
                        composable(CallShieldRoutes.CHECK) {
                            CheckScreen(
                                viewModel = viewModel,
                                initialSection = checkInitialSegment,
                                onOpenCallsForNumber = ::openCallsForNumber
                            )
                        }
                        composable(CallShieldRoutes.CONTACTS) {
                            ContactsScreen(
                                viewModel = viewModel,
                                onOpenMyCallerCard = { navController.navigate(CallShieldRoutes.MY_CALLER_CARD) },
                                onOpenDuplicateMerger = { navController.navigate(CallShieldRoutes.DUPLICATE_MERGER) },
                                onOpenCallsForNumber = ::openCallsForNumber
                            )
                        }
                        composable(CallShieldRoutes.SETTINGS) {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenSetupWizard = viewModel::reopenSetupWizard,
                                onOpenOfflineDb = { navController.navigate(CallShieldRoutes.OFFLINE_DB) },
                                onOpenSafeBlockLists = { navController.navigate(CallShieldRoutes.SAFE_BLOCK_LISTS) },
                                onOpenPrivacyCenter = { navController.navigate(CallShieldRoutes.PRIVACY_CENTER) },
                                onOpenOverlayCustomizer = { navController.navigate(CallShieldRoutes.OVERLAY_CUSTOMIZER) },
                                onOpenMissions = { navController.navigate(CallShieldRoutes.MISSIONS) },
                                onOpenTestLab = { navController.navigate(CallShieldRoutes.TEST_LAB) }
                            )
                        }
                        composable(CallShieldRoutes.TEST_LAB) {
                            TestLabScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(CallShieldRoutes.OFFLINE_DB) {
                            OfflineDbManagerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(CallShieldRoutes.SAFE_BLOCK_LISTS) {
                            SafeAndBlockListScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenCallsForNumber = { number ->
                                    navController.popBackStack()
                                    openCallsForNumber(number)
                                }
                            )
                        }
                        composable(CallShieldRoutes.DUPLICATE_MERGER) {
                            DuplicateMergerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(CallShieldRoutes.MY_CALLER_CARD) {
                            MyCallerCardScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(CallShieldRoutes.PRIVACY_CENTER) {
                            PrivacyCenterScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(CallShieldRoutes.OVERLAY_CUSTOMIZER) {
                            OverlayCustomizerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                        composable(CallShieldRoutes.MISSIONS) {
                            MissionsAndBadgesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
