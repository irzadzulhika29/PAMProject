package com.example.pamproject

import androidx.lifecycle.ViewModel
import com.example.pamproject.model.Account
import com.example.pamproject.model.AccountData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccountViewModel : ViewModel() {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        _accounts.value = AccountData.accountList
    }

    fun getAccountById(id: Int): Account? {
        return _accounts.value.find { it.id == id }
    }
}
