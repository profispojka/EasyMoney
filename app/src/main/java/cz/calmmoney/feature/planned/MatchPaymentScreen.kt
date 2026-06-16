package cz.calmmoney.feature.planned

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.calmmoney.core.designsystem.component.CalmCard
import cz.calmmoney.core.designsystem.component.CalmTopBar
import cz.calmmoney.core.designsystem.component.MoneyAmount
import cz.calmmoney.core.categorize.MerchantText
import cz.calmmoney.core.recurring.PlannedMatcher
import cz.calmmoney.core.time.PlannedPayments
import cz.calmmoney.data.db.RecordEntity
import cz.calmmoney.data.db.RecordType
import cz.calmmoney.data.repo.PlannedPaymentRepository
import cz.calmmoney.data.repo.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.abs

data class MatchCandidate(
    val recordId: String,
    val title: String,
    val dateText: String,
    val signedAmountMinor: Long,
    val amountMatches: Boolean,
)

data class MatchPaymentUiState(
    val loading: Boolean = true,
    val name: String = "",
    val signedAmountMinor: Long = 0,
    val dueText: String = "",
    val overdue: Boolean = false,
    val dueEpochDay: Long? = null,
    val candidates: List<MatchCandidate> = emptyList(),
)

@HiltViewModel
class MatchPaymentViewModel @Inject constructor(
    private val planned: PlannedPaymentRepository,
    records: RecordRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plannedId: String = checkNotNull(savedStateHandle["plannedId"])

    val state: StateFlow<MatchPaymentUiState> = combine(
        planned.observeById(plannedId), records.observeAll(),
    ) { p, recs ->
        if (p == null) return@combine MatchPaymentUiState(loading = false)
        val today = LocalDate.now()
        val due = PlannedPayments.dueOccurrence(
            p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay, p.paidThroughEpochDay,
        )
        val dueEd = due?.toEpochDay()
        val tol = maxOf(500L, p.amountMinor * 3 / 100)
        val payeeKey = MerchantText.key(p.name)

        // Kandidáti: záznamy stejného typu na stejném účtu, nejrelevantnější nahoře —
        // nejdřív shoda obchodníka, pak sedící částka, pak blízkost data splatnosti.
        val candidates = recs
            .filter { it.type == p.type && it.accountId == p.accountId }
            .sortedWith(
                compareBy<RecordEntity>(
                    { if (MerchantText.key(it.payee) == payeeKey) 0 else 1 },
                    { if (abs(it.amountMinor - p.amountMinor) <= tol) 0 else 1 },
                    { dueEd?.let { d -> abs(PlannedMatcher.epochDayOf(it.dateTime) - d) } ?: 0L },
                ),
            )
            .take(40)
            .map { r ->
                val d = LocalDate.ofEpochDay(PlannedMatcher.epochDayOf(r.dateTime))
                MatchCandidate(
                    recordId = r.id,
                    title = r.payee ?: r.note?.take(40) ?: "Platba",
                    dateText = PlannedPayments.formatDate(d),
                    signedAmountMinor = if (r.type == RecordType.EXPENSE) -r.amountMinor else r.amountMinor,
                    amountMatches = abs(r.amountMinor - p.amountMinor) <= tol,
                )
            }

        MatchPaymentUiState(
            loading = false,
            name = p.name,
            signedAmountMinor = if (p.type == RecordType.EXPENSE) -p.amountMinor else p.amountMinor,
            dueText = due?.let { PlannedPayments.formatDate(it) } ?: "—",
            overdue = due != null && due.isBefore(today),
            dueEpochDay = dueEd,
            candidates = candidates,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchPaymentUiState())

    /** Napojí platbu na vybranou transakci = označí splatný výskyt jako zaplacený → zmizí z přehledu. */
    fun link(onDone: () -> Unit) {
        val due = state.value.dueEpochDay ?: return
        viewModelScope.launch {
            planned.markPaid(plannedId, due)
            onDone()
        }
    }
}

@Composable
fun MatchPaymentScreen(
    onBack: () -> Unit,
    vm: MatchPaymentViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        CalmTopBar("Napojit platbu", onBack = onBack)

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
            return@Column
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmCard(Modifier.fillMaxWidth()) {
                Text(state.name, style = MaterialTheme.typography.titleMedium)
                MoneyAmount(state.signedAmountMinor, withSign = true, style = MaterialTheme.typography.headlineSmall)
                if (state.overdue) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            "Po splatnosti",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                        Text(state.dueText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Splatnost ${state.dueText}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "Vyber transakci, která tuhle platbu zaplatila. Platba pak zmizí z nadcházejících.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.candidates.isEmpty()) {
            Text(
                "Na tomhle účtu zatím nejsou žádné transakce k napojení.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            return@Column
        }

        LazyColumn(Modifier.weight(1f)) {
            items(state.candidates, key = { it.recordId }) { c ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.link(onBack) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.title, style = MaterialTheme.typography.bodyLarge)
                        Text(c.dateText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MoneyAmount(c.signedAmountMinor, withSign = true, style = MaterialTheme.typography.bodyLarge)
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
