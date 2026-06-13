package cz.calmmoney.feature.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCategoryViewModel @Inject constructor(
    private val categories: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val categoryId: String? = savedStateHandle.get<String>("categoryId")
    private val typeArg: String? = savedStateHandle.get<String>("type")
    val isEditing: Boolean get() = categoryId != null
    val defaultType: CategoryType =
        typeArg?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() } ?: CategoryType.EXPENSE

    val all: StateFlow<List<CategoryEntity>> =
        categories.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val editing: StateFlow<CategoryEntity?> =
        all.map { list -> categoryId?.let { id -> list.firstOrNull { it.id == id } } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(name: String, type: CategoryType, parentId: String?, icon: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (categoryId != null) categories.update(categoryId, name, parentId, icon)
            else categories.create(name, type, parentId, icon)
            onDone()
        }
    }
}

@Composable
fun AddCategoryScreen(onClose: () -> Unit, vm: AddCategoryViewModel = hiltViewModel()) {
    val all by vm.all.collectAsStateWithLifecycle()
    val editing by vm.editing.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (vm.isEditing) "Upravit kategorii" else "Nová kategorie",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Zavřít") }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        if (vm.isEditing && editing == null) {
            Text("Načítám…", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
        } else {
            CategoryForm(
                all = all,
                initial = editing,
                defaultType = vm.defaultType,
                onSubmit = { name, type, parentId, icon -> vm.save(name, type, parentId, icon, onClose) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryForm(
    all: List<CategoryEntity>,
    initial: CategoryEntity?,
    defaultType: CategoryType,
    onSubmit: (String, CategoryType, String?, String) -> Unit,
) {
    val isEdit = initial != null
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: defaultType) }
    var parentId by rememberSaveable { mutableStateOf(initial?.parentId) }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: CategoryIcons.keys.first()) }

    val parents = all.filter { it.type == type && it.parentId == null && it.id != initial?.id }
    val canSave = name.isNotBlank()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Název kategorie (např. Dovolená)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )

        if (!isEdit) {
            Text("Typ", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CalmChip("Výdaj", type == CategoryType.EXPENSE, onClick = { type = CategoryType.EXPENSE; parentId = null }, modifier = Modifier.weight(1f))
                CalmChip("Příjem", type == CategoryType.INCOME, onClick = { type = CategoryType.INCOME; parentId = null }, modifier = Modifier.weight(1f))
            }
        }

        Text("Nadřazená kategorie (volitelné)", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip("Žádná (hlavní)", parentId == null, onClick = { parentId = null })
            parents.forEach { p ->
                CalmChip(p.name, parentId == p.id, onClick = { parentId = p.id }, icon = CategoryIcons.forKey(p.icon))
            }
        }

        Text("Ikona", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryIcons.keys.forEach { key ->
                IconPick(key = key, selected = icon == key, onClick = { icon = key })
            }
        }

        CalmPrimaryButton(
            text = "Uložit",
            enabled = canSave,
            onClick = { onSubmit(name, type, parentId, icon) },
        )
    }
}

@Composable
private fun IconPick(key: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Icon(
            CategoryIcons.forKey(key),
            contentDescription = null,
            modifier = Modifier.padding(10.dp).size(26.dp),
        )
    }
}
