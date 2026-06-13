package cz.calmmoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cz.calmmoney.core.designsystem.theme.CalmMoneyTheme
import cz.calmmoney.core.navigation.CalmMoneyApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalmMoneyTheme {
                CalmMoneyApp()
            }
        }
    }
}
