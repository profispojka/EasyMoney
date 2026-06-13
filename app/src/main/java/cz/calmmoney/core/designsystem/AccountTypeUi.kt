package cz.calmmoney.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import cz.calmmoney.data.db.AccountType

/** Český název a monochromatická ikona typu účtu. */
object AccountTypeUi {
    fun label(type: AccountType): String = when (type) {
        AccountType.CASH -> "Hotovost"
        AccountType.CHECKING -> "Běžný účet"
        AccountType.CREDIT_CARD -> "Kreditní karta"
        AccountType.SAVINGS -> "Spoření"
        AccountType.INVESTMENT -> "Investice"
        AccountType.OTHER -> "Jiné"
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
