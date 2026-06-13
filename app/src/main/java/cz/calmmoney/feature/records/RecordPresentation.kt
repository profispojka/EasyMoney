package cz.calmmoney.feature.records

import cz.calmmoney.data.db.AccountEntity
import cz.calmmoney.data.db.CategoryEntity
import cz.calmmoney.data.db.RecordEntity
import cz.calmmoney.data.db.RecordType

/** Připravený řádek záznamu pro UI. amountMinor je znaménkové (příjem +, výdaj −). */
data class RecordRowUi(
    val id: String,
    val iconKey: String,
    val title: String,
    val subtitle: String?,
    val amountMinor: Long,
)

fun RecordEntity.toRowUi(
    categories: Map<String, CategoryEntity>,
    accounts: Map<String, AccountEntity>,
): RecordRowUi {
    val category = categoryId?.let { categories[it] }
    return when (type) {
        RecordType.EXPENSE -> RecordRowUi(
            id = id,
            iconKey = category?.icon ?: "more_horiz",
            title = category?.name ?: payee ?: "Bez kategorie",
            subtitle = note ?: accounts[accountId]?.name,
            amountMinor = -amountMinor,
        )
        RecordType.INCOME -> RecordRowUi(
            id = id,
            iconKey = category?.icon ?: "payments",
            title = category?.name ?: payee ?: "Příjem",
            subtitle = note ?: accounts[accountId]?.name,
            amountMinor = amountMinor,
        )
        RecordType.TRANSFER -> {
            val here = accounts[accountId]?.name ?: "?"
            val there = accounts[transferAccountId]?.name ?: "?"
            val subtitle = if (transferOut == true) "$here → $there" else "$there → $here"
            RecordRowUi(
                id = id,
                iconKey = "more_horiz",
                title = "Převod",
                subtitle = subtitle,
                amountMinor = if (transferOut == true) -amountMinor else amountMinor,
            )
        }
    }
}
