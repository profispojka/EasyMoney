package cz.calmmoney.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Zůstatek jednoho účtu (počáteční ± jeho záznamy vč. převodů). */
data class AccountBalance(val accountId: String, val balanceMinor: Long)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE archived = 0 ORDER BY sortOrder, name")
    fun observeActive(): Flow<List<AccountEntity>>

    @Query(
        """
        SELECT a.id AS accountId,
          a.initialBalanceMinor + IFNULL(SUM(CASE
            WHEN r.type = 'INCOME' THEN r.amountMinor
            WHEN r.type = 'EXPENSE' THEN -r.amountMinor
            WHEN r.type = 'TRANSFER' AND r.transferOut = 0 THEN r.amountMinor
            WHEN r.type = 'TRANSFER' AND r.transferOut = 1 THEN -r.amountMinor
            ELSE 0 END), 0) AS balanceMinor
        FROM accounts a
        LEFT JOIN records r ON r.accountId = a.id
        WHERE a.archived = 0
        GROUP BY a.id
        """
    )
    fun observeBalances(): Flow<List<AccountBalance>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): AccountEntity?

    /** Čisté jmění = počáteční zůstatky nevyloučených účtů ± jejich záznamy. */
    @Query(
        """
        SELECT
          (SELECT IFNULL(SUM(initialBalanceMinor), 0) FROM accounts WHERE excludeFromStats = 0 AND archived = 0)
          + (SELECT IFNULL(SUM(CASE WHEN type = 'INCOME' THEN amountMinor
                                    WHEN type = 'EXPENSE' THEN -amountMinor
                                    ELSE 0 END), 0)
             FROM records
             WHERE accountId IN (SELECT id FROM accounts WHERE excludeFromStats = 0 AND archived = 0))
        """
    )
    fun observeNetWorthMinor(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder, name")
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE parentId = :parentId")
    suspend fun deleteChildren(parentId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY createdAt")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface PlannedPaymentDao {
    @Query("SELECT * FROM planned_payments ORDER BY startEpochDay")
    fun observeAll(): Flow<List<PlannedPaymentEntity>>

    @Query("SELECT * FROM planned_payments WHERE id = :id")
    fun observeById(id: String): Flow<PlannedPaymentEntity?>

    @Query("SELECT * FROM planned_payments WHERE id = :id")
    suspend fun getById(id: String): PlannedPaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PlannedPaymentEntity)

    @Delete
    suspend fun delete(payment: PlannedPaymentEntity)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records ORDER BY dateTime DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE accountId = :accountId ORDER BY dateTime DESC")
    fun observeByAccount(accountId: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id")
    fun observeById(id: String): Flow<RecordEntity?>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: String): RecordEntity?

    /** Idempotentní vložení Fio záznamu (dedup přes unikátní fioTransactionId). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(record: RecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: RecordEntity)

    @Update
    suspend fun update(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)
}
