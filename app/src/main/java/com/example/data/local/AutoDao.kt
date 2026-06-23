package com.example.data.local

import androidx.room.*
import com.example.data.model.Car
import com.example.data.model.Expense
import com.example.data.model.Reminder
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoDao {

    // --- Cars ---
    @Query("SELECT * FROM cars ORDER BY id DESC")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE isActive = 1 LIMIT 1")
    fun getActiveCarFlow(): Flow<Car?>

    @Query("SELECT * FROM cars WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCar(): Car?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car): Long

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    @Query("UPDATE cars SET isActive = 0")
    suspend fun deactivateAllCars()

    @Transaction
    suspend fun setActiveCar(carId: Int) {
        deactivateAllCars()
        QuerySetActiveCar(carId)
    }

    @Query("UPDATE cars SET isActive = 1 WHERE id = :carId")
    suspend fun QuerySetActiveCar(carId: Int)


    // --- Expenses ---
    @Query("SELECT * FROM expenses WHERE carId = :carId ORDER BY date DESC")
    fun getExpensesForCar(carId: Int): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)


    // --- Reminders ---
    @Query("SELECT * FROM reminders WHERE carId = :carId ORDER BY isCompleted ASC, dueDate ASC, dueMileage ASC")
    fun getRemindersForCar(carId: Int): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)


    // --- Trips ---
    @Query("SELECT * FROM trips WHERE carId = :carId ORDER BY date DESC")
    fun getTripsForCar(carId: Int): Flow<List<Trip>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)
}
