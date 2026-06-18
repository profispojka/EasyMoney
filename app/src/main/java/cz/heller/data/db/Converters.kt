package cz.heller.data.db

import androidx.room.TypeConverter

/** Enumy ukládáme jako jejich name (String). */
class Converters {
    @TypeConverter fun fromAccountType(v: AccountType): String = v.name
    @TypeConverter fun toAccountType(v: String): AccountType = AccountType.valueOf(v)

    @TypeConverter fun fromCategoryType(v: CategoryType): String = v.name
    @TypeConverter fun toCategoryType(v: String): CategoryType = CategoryType.valueOf(v)

    @TypeConverter fun fromRecordType(v: RecordType): String = v.name
    @TypeConverter fun toRecordType(v: String): RecordType = RecordType.valueOf(v)

    @TypeConverter fun fromPaymentType(v: PaymentType?): String? = v?.name
    @TypeConverter fun toPaymentType(v: String?): PaymentType? = v?.let { PaymentType.valueOf(it) }

    @TypeConverter fun fromRecordSource(v: RecordSource): String = v.name
    @TypeConverter fun toRecordSource(v: String): RecordSource = RecordSource.valueOf(v)

    @TypeConverter fun fromBudgetPeriod(v: BudgetPeriod): String = v.name
    @TypeConverter fun toBudgetPeriod(v: String): BudgetPeriod = BudgetPeriod.valueOf(v)

    @TypeConverter fun fromFrequencyUnit(v: FrequencyUnit): String = v.name
    @TypeConverter fun toFrequencyUnit(v: String): FrequencyUnit = FrequencyUnit.valueOf(v)

    // Seznam ID (UUID/slug bez čárky) — ukládáme jako CSV.
    @TypeConverter fun fromStringList(v: List<String>): String = v.joinToString(",")
    @TypeConverter fun toStringList(v: String): List<String> =
        if (v.isBlank()) emptyList() else v.split(",")
}
