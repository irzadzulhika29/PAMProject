package com.example.pamproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pamproject.ui.screen.AccountDetailScreen
import com.example.pamproject.ui.screen.AccountListScreen
import com.example.pamproject.viewmodel.AccountViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val accountViewModel: AccountViewModel = viewModel()
    val accounts by accountViewModel.accounts.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.AccountList.route,
        modifier = modifier
    ) {
        // Account List Screen
        composable(route = Screen.AccountList.route) {
            AccountListScreen(
                accounts = accounts,
                onAccountClick = { accountId ->
                    navController.navigate(Screen.AccountDetail.createRoute(accountId))
                }
            )
        }

        // Account Detail Screen
        composable(
            route = Screen.AccountDetail.route,
            arguments = listOf(
                navArgument("accountId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getInt("accountId")
            if (accountId != null) {
                AccountDetailScreen(
                    accountId = accountId,
                    onBackClick = { navController.navigateUp() },
                    viewModel = accountViewModel
                )
            } else {
                navController.navigateUp()
            }
        }
    }
}

