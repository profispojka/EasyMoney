package cz.calmmoney.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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

    // v5 → v6: plánovaná platba si pamatuje, do kdy je zaplaceno (párování s transakcemi).
    // Nedestruktivní — jen přidá sloupec, existující data (záznamy, platby) zůstanou.
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE planned_payments ADD COLUMN paidThroughEpochDay INTEGER")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CalmMoneyDatabase =
        Room.databaseBuilder(context, CalmMoneyDatabase::class.java, CalmMoneyDatabase.NAME)
            .addMigrations(MIGRATION_5_6)
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

                // Doplní kategorie přidané po v5 (např. „Práce / Podnikání") i do existující DB.
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    insertCategories(db, DefaultCategories.extras())
                }
            })
            // Dev: při změně schématu DB znovu vytvoř (před vydáním, žádná reálná data).
            .fallbackToDestructiveMigration()
            .build()

    private fun seedCategories(db: SupportSQLiteDatabase) = insertCategories(db, DefaultCategories.all())

    private fun insertCategories(db: SupportSQLiteDatabase, categories: List<cz.calmmoney.data.db.CategoryEntity>) {
        categories.forEach { c ->
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
