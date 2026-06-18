package cz.heller.feature.budgets

import cz.heller.data.db.CategoryEntity
import cz.heller.data.db.CategoryType

/** Skupina výdajových kategorií (top-level, parentId == null). */
data class CategoryGroup(val id: String, val name: String, val icon: String)

/** Vrátí výdajové skupiny (nadřazené kategorie) seřazené dle pořadí. */
fun expenseGroups(categories: List<CategoryEntity>): List<CategoryGroup> =
    categories
        .filter { it.type == CategoryType.EXPENSE && it.parentId == null }
        .sortedBy { it.sortOrder }
        .map { CategoryGroup(it.id, it.name, it.icon) }

/** ID skupiny, do které kategorie patří (sama, je-li top-level; jinak její rodič). */
fun groupIdOf(categoryId: String?, byId: Map<String, CategoryEntity>): String? {
    val c = categoryId?.let { byId[it] } ?: return null
    return c.parentId ?: c.id
}
