package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brand: String,
    val model: String,
    val year: Int,
    val mileage: Int,
    val fuelType: String, // Petrol, Diesel, Electric, Hybrid, Gas
    val isActive: Boolean = false
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val category: String, // Fuel, Service, Insurance, Repair, Other
    val amount: Double,
    val date: Long, // timestamp
    val description: String,
    val odometer: Int,
    val receiptPhotoUri: String? = null
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val title: String,
    val description: String,
    val dueDate: Long? = null,
    val dueMileage: Int? = null,
    val isCompleted: Boolean = false,
    val category: String, // Oil, Insurance, Filter, Maintenance, Other
    val isAIAdaptive: Boolean = false // Suggested or optimized by AI based on styled trips
)

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val carId: Int,
    val date: Long,
    val durationSeconds: Int,
    val distanceKm: Double,
    val averageSpeed: Double,
    val maxSpeed: Double,
    val averageRpm: Double,
    val averageFuelConsumption: Double, // L/100km
    val harshAccels: Int,
    val harshBrakes: Int,
    val drivingStyleScore: Int, // 0 to 100
    val drivingStyleCategory: String, // Calm, Moderate, Aggressive
    val aiRecommendations: String // Recommendations generated for this trip
)
