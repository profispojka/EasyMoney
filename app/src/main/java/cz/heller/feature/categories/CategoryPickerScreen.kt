package cz.heller.feature.categories

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.R
import cz.heller.core.designsystem.CategoryIcons
import cz.heller.data.db.CategoryType
import cz.heller.data.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CategoryPickerViewModel @Inject constructor(
    categories: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val type: CategoryType =
        runCatching { CategoryType.valueOf(savedStateHandle.get<String>("type") ?: "EXPENSE") }
            .getOrDefault(CategoryType.EXPENSE)

    val items: StateFlow<List<CategoryListItem>> =
        categories.observeAll().map { buildCategoryList(it, type) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * Výběr kategorie na samostatné obrazovce. [onPicked] vrací id kategorie,
 * nebo prázdný řetězec pro „Bez kategorie".
 */
@Composable
fun CategoryPickerScreen(
    onBack: () -> Unit,
    onPicked: (String) -> Unit,
    vm: CategoryPickerViewModel = hiltViewModel(),
) {
    val items by vm.items.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.category_picker_title), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "__none__") {
                Text(
                    stringResource(R.string.no_category),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPicked("") }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
            items(items, key = { it.category.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPicked(item.category.id) }
                        .padding(start = if (item.isSub) 36.dp else 16.dp, end = 16.dp)
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(CategoryIcons.forKey(item.category.icon), contentDescription = null, modifier = Modifier.size(24.dp))
                    Text(
                        item.category.name,
                        style = if (item.isSub) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    )
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
