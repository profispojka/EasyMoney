package cz.calmmoney.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        RecordEntity::class,
        BudgetEntity::class,
        PlannedPaymentEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CalmMoneyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recordDao(): RecordDao
    abstract fun budgetDao(): BudgetDao
    abstract fun plannedPaymentDao(): PlannedPaymentDao

    companion object {
        const val NAME = "calmmoney.db"
    }
}
