package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Car
import com.example.data.model.Expense
import com.example.data.model.Reminder
import com.example.data.model.Trip
import com.example.data.repository.AutoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class LiveTripTelemetry(
    val isRunning: Boolean = false,
    val speed: Float = 0f,
    val rpm: Float = 0f,
    val instantConsumption: Double = 0.0,
    val elapsedSeconds: Int = 0,
    val distanceKm: Double = 0.0,
    val harshAccels: Int = 0,
    val harshBrakes: Int = 0,
    val activeEventAlert: String? = null
)

class AutoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AutoRepository(db.autoDao())

    // --- State Streams ---
    val allCars: StateFlow<List<Car>> = repository.allCars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCar: StateFlow<Car?> = repository.activeCarFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    // --- Live Telemetry (Trip Simulator Engine) ---
    private val _telemetry = MutableStateFlow(LiveTripTelemetry())
    val telemetry: StateFlow<LiveTripTelemetry> = _telemetry.asStateFlow()

    private var simulationJob: Job? = null

    // --- AI Insight States ---
    private val _activeTripAnalysis = MutableStateFlow<String?>(null)
    val activeTripAnalysis: StateFlow<String?> = _activeTripAnalysis.asStateFlow()

    private val _generalAiInsight = MutableStateFlow<String?>(null)
    val generalAiInsight: StateFlow<String?> = _generalAiInsight.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        // Collect active car data update streams
        viewModelScope.launch {
            activeCar.collect { car ->
                if (car != null) {
                    launch {
                        repository.getExpensesForCar(car.id).collect { _expenses.value = it }
                    }
                    launch {
                        repository.getRemindersForCar(car.id).collect { _reminders.value = it }
                    }
                    launch {
                        repository.getTripsForCar(car.id).collect { _trips.value = it }
                    }
                    _generalAiInsight.value = null
                } else {
                    _expenses.value = emptyList()
                    _reminders.value = emptyList()
                    _trips.value = emptyList()
                }
            }
        }

        // Onboarding Check: Setup a stylish demo car and initial tasks if first launch!
        viewModelScope.launch {
            allCars.first { true } // wait for initial load
            val carsList = allCars.value
            if (carsList.isEmpty()) {
                setupDemoData()
            }
        }
    }

    private suspend fun setupDemoData() {
        val demoCarId = repository.insertCar(
            Car(
                brand = "Volkswagen",
                model = "Golf VII 2.0 TDI",
                year = 2018,
                mileage = 142350,
                fuelType = "Дизель",
                isActive = true
            )
        ).toInt()

        // Insert initial mock expenses inside digital service book
        repository.insertExpense(
            Expense(
                carId = demoCarId,
                category = "Паливо",
                amount = 1850.0,
                date = System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000), // 3 days ago
                description = "Заправка ОККО 35л",
                odometer = 141950
            )
        )
        repository.insertExpense(
            Expense(
                carId = demoCarId,
                category = "Обслуговування",
                amount = 4500.0,
                date = System.currentTimeMillis() - (20L * 24 * 60 * 60 * 1000), // 20 days ago
                description = "Заміна масла Castrol 5W-30 та фільтрів",
                odometer = 140800
            )
        )
        repository.insertExpense(
            Expense(
                carId = demoCarId,
                category = "Страховка",
                amount = 2300.0,
                date = System.currentTimeMillis() - (45L * 24 * 60 * 60 * 1000), // 45 days ago
                description = "Автоцивілка СК 'Гарант'",
                odometer = 139100
            )
        )

        // Insert default initial reminders
        repository.insertReminder(
            Reminder(
                carId = demoCarId,
                title = "Перевірка гальмових дисків",
                description = "Регулярний огляд гальмівної системи кожні 20тис км.",
                dueMileage = 150000,
                category = "Maintenance"
            )
        )
        repository.insertReminder(
            Reminder(
                carId = demoCarId,
                title = "Оновлення страховки",
                description = "Термін дії діючого полісу підходить до кінця.",
                dueDate = System.currentTimeMillis() + (320L * 24 * 60 * 60 * 1000), // 320 days
                category = "Insurance"
            )
        )

        // Add a demo trip to let users explore simulator instantly
        val demoTripId = Trip(
            carId = demoCarId,
            date = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000),
            durationSeconds = 620,
            distanceKm = 12.5,
            averageSpeed = 72.3,
            maxSpeed = 98.0,
            averageRpm = 2100.0,
            averageFuelConsumption = 5.2,
            harshAccels = 0,
            harshBrakes = 0,
            drivingStyleScore = 100,
            drivingStyleCategory = "Спокійний (Еко)",
            aiRecommendations = """
                🌱 **Екологічно та зразково!** 
                Ваш стиль їзди ідеальний: плавні старти та гальмування продовжують термін служби гальмівних колодок на 30%. Середня витрата палива (5.2 л/100км) є оптимальною.
            """.trimIndent()
        )
        repository.insertTrip(demoTripId)
    }

    // --- UI Interactions ---

    fun insertCar(brand: String, model: String, year: Int, mileage: Int, fuelType: String) {
        viewModelScope.launch {
            val newlyAddedCarId = repository.insertCar(
                Car(
                    brand = brand,
                    model = model,
                    year = year,
                    mileage = mileage,
                    fuelType = fuelType,
                    isActive = false // User can select it
                )
            ).toInt()
            
            // If it's the only car, set it active
            if (allCars.value.size == 1) {
                repository.setActiveCar(newlyAddedCarId)
            }
        }
    }

    fun selectActiveCar(carId: Int) {
        viewModelScope.launch {
            repository.setActiveCar(carId)
        }
    }

    fun deleteCar(car: Car) {
        viewModelScope.launch {
            repository.deleteCar(car)
            // If the deleted car was active and we have others left, activate another
            val leftCars = allCars.value.filter { it.id != car.id }
            if (car.isActive && leftCars.isNotEmpty()) {
                repository.setActiveCar(leftCars.first().id)
            }
        }
    }

    fun addExpense(category: String, amount: Double, odometer: Int, description: String, receiptUri: String? = null) {
        val car = activeCar.value ?: return
        viewModelScope.launch {
            val currentOdometer = maxOf(car.mileage, odometer)
            repository.insertExpense(
                Expense(
                    carId = car.id,
                    category = category,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    description = description,
                    odometer = currentOdometer,
                    receiptPhotoUri = receiptUri
                )
            )
            // Update car's mileage if expense mileage is greater than active car's current mileage
            if (currentOdometer > car.mileage) {
                repository.updateCar(car.copy(mileage = currentOdometer))
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addReminder(title: String, description: String, dueDate: Long?, dueMileage: Int?, category: String) {
        val car = activeCar.value ?: return
        viewModelScope.launch {
            repository.insertReminder(
                Reminder(
                    carId = car.id,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    dueMileage = dueMileage,
                    category = category
                )
            )
        }
    }

    fun completeReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.insertReminder(reminder.copy(isCompleted = true))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // --- Telemetry SIMULATOR Implementation ---

    fun startTripSimulation() {
        val car = activeCar.value ?: return
        if (_telemetry.value.isRunning) return

        _telemetry.value = LiveTripTelemetry(
            isRunning = true,
            speed = 0f,
            rpm = 800f,
            instantConsumption = 0.0,
            elapsedSeconds = 0,
            distanceKm = 0.0,
            harshAccels = 0,
            harshBrakes = 0
        )
        _activeTripAnalysis.value = null

        simulationJob = viewModelScope.launch {
            var totalSpeedSum = 0f
            var ticks = 0
            var cumulativeDistance = 0.0
            
            while (_telemetry.value.isRunning) {
                delay(1000)
                ticks++
                
                // Telemetry simulation algorithm with high-fidelity vehicle event indicators
                val previousTelemetry = _telemetry.value
                val isStopping = Random.nextFloat() < 0.08f && previousTelemetry.speed > 50
                val isAccelerating = !isStopping && (Random.nextFloat() < 0.25f || previousTelemetry.speed < 20)
                
                var newSpeed = previousTelemetry.speed
                var activeEvent: String? = null
                var extraAccels = 0
                var extraBrakes = 0

                if (isAccelerating) {
                    val accDelta = Random.nextFloat() * 18f + 5f
                    newSpeed += accDelta
                    if (newSpeed > 140f) newSpeed = 138f
                    
                    if (accDelta > 18f) {
                        extraAccels = 1
                        activeEvent = "⚡ Різке прискорення!"
                    }
                } else if (isStopping) {
                    val brakeDelta = Random.nextFloat() * 22f + 8f
                    newSpeed -= brakeDelta
                    if (newSpeed < 0f) newSpeed = 0f
                    
                    if (brakeDelta > 20f) {
                        extraBrakes = 1
                        activeEvent = "⚠️ Різке гальмування!"
                    }
                } else {
                    // cruising
                    newSpeed += Random.nextFloat() * 6f - 3f
                    if (newSpeed < 0f) newSpeed = 5f
                }

                totalSpeedSum += newSpeed
                
                // Calculate virtual RPM based on simulated transmission gears (1st up to 6th)
                val rpm = when (newSpeed) {
                    in 0f..20f -> 1000f + (newSpeed * 60f)
                    in 20f..45f -> 1200f + ((newSpeed - 20f) * 45f)
                    in 45f..70f -> 1400f + ((newSpeed - 45f) * 35f)
                    in 70f..100f -> 1600f + ((newSpeed - 70f) * 25f)
                    else -> 1800f + ((newSpeed - 100f) * 20f)
                }

                // Instant fuel consumption logic (multiplied during harsh accelerations)
                var instantConsumption = 4.5 + (rpm / 1000.0) * 1.5
                if (isAccelerating) {
                    instantConsumption *= 1.8
                    if (extraAccels > 0) instantConsumption *= 1.4
                } else if (isStopping) {
                    instantConsumption = 0.8 // energy regeneration or deceleration cut-off
                }

                // Cumulative distance increment (speed km/h converted to km/s)
                val distanceMultiplier = newSpeed / 3600.0
                cumulativeDistance += distanceMultiplier

                _telemetry.value = previousTelemetry.copy(
                    speed = newSpeed,
                    rpm = rpm,
                    instantConsumption = instantConsumption,
                    elapsedSeconds = ticks,
                    distanceKm = cumulativeDistance,
                    harshAccels = previousTelemetry.harshAccels + extraAccels,
                    harshBrakes = previousTelemetry.harshBrakes + extraBrakes,
                    activeEventAlert = activeEvent
                )

                // Flash notification overlay clear
                if (activeEvent != null) {
                    delay(1200)
                    _telemetry.value = _telemetry.value.copy(activeEventAlert = null)
                }
            }
        }
    }

    fun stopTripSimulation() {
        val car = activeCar.value ?: return
        val currentTelemetry = _telemetry.value
        if (!currentTelemetry.isRunning) return

        simulationJob?.cancel()
        _telemetry.value = currentTelemetry.copy(isRunning = false)

        viewModelScope.launch {
            // Calculate final analytics
            val duration = currentTelemetry.elapsedSeconds
            val distance = maxOf(0.1, currentTelemetry.distanceKm)
            
            // Speed telemetry defaults
            val averageSpeed = if (duration > 0) (distance / (duration / 3600.0)) else 55.0
            val maxSpeed = maxOf(85.0, averageSpeed + 20.0)
            val avgRpm = 1950.0 + (currentTelemetry.harshAccels * 150)
            
            // Fuel averages
            val averageFuelConsumption = if (car.fuelType.lowercase() == "електро") 0.0 else {
                val base = if (car.fuelType.lowercase() == "дизель") 5.5 else 7.8
                maxOf(3.2, base + (currentTelemetry.harshAccels * 0.4) + (currentTelemetry.harshBrakes * 0.2))
            }

            // Drive Score (Starts at 100, penalize for bad habits)
            val scorePenalties = (currentTelemetry.harshAccels * 10) + (currentTelemetry.harshBrakes * 15)
            val finalScore = maxOf(30, 100 - scorePenalties)

            val styleCategory = when {
                finalScore >= 85 -> "Спокійний (Еко)"
                finalScore >= 60 -> "Помірний"
                else -> "Агресивний"
            }

            val newlyRecordedTrip = Trip(
                carId = car.id,
                date = System.currentTimeMillis(),
                durationSeconds = duration,
                distanceKm = Math.round(distance * 100.0) / 100.0,
                averageSpeed = Math.round(averageSpeed * 10.0) / 10.0,
                maxSpeed = Math.round(maxSpeed * 10.0) / 10.0,
                averageRpm = Math.round(avgRpm).toDouble(),
                averageFuelConsumption = Math.round(averageFuelConsumption * 10.0) / 10.0,
                harshAccels = currentTelemetry.harshAccels,
                harshBrakes = currentTelemetry.harshBrakes,
                drivingStyleScore = finalScore,
                drivingStyleCategory = styleCategory,
                aiRecommendations = "Очікуємо аналіз штучним інтелектом..."
            )

            // Save Trip entries to Room.
            // Under `insertTrip`, if score is low, it triggers adaptive reductions on Reminders!
            repository.insertTrip(newlyRecordedTrip)

            // Update Vehicle Mileage odometer
            val updatedCarMileage = car.mileage + newlyRecordedTrip.distanceKm.toInt()
            repository.updateCar(car.copy(mileage = updatedCarMileage))

            // Trigger immediate background AI trip evaluation to make it extremely responsive!
            analyzeTripWithAi(newlyRecordedTrip)
        }
    }

    private fun analyzeTripWithAi(trip: Trip) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                // Fetch analysis from repository wrappers of Gemini REST manager
                val analysisResult = repository.getAiTripAnalysis(trip)
                _activeTripAnalysis.value = analysisResult
                
                // Update trip database entry with real AI commentary!
                trip.id.let { id ->
                    // Since entities require unique updates, save back with AI insight
                    val tripsInDb = repository.getTripsForCar(trip.carId).firstOrNull() ?: emptyList()
                    val targetTrip = tripsInDb.firstOrNull { it.date == trip.date }
                    if (targetTrip != null) {
                        repository.insertTrip(targetTrip.copy(aiRecommendations = analysisResult))
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoControlViewModel", "Error fetching trip AI analysis", e)
                _activeTripAnalysis.value = "Помилка зв'язку з ШІ. Будь ласка, перевірте з'єднання."
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // --- Complex Diagnostics & Overall Analytical Insights Suite ---

    fun generateOverallAiInsight() {
        val car = activeCar.value ?: return
        val currentExpenses = expenses.value
        val currentTrips = trips.value

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val insightResult = repository.getAiGeneralAssessment(car, currentExpenses, currentTrips)
                _generalAiInsight.value = insightResult
            } catch (e: Exception) {
                Log.e("AutoControlViewModel", "Error generating overall assessment", e)
                _generalAiInsight.value = "Будь ласка, додайте більше даних (витрат або поїздок) для генерації ШІ звіту."
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
