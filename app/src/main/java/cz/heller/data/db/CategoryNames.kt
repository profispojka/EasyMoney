package cz.heller.data.db

import android.content.Context
import androidx.annotation.StringRes
import cz.heller.R

/**
 * Lokalizace názvů přednastavených kategorií podle stabilního `id` (slugu).
 * Vlastní (uživatelské) kategorie mají náhodné UUID id, takže v mapě nejsou
 * a zobrazí se jejich uložený název beze změny.
 */
object CategoryNames {

    private val res: Map<String, Int> = mapOf(
        "food" to R.string.cat_food,
        "food_groceries" to R.string.cat_food_groceries,
        "food_restaurant" to R.string.cat_food_restaurant,
        "food_bar" to R.string.cat_food_bar,
        "shopping" to R.string.cat_shopping,
        "shopping_clothes" to R.string.cat_shopping_clothes,
        "shopping_jewelry" to R.string.cat_shopping_jewelry,
        "shopping_health_beauty" to R.string.cat_shopping_health_beauty,
        "shopping_kids" to R.string.cat_shopping_kids,
        "shopping_home_garden" to R.string.cat_shopping_home_garden,
        "shopping_pets" to R.string.cat_shopping_pets,
        "shopping_electronics" to R.string.cat_shopping_electronics,
        "shopping_gifts" to R.string.cat_shopping_gifts,
        "shopping_office_tools" to R.string.cat_shopping_office_tools,
        "shopping_free_time" to R.string.cat_shopping_free_time,
        "shopping_drugstore" to R.string.cat_shopping_drugstore,
        "housing" to R.string.cat_housing,
        "housing_rent" to R.string.cat_housing_rent,
        "housing_mortgage" to R.string.cat_housing_mortgage,
        "housing_utilities" to R.string.cat_housing_utilities,
        "housing_services" to R.string.cat_housing_services,
        "housing_maintenance" to R.string.cat_housing_maintenance,
        "housing_property_insurance" to R.string.cat_housing_property_insurance,
        "transport" to R.string.cat_transport,
        "transport_public" to R.string.cat_transport_public,
        "transport_taxi" to R.string.cat_transport_taxi,
        "transport_long_distance" to R.string.cat_transport_long_distance,
        "transport_business_trips" to R.string.cat_transport_business_trips,
        "vehicle" to R.string.cat_vehicle,
        "vehicle_fuel" to R.string.cat_vehicle_fuel,
        "vehicle_parking" to R.string.cat_vehicle_parking,
        "vehicle_maintenance" to R.string.cat_vehicle_maintenance,
        "vehicle_rental" to R.string.cat_vehicle_rental,
        "vehicle_vehicle_insurance" to R.string.cat_vehicle_vehicle_insurance,
        "vehicle_leasing" to R.string.cat_vehicle_leasing,
        "life" to R.string.cat_life,
        "life_healthcare" to R.string.cat_life_healthcare,
        "life_wellness" to R.string.cat_life_wellness,
        "life_sport" to R.string.cat_life_sport,
        "life_culture" to R.string.cat_life_culture,
        "life_life_events" to R.string.cat_life_life_events,
        "life_hobbies" to R.string.cat_life_hobbies,
        "life_education" to R.string.cat_life_education,
        "life_books" to R.string.cat_life_books,
        "life_tv" to R.string.cat_life_tv,
        "life_holidays" to R.string.cat_life_holidays,
        "life_charity" to R.string.cat_life_charity,
        "comm" to R.string.cat_comm,
        "comm_phone" to R.string.cat_comm_phone,
        "comm_internet" to R.string.cat_comm_internet,
        "comm_software" to R.string.cat_comm_software,
        "comm_postal" to R.string.cat_comm_postal,
        "financial" to R.string.cat_financial,
        "financial_taxes" to R.string.cat_financial_taxes,
        "financial_insurance" to R.string.cat_financial_insurance,
        "financial_loans" to R.string.cat_financial_loans,
        "financial_fines" to R.string.cat_financial_fines,
        "financial_advisory" to R.string.cat_financial_advisory,
        "financial_fees" to R.string.cat_financial_fees,
        "financial_alimony" to R.string.cat_financial_alimony,
        "investments" to R.string.cat_investments,
        "investments_realty" to R.string.cat_investments_realty,
        "investments_movables" to R.string.cat_investments_movables,
        "investments_financial" to R.string.cat_investments_financial,
        "investments_savings" to R.string.cat_investments_savings,
        "investments_collections" to R.string.cat_investments_collections,
        "income" to R.string.cat_income,
        "income_wage" to R.string.cat_income_wage,
        "income_interest" to R.string.cat_income_interest,
        "income_sale" to R.string.cat_income_sale,
        "income_rental" to R.string.cat_income_rental,
        "income_grants" to R.string.cat_income_grants,
        "income_lending" to R.string.cat_income_lending,
        "income_coupons" to R.string.cat_income_coupons,
        "income_lottery" to R.string.cat_income_lottery,
        "income_refunds" to R.string.cat_income_refunds,
        "income_alimony" to R.string.cat_income_alimony,
        "income_gifts" to R.string.cat_income_gifts,
        "others" to R.string.cat_others,
        "others_missing" to R.string.cat_others_missing,
        "work" to R.string.cat_work,
        "cash_withdrawal" to R.string.cat_cash_withdrawal,
    )

    @StringRes
    fun resFor(id: String): Int? = res[id]

    /** Lokalizovaný název pro přednastavenou kategorii, jinak uložený [fallback]. */
    fun display(context: Context, id: String, fallback: String): String =
        res[id]?.let { context.getString(it) } ?: fallback

    /** Vrátí kopii kategorie s lokalizovaným názvem (pro přednastavené). */
    fun localized(context: Context, category: CategoryEntity): CategoryEntity =
        res[category.id]?.let { category.copy(name = context.getString(it)) } ?: category
}
