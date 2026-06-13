package cz.calmmoney.data.db

enum class AccountType { CASH, CHECKING, CREDIT_CARD, SAVINGS, INVESTMENT, OTHER }

enum class CategoryType { INCOME, EXPENSE }

enum class RecordType { EXPENSE, INCOME, TRANSFER }

enum class PaymentType { CASH, CARD, TRANSFER, OTHER }

/** Původ záznamu: ruční / z Fio importu (Fáze 3) / jiný import. */
enum class RecordSource { MANUAL, FIO, IMPORT }

enum class BudgetPeriod { WEEK, MONTH, YEAR }

/** Jednotka opakování plánované platby (počet jednotek určuje interval). */
enum class FrequencyUnit { DAY, WEEK, MONTH, YEAR }
