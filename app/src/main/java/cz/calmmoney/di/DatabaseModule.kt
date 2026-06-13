package cz.calmmoney.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.calmmoney.data.db.AccountDao
import cz.calmmoney.data.db.CalmMoneyDatabase
import cz.calmmoney.data.db.CategoryDao
import cz.calmmoney.data.db.DefaultCategories
import cz.calmmoney.data.db.RecordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CalmMoneyDatabase =
        Room.databaseBuilder(context, CalmMoneyDatabase::class.java, CalmMoneyDatabase.NAME)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedCategories(db)
                }

                // Při dev destruktivní migraci se tabulky znovu vytvoří — naseeduj znovu.
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    super.onDestructiveMigration(db)
                    seedCategories(db)
                }
            })
            // Dev: při změně schématu DB znovu vytvoř (před vydáním, žádná reálná data).
            .fallbackToDestructiveMigration()
            .build()

    private fun seedCategories(db: SupportSQLiteDatabase) {
        DefaultCategories.all().forEach { c ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (id, name, type, parentId, icon, sortOrder, isDefault) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    c.id, c.name, c.type.name, c.parentId, c.icon, c.sortOrder,
                    if (c.isDefault) 1 else 0,
                ),
            )
        }
    }

    @Provides
    fun provideAccountDao(db: CalmMoneyDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideCategoryDao(db: CalmMoneyDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideRecordDao(db: CalmMoneyDatabase): RecordDao = db.recordDao()

    @Provides
    fun provideBudgetDao(db: CalmMoneyDatabase): cz.calmmoney.data.db.BudgetDao = db.budgetDao()

    @Provides
    fun providePlannedPaymentDao(db: CalmMoneyDatabase): cz.calmmoney.data.db.PlannedPaymentDao =
        db.plannedPaymentDao()
}
