package com.example.pamproject.navigation

sealed class Screen(val route: String) {
    object AccountList : Screen("account_list")
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Int) = "account_detail/$accountId"
    }
}

