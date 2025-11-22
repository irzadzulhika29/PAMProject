package com.example.pamproject.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pamproject.model.AccountData
import com.example.pamproject.ui.screen.AccountDetailScreen
import com.example.pamproject.ui.screen.AccountListScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.AccountList.route,
        modifier = modifier
    ) {
        // Account List Screen
        composable(route = Screen.AccountList.route) {
            AccountListScreen(
                accounts = AccountData.accountList,
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
            val account = AccountData.accountList.find { it.id == accountId }

            AccountDetailScreen(
                account = account,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}

