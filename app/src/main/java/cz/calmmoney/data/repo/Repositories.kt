package cz.calmmoney.data.repo

import cz.calmmoney.data.db.AccountDao
import cz.calmmoney.data.db.AccountEntity
import cz.calmmoney.data.db.AccountType
import cz.calmmoney.data.db.CategoryDao
import cz.calmmoney.data.db.CategoryEntity
import cz.calmmoney.data.db.CategoryType
import cz.calmmoney.core.recurring.PlannedMatcher
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.data.db.RecordDao
import cz.calmmoney.data.db.RecordEntity
import cz.calmmoney.data.db.RecordType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun newId(): String = UUID.randomUUID().toString()
private fun now(): Long = System.currentTimeMillis()

@Singleton
class AccountRepository @Inject constructor(private val dao: AccountDao) {

    fun observeActive(): Flow<List<AccountEntity>> = dao.observeActive()
    fun observeById(id: String): Flow<AccountEntity?> = dao.observeById(id)
    fun observeNetWorthMinor(): Flow<Long> = dao.observeNetWorthMinor()
    fun observeBalances(): Flow<List<cz.calmmoney.data.db.AccountBalance>> = dao.observeBalances()

    suspend fun create(
        name: String,
        type: AccountType,
        initialBalanceMinor: Long,
        icon: String,
    ): String {
        val id = newId()
        val ts = now()
        dao.upsert(
            AccountEntity(
                id = id,
                name = name.trim(),
                type = type,
                initialBalanceMinor = initialBalanceMinor,
                icon = icon,
                createdAt = ts,
                updatedAt = ts,
            )
        )
        return id
    }

    suspend fun save(account: AccountEntity) = dao.update(account.copy(updatedAt = now()))
    suspend fun delete(account: AccountEntity) = dao.delete(account)
    suspend fun getById(id: String): AccountEntity? = dao.getById(id)

    suspend fun updateAccount(
        id: String,
        name: String,
        type: AccountType,
        initialBalanceMinor: Long,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                name = name.trim().ifBlank { "Účet" },
                type = type,
                initialBalanceMinor = initialBalanceMinor,
                updatedAt = now(),
            )
        )
    }
}

@Singleton
class CategoryRepository @Inject constructor(private val dao: CategoryDao) {
    fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>> = dao.observeByType(type)
    suspend fun upsert(category: CategoryEntity) = dao.upsert(category)
    suspend fun getById(id: String): CategoryEntity? = dao.getById(id)

    suspend fun create(name: String, type: CategoryType, parentId: String?, icon: String): String {
        val id = newId()
        dao.upsert(
            CategoryEntity(
                id = id,
                name = name.trim().ifBlank { "Kategorie" },
                type = type,
                parentId = parentId,
                icon = icon,
                sortOrder = 100_000, // vlastní kategorie až za přednastavené
                isDefault = false,
            )
        )
        return id
    }

    suspend fun update(id: String, name: String, parentId: String?, icon: String) {
        val existing = dao.getById(id) ?: return
        dao.upsert(existing.copy(name = name.trim().ifBlank { "Kategorie" }, parentId = parentId, icon = icon))
    }

    /** Smaže kategorii; je-li to skupina, i její podkategorie. */
    suspend fun deleteWithChildren(category: CategoryEntity) {
        dao.deleteChildren(category.id)
        dao.delete(category)
    }
}

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: cz.calmmoney.data.db.BudgetDao,
) {
    fun observeAll(): Flow<List<cz.calmmoney.data.db.BudgetEntity>> = dao.observeAll()

    suspend fun create(
        name: String,
        categoryGroupIds: List<String>,
        amountMinor: Long,
        period: cz.calmmoney.data.db.BudgetPeriod,
    ) {
        val ts = now()
        dao.upsert(
            cz.calmmoney.data.db.BudgetEntity(
                id = newId(),
                name = name.trim().ifBlank { "Rozpočet" },
                categoryGroupIds = categoryGroupIds,
                amountMinor = amountMinor,
                period = period,
                createdAt = ts,
                updatedAt = ts,
            )
        )
    }

    suspend fun delete(budget: cz.calmmoney.data.db.BudgetEntity) = dao.delete(budget)
}

@Singleton
class PlannedPaymentRepository @Inject constructor(
    private val dao: cz.calmmoney.data.db.PlannedPaymentDao,
    private val recordDao: RecordDao,
) {
    fun observeAll(): Flow<List<cz.calmmoney.data.db.PlannedPaymentEntity>> = dao.observeAll()
    fun observeById(id: String): Flow<cz.calmmoney.data.db.PlannedPaymentEntity?> = dao.observeById(id)
    suspend fun getById(id: String): cz.calmmoney.data.db.PlannedPaymentEntity? = dao.getById(id)

    suspend fun update(
        id: String,
        name: String,
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        frequencyUnit: cz.calmmoney.data.db.FrequencyUnit,
        frequencyCount: Int,
        startEpochDay: Long,
        endEpochDay: Long?,
        note: String?,
    ) {
        val existing = dao.getById(id) ?: return
        dao.upsert(
            existing.copy(
                name = name.trim().ifBlank { "Platba" },
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = amountMinor,
                frequencyUnit = frequencyUnit,
                frequencyCount = frequencyCount,
                startEpochDay = startEpochDay,
                endEpochDay = endEpochDay,
                note = note?.trim()?.ifBlank { null },
                updatedAt = now(),
            )
        )
    }

    suspend fun create(
        name: String,
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        frequencyUnit: cz.calmmoney.data.db.FrequencyUnit,
        frequencyCount: Int,
        startEpochDay: Long,
        endEpochDay: Long?,
        note: String?,
    ) {
        val ts = now()
        // Starší výskyty (před dneškem) ber jako vyřízené, ať nová platba nehází falešné „po splatnosti".
        val paidThrough = PlannedPayments.lastOccurrenceBefore(
            startEpochDay, frequencyUnit, frequencyCount, endEpochDay,
        )
        dao.upsert(
            cz.calmmoney.data.db.PlannedPaymentEntity(
                id = newId(),
                name = name.trim().ifBlank { "Platba" },
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = amountMinor,
                frequencyUnit = frequencyUnit,
                frequencyCount = frequencyCount,
                startEpochDay = startEpochDay,
                endEpochDay = endEpochDay,
                note = note?.trim()?.ifBlank { null },
                paidThroughEpochDay = paidThrough,
                createdAt = ts,
                updatedAt = ts,
            )
        )
    }

    /** Označí splatný výskyt jako zaplacený (ručně napojený na transakci) — zmizí z nadcházejících. */
    suspend fun markPaid(id: String, throughEpochDay: Long) = dao.setPaidThrough(id, throughEpochDay, now())

    /**
     * Projde všechny plánované platby a podle existujících transakcí posune „zaplaceno do"
     * (auto-párování). Volá se po Fio synchronizaci.
     */
    suspend fun reconcileAll() {
        val payments = dao.observeAll().first()
        if (payments.isEmpty()) return
        val records = recordDao.observeAll().first()
        val today = LocalDate.now()
        payments.forEach { p ->
            val newPaid = PlannedMatcher.reconcile(p, records, today)
            if (newPaid != null && newPaid != p.paidThroughEpochDay) {
                dao.setPaidThrough(p.id, newPaid, now())
            }
        }
    }

    suspend fun delete(payment: cz.calmmoney.data.db.PlannedPaymentEntity) = dao.delete(payment)
}

@Singleton
class RecordRepository @Inject constructor(
    private val dao: RecordDao,
    private val categorization: CategorizationRepository,
) {

    fun observeAll(): Flow<List<RecordEntity>> = dao.observeAll()
    fun observeRecent(limit: Int): Flow<List<RecordEntity>> = dao.observeRecent(limit)
    fun observeByAccount(accountId: String): Flow<List<RecordEntity>> = dao.observeByAccount(accountId)
    fun observeById(id: String): Flow<RecordEntity?> = dao.observeById(id)
    suspend fun getById(id: String): RecordEntity? = dao.getById(id)

    /** Příjem nebo výdaj. amountMinor je vždy kladné, směr nese [type]. */
    suspend fun addEntry(
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        dateTime: Long = now(),
        payee: String? = null,
        note: String? = null,
    ) {
        val ts = now()
        dao.upsert(
            RecordEntity(
                id = newId(),
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = amountMinor,
                dateTime = dateTime,
                payee = payee?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                createdAt = ts,
                updatedAt = ts,
            )
        )
        categorization.learn(payee, categoryId)
    }

    /** Převod mezi účty = dva propojené záznamy (odchozí + příchozí). */
    suspend fun addTransfer(
        fromAccountId: String,
        toAccountId: String,
        amountMinor: Long,
        dateTime: Long = now(),
        note: String? = null,
    ) {
        val ts = now()
        val outId = newId()
        val inId = newId()
        dao.upsert(
            RecordEntity(
                id = outId, type = RecordType.TRANSFER, accountId = fromAccountId,
                categoryId = null, amountMinor = amountMinor, dateTime = dateTime,
                note = note?.trim()?.ifBlank { null },
                transferAccountId = toAccountId, transferRecordId = inId, transferOut = true,
                createdAt = ts, updatedAt = ts,
            )
        )
        dao.upsert(
            RecordEntity(
                id = inId, type = RecordType.TRANSFER, accountId = toAccountId,
                categoryId = null, amountMinor = amountMinor, dateTime = dateTime,
                note = note?.trim()?.ifBlank { null },
                transferAccountId = fromAccountId, transferRecordId = outId, transferOut = false,
                createdAt = ts, updatedAt = ts,
            )
        )
    }

    /** Úprava příjmu/výdaje (původ zůstává). */
    suspend fun updateEntry(
        id: String,
        type: RecordType,
        accountId: String,
        categoryId: String?,
        amountMinor: Long,
        dateTime: Long,
        payee: String? = null,
        note: String? = null,
    ) {
        val existing = dao.getById(id) ?: return
        dao.update(
            existing.copy(
                type = type,
                accountId = accountId,
                categoryId = categoryId,
                amountMinor = amountMinor,
                dateTime = dateTime,
                payee = payee?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                updatedAt = now(),
            )
        )
        categorization.learn(payee, categoryId)
    }

    suspend fun delete(record: RecordEntity) = dao.delete(record)

    /** Smaže záznam; u převodu i jeho druhou nohu. */
    suspend fun deleteWithPartner(record: RecordEntity) {
        record.transferRecordId?.let { partnerId ->
            dao.getById(partnerId)?.let { dao.delete(it) }
        }
        dao.delete(record)
    }
}
