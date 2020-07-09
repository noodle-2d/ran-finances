package com.ran.kolibri.common.dao

import com.ran.kolibri.common.entity.Transaction

interface TransactionDao {
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun deleteAllTransactions(): Int
}
