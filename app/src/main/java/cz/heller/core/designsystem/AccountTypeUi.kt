package cz.heller.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.annotation.StringRes
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import cz.heller.R
import cz.heller.data.db.AccountType

/** Lokalizovaný název (resource id) a monochromatická ikona typu účtu. */
object AccountTypeUi {
    @StringRes
    fun labelRes(type: AccountType): Int = when (type) {
        AccountType.CASH -> R.string.account_type_cash
        AccountType.CHECKING -> R.string.account_type_checking
        AccountType.CREDIT_CARD -> R.string.account_type_credit_card
        AccountType.SAVINGS -> R.string.account_type_savings
        AccountType.INVESTMENT -> R.string.account_type_investment
        AccountType.OTHER -> R.string.account_type_other
    }

    fun icon(type: AccountType): ImageVector = when (type) {
        AccountType.CASH -> Icons.Filled.Payments
        AccountType.CHECKING -> Icons.Filled.AccountBalance
        AccountType.CREDIT_CARD -> Icons.Filled.CreditCard
        AccountType.SAVINGS -> Icons.Filled.Savings
        AccountType.INVESTMENT -> Icons.Filled.TrendingUp
        AccountType.OTHER -> Icons.Filled.AccountBalanceWallet
    }
}
