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
    val accountName = accounts[accountId]?.name
    return when (type) {
        RecordType.EXPENSE -> RecordRowUi(
            id = id,
            iconKey = category?.icon ?: "more_horiz",
            // Titulek = obchodník; pod ním kategorie · účet. U ručních (bez plátce) titulek = kategorie.
            title = payee ?: category?.name ?: "Bez kategorie",
            subtitle = if (payee != null) {
                listOfNotNull(category?.name ?: "Bez kategorie", accountName).joinToString(" · ")
            } else {
                note ?: accountName
            },
            amountMinor = -amountMinor,
        )
        RecordType.INCOME -> RecordRowUi(
            id = id,
            iconKey = category?.icon ?: "payments",
            title = payee ?: category?.name ?: "Příjem",
            subtitle = if (payee != null) {
                listOfNotNull(category?.name ?: "Bez kategorie", accountName).joinToString(" · ")
            } else {
                note ?: accountName
            },
            amountMinor = amountMinor,
        )
        RecordType.TRANSFER -> {
            val here = accounts[accountId]?.name ?: "?"
            // Fio import je jednostranný (bez protiúčtu) — ukaž plátce / poznámku.
            val subtitle = if (transferAccountId == null) {
                payee ?: note ?: here
            } else {
                val there = accounts[transferAccountId]?.name ?: "?"
                if (transferOut == true) "$here → $there" else "$there → $here"
            }
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
