package cz.heller.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPostOffice
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Soap
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector

/** Mapuje klíč ikony (z dat) na monochromatickou Material ikonu. */
object CategoryIcons {

    /** Klíče dostupné pro výběr u vlastních kategorií (řazené tematicky). */
    val keys: List<String> = listOf(
        // Jídlo, nákupy
        "restaurant", "fastfood", "local_cafe", "shopping_cart", "shopping_bag",
        "checkroom", "diamond", "spa", "child_care", "pets", "soap",
        // Elektronika, komunikace, volný čas
        "devices", "smartphone", "wifi", "tv", "sports_esports", "card_giftcard",
        "confirmation_number", "casino", "weekend",
        // Bydlení, domácnost
        "home", "apartment", "vpn_key", "bolt", "build", "handyman", "room_service",
        "yard",
        // Doprava, cestování
        "directions_bus", "directions_car", "local_taxi", "train", "flight",
        "beach_access", "local_gas_station", "local_parking",
        // Vzdělání, zábava, zdraví
        "school", "menu_book", "palette", "fitness_center", "theater_comedy",
        "emoji_events", "celebration", "favorite", "medical_services",
        "health_and_safety",
        // Finance
        "account_balance", "local_atm", "savings", "trending_up", "show_chart", "payments",
        "sell", "credit_card", "receipt_long", "request_quote", "gavel", "policy",
        "shield", "verified_user", "support_agent", "business_center",
        // Lidé, dárky, ostatní
        "family_restroom", "handshake", "volunteer_activism", "collections",
        "local_post_office", "undo", "more_horiz", "help_outline",
    )

    fun forKey(key: String): ImageVector = when (key) {
        "restaurant" -> Icons.Filled.Restaurant
        "fastfood" -> Icons.Filled.Fastfood
        "local_cafe" -> Icons.Filled.LocalCafe
        "shopping_cart" -> Icons.Filled.ShoppingCart
        "shopping_bag" -> Icons.Filled.ShoppingBag
        "checkroom" -> Icons.Filled.Checkroom
        "diamond" -> Icons.Filled.Diamond
        "spa" -> Icons.Filled.Spa
        "child_care" -> Icons.Filled.ChildCare
        "pets" -> Icons.Filled.Pets
        "soap" -> Icons.Filled.Soap
        "devices" -> Icons.Filled.Devices
        "smartphone" -> Icons.Filled.Smartphone
        "wifi" -> Icons.Filled.Wifi
        "tv" -> Icons.Filled.Tv
        "sports_esports" -> Icons.Filled.SportsEsports
        "card_giftcard" -> Icons.Filled.CardGiftcard
        "confirmation_number" -> Icons.Filled.ConfirmationNumber
        "casino" -> Icons.Filled.Casino
        "weekend" -> Icons.Filled.Weekend
        "home" -> Icons.Filled.Home
        "apartment" -> Icons.Filled.Apartment
        "local_atm" -> Icons.Filled.LocalAtm
        "vpn_key" -> Icons.Filled.VpnKey
        "bolt" -> Icons.Filled.Bolt
        "build" -> Icons.Filled.Build
        "handyman" -> Icons.Filled.Handyman
        "room_service" -> Icons.Filled.RoomService
        "yard" -> Icons.Filled.Yard
        "directions_bus" -> Icons.Filled.DirectionsBus
        "directions_car" -> Icons.Filled.DirectionsCar
        "local_taxi" -> Icons.Filled.LocalTaxi
        "train" -> Icons.Filled.Train
        "flight" -> Icons.Filled.Flight
        "beach_access" -> Icons.Filled.BeachAccess
        "local_gas_station" -> Icons.Filled.LocalGasStation
        "local_parking" -> Icons.Filled.LocalParking
        "car_repair" -> Icons.Filled.CarRepair
        "car_rental" -> Icons.Filled.CarRental
        "school" -> Icons.Filled.School
        "menu_book" -> Icons.Filled.MenuBook
        "palette" -> Icons.Filled.Palette
        "fitness_center" -> Icons.Filled.FitnessCenter
        "theater_comedy" -> Icons.Filled.TheaterComedy
        "emoji_events" -> Icons.Filled.EmojiEvents
        "celebration" -> Icons.Filled.Celebration
        "favorite" -> Icons.Filled.Favorite
        "medical_services" -> Icons.Filled.MedicalServices
        "health_and_safety" -> Icons.Filled.HealthAndSafety
        "account_balance" -> Icons.Filled.AccountBalance
        "savings" -> Icons.Filled.Savings
        "trending_up" -> Icons.Filled.TrendingUp
        "show_chart" -> Icons.Filled.ShowChart
        "payments" -> Icons.Filled.Payments
        "sell" -> Icons.Filled.Sell
        "credit_card" -> Icons.Filled.CreditCard
        "receipt_long" -> Icons.Filled.ReceiptLong
        "request_quote" -> Icons.Filled.RequestQuote
        "gavel" -> Icons.Filled.Gavel
        "policy" -> Icons.Filled.Policy
        "shield" -> Icons.Filled.Shield
        "verified_user" -> Icons.Filled.VerifiedUser
        "support_agent" -> Icons.Filled.SupportAgent
        "business_center" -> Icons.Filled.BusinessCenter
        "family_restroom" -> Icons.Filled.FamilyRestroom
        "handshake" -> Icons.Filled.Handshake
        "volunteer_activism" -> Icons.Filled.VolunteerActivism
        "collections" -> Icons.Filled.Collections
        "local_post_office" -> Icons.Filled.LocalPostOffice
        "undo" -> Icons.Filled.Replay
        "more_horiz" -> Icons.Filled.MoreHoriz
        "help_outline" -> Icons.Filled.HelpOutline
        else -> Icons.Filled.Category
    }
}
