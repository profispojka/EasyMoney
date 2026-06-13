package cz.calmmoney.feature.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.CategoryIcons
import cz.calmmoney.core.designsystem.component.CalmChip
import cz.calmmoney.core.designsystem.component.CalmPrimaryButton
import cz.calmmoney.data.db.CategoryEntity
import cz.calmmoney.data.db.CategoryType
import cz.calmmoney.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryListItem(val category: CategoryEntity, val isSub: Boolean)

fun buildCategoryList(all: List<CategoryEntity>, type: CategoryType): List<CategoryListItem> {
    val ofType = all.filter { it.type == type }
    val tops = ofType.filter { it.parentId == null }.sortedBy { it.sortOrder }
    val topIds = tops.map { it.id }.toSet()
    val result = mutableListOf<CategoryListItem>()
    tops.forEach { top ->
        result += CategoryListItem(top, false)
        ofType.filter { it.parentId == top.id }.sortedBy { it.sortOrder }.forEach { sub ->
            result += CategoryListItem(sub, true)
        }
    }
    // osiřelé podkategorie (rodič smazán)
    ofType.filter { it.parentId != null && it.parentId !in topIds }.forEach {
        result += CategoryListItem(it, true)
    }
    return result
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categories: CategoryRepository,
) : ViewModel() {
    val all: StateFlow<List<CategoryEntity>> =
        categories.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(category: CategoryEntity) {
        viewModelScope.launch { categories.deleteWithChildren(category) }
    }
}

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onAdd: (CategoryType) -> Unit,
    onEdit: (String) -> Unit,
    vm: CategoriesViewModel = hiltViewModel(),
) {
    val all by vm.all.collectAsStateWithLifecycle()
    var type by remember { mutableStateOf(CategoryType.EXPENSE) }
    var toDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    val items = buildCategoryList(all, type)

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět") }
            Text("Kategorie", style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalmChip("Výdaje", type == CategoryType.EXPENSE, onClick = { type = CategoryType.EXPENSE }, modifier = Modifier.weight(1f))
                CalmChip("Příjmy", type == CategoryType.INCOME, onClick = { type = CategoryType.INCOME }, modifier = Modifier.weight(1f))
            }
            CalmPrimaryButton("+ Nová kategorie", onClick = { onAdd(type) })
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.category.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(item.category.id) }
                        .padding(start = if (item.isSub) 36.dp else 16.dp, end = 8.dp)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(CategoryIcons.forKey(item.category.icon), contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(
                        item.category.name,
                        style = if (item.isSub) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { toDelete = item.category }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Smazat", modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    val c = toDelete
    if (c != null) {
        val hasChildren = all.any { it.parentId == c.id }
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Smazat kategorii?") },
            text = {
                Text(
                    if (hasChildren) "Smaže se „${c.name}“ i její podkategorie. Záznamy zůstanou, ale budou „Bez kategorie“."
                    else "Kategorie „${c.name}“ bude odstraněna. Záznamy s ní zůstanou jako „Bez kategorie“.",
                )
            },
            confirmButton = { TextButton(onClick = { vm.delete(c); toDelete = null }) { Text("Smazat") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Zrušit") } },
        )
    }
}
