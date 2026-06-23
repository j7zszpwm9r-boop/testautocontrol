package com.example.data.repository

import com.example.data.api.GeminiManager
import com.example.data.local.AutoDao
import com.example.data.model.Car
import com.example.data.model.Expense
import com.example.data.model.Reminder
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

class AutoRepository(private val autoDao: AutoDao) {

    val allCars: Flow<List<Car>> = autoDao.getAllCars()
    val activeCarFlow: Flow<Car?> = autoDao.getActiveCarFlow()

    suspend fun getActiveCar(): Car? = autoDao.getActiveCar()

    suspend fun insertCar(car: Car): Long {
        return autoDao.insertCar(car)
    }

    suspend fun updateCar(car: Car) {
        autoDao.updateCar(car)
    }

    suspend fun deleteCar(car: Car) {
        autoDao.deleteCar(car)
    }

    suspend fun setActiveCar(carId: Int) {
        autoDao.setActiveCar(carId)
    }

    fun getExpensesForCar(carId: Int): Flow<List<Expense>> {
        return autoDao.getExpensesForCar(carId)
    }

    suspend fun insertExpense(expense: Expense) {
        autoDao.insertExpense(expense)

        // Automatically create or adjust corresponding reminders on adding "Service"
        if (expense.category.lowercase() == "service" || expense.category.lowercase() == "обслуговування") {
            // Suggest oil/filter next checkups in 10,000 km or 6 months
            val activeCar = autoDao.getActiveCar()
            if (activeCar != null) {
                val nextChangeMileage = activeCar.mileage + 10000
                val nextChangeDate = System.currentTimeMillis() + (180L * 24 * 60 * 60 * 1000) // 180 days
                
                val autoOilReminder = Reminder(
                    carId = activeCar.id,
                    title = "Заміна оливи (Рекомендована)",
                    description = "Автоматичне нагадування після внесеного сервісу. Заплануйте заміну оливи на пробігу $nextChangeMileage або через 6 місяців.",
                    dueDate = nextChangeDate,
                    dueMileage = nextChangeMileage,
                    category = "Oil",
                    isAIAdaptive = false
                )
                autoDao.insertReminder(autoOilReminder)
            }
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        autoDao.deleteExpense(expense)
    }

    fun getRemindersForCar(carId: Int): Flow<List<Reminder>> {
        return autoDao.getRemindersForCar(carId)
    }

    suspend fun insertReminder(reminder: Reminder) {
        autoDao.insertReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        autoDao.deleteReminder(reminder)
    }

    fun getTripsForCar(carId: Int): Flow<List<Trip>> {
        return autoDao.getTripsForCar(carId)
    }

    suspend fun insertTrip(trip: Trip) {
        autoDao.insertTrip(trip)
        
        // Smart dynamic reminder adjustments based on style!
        // If driving style is Aggressive, we automatically adapt or suggest reminders with narrower intervals.
        if (trip.drivingStyleCategory.lowercase() == "aggressive" || trip.drivingStyleCategory.lowercase() == "агресивний") {
            val reminders = autoDao.getRemindersForCar(trip.carId).firstOrNull() ?: emptyList()
            for (reminder in reminders) {
                // If there's an active oil or brake reminder, adapt it with reduced mileage due to severe wear!
                if (!reminder.isCompleted && (reminder.category.lowercase() == "oil" || reminder.category.lowercase() == "brake" || reminder.category.lowercase() == "олива" || reminder.category.lowercase() == "гальма")) {
                    val currentDueMileage = reminder.dueMileage
                    if (currentDueMileage != null && currentDueMileage > trip.odometerUpdateValue(reminder.dueMileage)) {
                        // Adaptive decrement
                        val shortenedMileage = currentDueMileage - 1500
                        val adaptedReminder = reminder.copy(
                            dueMileage = shortenedMileage,
                            title = "${reminder.title} ⚡ AI",
                            description = "${reminder.description}\n[Коригування AI]: Термін скорочено на 1500 км через виявлений агресивний стиль їзди (часті старт-стоп/високі оберти).",
                            isAIAdaptive = true
                        )
                        autoDao.insertReminder(adaptedReminder)
                    }
                }
            }
        }
    }

    // Helper to extract or parse mileage update values safely
    private fun Trip.odometerUpdateValue(fallback: Int): Int {
        // Just returns a baseline, the active car's mileage is generally larger or equals trip
        return fallback - 1000
    }

    suspend fun deleteTrip(trip: Trip) {
        autoDao.deleteTrip(trip)
    }

    // --- AI Insight Calls ---
    suspend fun getAiTripAnalysis(trip: Trip): String {
        val prompt = """
            Проаналізуй поїздку автомобіля та дай стислі дієві рекомендації в тоні турботливого автоексперта. Поїздка відбулася з наступними характеристиками:
            - Відстань: ${trip.distanceKm} км
            - Тривалість: ${trip.durationSeconds / 60} хв
            - Середня швидкість: ${trip.averageSpeed} км/год
            - Максимальна швидкість: ${trip.maxSpeed} км/год
            - Середні оберти двигуна: ${trip.averageRpm} об/хв
            - Середня витрата палива: ${trip.averageFuelConsumption} л/100 км
            - Кількість різких прискорень: ${trip.harshAccels}
            - Кількість різких гальмувань: ${trip.harshBrakes}
            
            Зроби оцінку водіння в категорії (Спокійний, Помірний, Агресивний) та сформулюй висновок українською мовою. Напиши рекомендації щодо безпеки системи та як економити пальне. Форматуй за допомогою списків та жирного шрифту.
        """.trimIndent()
        
        return GeminiManager.generateAdvice(prompt, "Ти — інтелектуальний помічник водія AutoControl. Твоя мета — допомагати економити кошти та зберігати технічний стан авто за допомогою розумної аналітики.")
    }

    suspend fun getAiGeneralAssessment(car: Car, expenses: List<Expense>, trips: List<Trip>): String {
        val expensesText = expenses.take(10).joinToString("\n") { 
            "- Категорія: ${it.category}, сума: ${it.amount} грн, опис: ${it.description}, дата: ${Date(it.date)}"
        }
        val tripsText = trips.take(5).joinToString("\n") { 
            "- Відстань: ${it.distanceKm} км, стиль: ${it.drivingStyleCategory}, середня витрата: ${it.averageFuelConsumption} л/100км"
        }

        val prompt = """
            Згенеруй комплексний аналіз стану та витрат для автомобіля:
            Марка/Модель: ${car.brand} ${car.model}
            Рік випуску: ${car.year}
            Поточний пробіг: ${car.mileage} км
            Тип палива: ${car.fuelType}
            
            Останні витрати автомобіля:
            $expensesText
            
            Останні поїздки автомобіля:
            $tripsText
            
            На основі цих даних надай глибокий аналіз українською мовою:
            1. Вартість 1 км пробігу (оціночно, враховуючи внесені дані)
            2. Оцінка загальної ефективності та витрат палива
            3. Персоналізований графік обслуговування, адаптований під стиль їзди (екстремальний знос або режим економії)
            4. Чи варто планувати заміну/продаж авто найближчим часом на основі зносу та віку?
            
            Зроби відповідь чіткою, структурованою, професійною, без зайвої води. Використовуй емодзі для покращення візуального сприйняття.
        """.trimIndent()

        return GeminiManager.generateAdvice(prompt, "Ти — персональний автомобільний AI-консультант AutoControl. Твоє завдання — аналізувати великі обсяги технічних даних автомобіля та давати власнику чіткі інструкції для економії коштів.")
    }
}
