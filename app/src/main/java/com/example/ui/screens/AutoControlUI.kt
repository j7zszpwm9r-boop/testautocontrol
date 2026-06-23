package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Car
import com.example.data.model.Expense
import com.example.data.model.Reminder
import com.example.data.model.Trip
import com.example.ui.AutoViewModel
import com.example.ui.LiveTripTelemetry
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoControlMainScreen(viewModel: AutoViewModel) {
    val activeCar by viewModel.activeCar.collectAsStateWithLifecycle()
    val allCars by viewModel.allCars.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("dashboard") }
    var showCarSelector by remember { mutableStateOf(false) }
    var showAddCarDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CosmicSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val tabs = listOf(
                    Triple("dashboard", "Головна", Icons.Default.Dashboard),
                    Triple("expenses", "Витрати", Icons.Default.ReceiptLong),
                    Triple("sim", "OBD-II", Icons.Default.Speed),
                    Triple("reminders", "Сервіс", Icons.Default.BuildCircle),
                    Triple("ai", "Паспорт AI", Icons.Default.AutoAwesome),
                    Triple("carplay_hud", "CarPlay", Icons.Default.DirectionsCar)
                )

                tabs.forEach { (tabId, label, icon) ->
                    NavigationBarItem(
                        selected = currentTab == tabId,
                        onClick = { currentTab = tabId },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CosmicBackground,
                            selectedTextColor = CyberCyan,
                            indicatorColor = CyberCyan,
                            unselectedIconColor = DarkGreyText,
                            unselectedTextColor = DarkGreyText
                        ),
                        modifier = Modifier.testTag("nav_item_$tabId")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CosmicBackground)
                .padding(innerPadding)
        ) {
            // Main Top Bar
            Column(modifier = Modifier.fillMaxSize()) {
                // Customized Header for Car Selector
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CosmicSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showCarSelector = true }
                                .padding(vertical = 4.dp)
                                .testTag("car_selector_trigger")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Active car",
                                tint = CyberCyan,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = activeCar?.let { "${it.brand} ${it.model}" } ?: "Додати автомобіль",
                                    color = PureWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1
                                )
                                Text(
                                    text = activeCar?.let { "Рік: ${it.year} • П.: ${it.mileage} км" } ?: "Торкніться, щоб створити",
                                    color = DarkGreyText,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select car",
                                tint = CyberCyan
                            )
                        }

                        // Logo branding
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = CyberCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "PRO",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "AutoControl",
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Flashing telemetry overlay warning if driving is global
                    if (telemetry.isRunning) {
                        Surface(
                            color = ElectricGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ElectricGold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = ElectricGold,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Триває запис поїздки: швидкість ${telemetry.speed.toInt()} км/год • RPM: ${telemetry.rpm.toInt()}",
                                    color = ElectricGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Screen view swapper
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    when (currentTab) {
                        "dashboard" -> DashboardScreen(
                            activeCar = activeCar,
                            expenses = expenses,
                            trips = trips,
                            onNavigate = { currentTab = it }
                        )
                        "expenses" -> ExpensesScreen(
                            expenses = expenses,
                            carMileage = activeCar?.mileage ?: 0,
                            onAddExpense = { cat, amt, odo, desc ->
                                viewModel.addExpense(cat, amt, odo, desc)
                            },
                            onDeleteExpense = { viewModel.deleteExpense(it) }
                        )
                        "sim" -> OBDSimulatorScreen(
                            telemetry = telemetry,
                            activeTripAnalysis = viewModel.activeTripAnalysis.collectAsStateWithLifecycle().value,
                            isAiLoading = viewModel.isAiLoading.collectAsStateWithLifecycle().value,
                            onStartSim = { viewModel.startTripSimulation() },
                            onStopSim = { viewModel.stopTripSimulation() }
                        )
                        "reminders" -> RemindersScreen(
                            reminders = reminders,
                            carMileage = activeCar?.mileage ?: 0,
                            onAddReminder = { title, desc, date, mile, cat ->
                                viewModel.addReminder(title, desc, date, mile, cat)
                            },
                            onCompleteReminder = { viewModel.completeReminder(it) },
                            onDeleteReminder = { viewModel.deleteReminder(it) }
                        )
                        "ai" -> AiConsultantScreen(
                            activeCar = activeCar,
                            expenses = expenses,
                            trips = trips,
                            generalInsight = viewModel.generalAiInsight.collectAsStateWithLifecycle().value,
                            isAiLoading = viewModel.isAiLoading.collectAsStateWithLifecycle().value,
                            onGenerateInsight = { viewModel.generateOverallAiInsight() }
                        )
                        "carplay_hud" -> CarPlaySimulationScreen(
                            telemetry = telemetry,
                            reminders = reminders,
                            activeCar = activeCar,
                            onStartStop = {
                                if (telemetry.isRunning) viewModel.stopTripSimulation() else viewModel.startTripSimulation()
                            }
                        )
                    }
                }
            }

            // --- Car Selector Modal Drawer Sheet ---
            if (showCarSelector) {
                AlertDialog(
                    onDismissRequest = { showCarSelector = false },
                    containerColor = CosmicSurface,
                    title = { Text("Оберіть автомобіль", color = PureWhite, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 260.dp)
                            ) {
                                items(allCars) { car ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (car.isActive) CosmicSurfaceSelected else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.selectActiveCar(car.id)
                                                showCarSelector = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                tint = if (car.isActive) CyberCyan else DarkGreyText
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${car.brand} ${car.model}",
                                                    color = if (car.isActive) CyberCyan else PureWhite,
                                                    fontWeight = if (car.isActive) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "Пробіг: ${car.mileage} км • ${car.fuelType}",
                                                    color = DarkGreyText,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        if (car.isActive) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Active",
                                                tint = CyberCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            IconButton(
                                                onClick = { viewModel.deleteCar(car) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Видалити",
                                                    tint = CyberPink.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    showCarSelector = false
                                    showAddCarDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_car_trigger")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = CosmicBackground)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Додати нове авто", color = CosmicBackground, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showCarSelector = false }) {
                            Text("Закрити", color = CyberCyan)
                        }
                    }
                )
            }

            // --- Add Car Modal Dialog ---
            if (showAddCarDialog) {
                var brand by remember { mutableStateOf("") }
                var model by remember { mutableStateOf("") }
                var year by remember { mutableStateOf("") }
                var mileage by remember { mutableStateOf("") }
                var selectedFuel by remember { mutableStateOf("Бензин") }
                val fuels = listOf("Бензин", "Дизель", "Електро", "Гібрид", "Газ")

                AlertDialog(
                    onDismissRequest = { showAddCarDialog = false },
                    containerColor = CosmicSurface,
                    title = { Text("Додати автомобіль", color = PureWhite, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .testTag("add_car_form")
                        ) {
                            OutlinedTextField(
                                value = brand,
                                onValueChange = { brand = it },
                                label = { Text("Марка (напр. VW, Tesla)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedLabelColor = DarkGreyText,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("car_brand_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it },
                                label = { Text("Модель (напр. Passat, Model S)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedLabelColor = DarkGreyText,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("car_model_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = year,
                                onValueChange = { year = it },
                                label = { Text("Рік випуску") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedLabelColor = DarkGreyText,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("car_year_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = mileage,
                                onValueChange = { mileage = it },
                                label = { Text("Поточний пробіг (км)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedLabelColor = CyberCyan,
                                    unfocusedLabelColor = DarkGreyText,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("car_mileage_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Тип палива", color = PureWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            fuels.chunked(3).forEach { rowFuels ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowFuels.forEach { fuel ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { selectedFuel = fuel }
                                        ) {
                                            RadioButton(
                                                selected = selectedFuel == fuel,
                                                onClick = { selectedFuel = fuel },
                                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                                            )
                                            Text(fuel, color = PureWhite, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCarDialog = false }) {
                            Text("Скасувати", color = CyberPink)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (brand.isNotBlank() && model.isNotBlank()) {
                                    viewModel.insertCar(
                                        brand = brand,
                                        model = model,
                                        year = year.toIntOrNull() ?: 2020,
                                        mileage = mileage.toIntOrNull() ?: 0,
                                        fuelType = selectedFuel
                                    )
                                    showAddCarDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            modifier = Modifier.testTag("car_save_button")
                        ) {
                            Text("Зберегти", color = CosmicBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

// --- TAB 1: DASHBOARD ---
@Composable
fun DashboardScreen(
    activeCar: Car?,
    expenses: List<Expense>,
    trips: List<Trip>,
    onNavigate: (String) -> Unit
) {
    if (activeCar == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = DarkGreyText,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Вітаємо в AutoControl!",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Для початку роботи додайте свій перший автомобіль за допомогою меню зверху.",
                color = DarkGreyText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    // Calculations
    val totalSpent = expenses.sumOf { it.amount }
    val averageScore = if (trips.isNotEmpty()) trips.map { it.drivingStyleScore }.average().toInt() else 100
    
    // Calculate cost of 1km (Total spent on gas & service / mileage tracked in expenses or hardcoded model)
    val totalExpenseMileageRange = if (expenses.size > 1) {
        val sortedExps = expenses.sortedBy { it.odometer }
        val diff = sortedExps.last().odometer - sortedExps.first().odometer
        if (diff > 0) diff else 1000
    } else 1000
    val costPerKm = if (totalSpent > 0) totalSpent / totalExpenseMileageRange else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Core Statistics Dashboard cards in high fidelity grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cost per 1 km
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Вартість 1 км", color = DarkGreyText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (costPerKm > 0) String.format("%.2f грн", costPerKm) else "---",
                            color = CyberCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Eco score
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = if (averageScore >= 85) Icons.Default.Eco else Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (averageScore >= 85) CalmGreen else if (averageScore >= 60) ModerateOrange else AggressiveRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Еко-володіння", color = DarkGreyText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "$averageScore/100",
                            color = if (averageScore >= 85) CalmGreen else if (averageScore >= 60) ModerateOrange else AggressiveRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Загальні витрати", color = DarkGreyText, fontSize = 12.sp)
                        Text(
                            text = String.format("%,.2f грн", totalSpent),
                            color = PureWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Button(
                        onClick = { onNavigate("expenses") },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("+ Додати", color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- Custom Canvas Spending Chart ---
        item {
            Text(
                "Розподіл витрат",
                color = PureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Extract sums
                    val fuelSum = expenses.filter { it.category == "Паливо" }.sumOf { it.amount }.toFloat()
                    val serviceSum = expenses.filter { it.category == "Обслуговування" }.sumOf { it.amount }.toFloat()
                    val insuranceSum = expenses.filter { it.category == "Страховка" }.sumOf { it.amount }.toFloat()
                    val repairsSum = expenses.filter { it.category == "Ремонт" }.sumOf { it.amount }.toFloat()
                    val otherSum = expenses.filter { it.category == "Інше" }.sumOf { it.amount }.toFloat()

                    val chartTotal = fuelSum + serviceSum + insuranceSum + repairsSum + otherSum

                    if (chartTotal > 0f) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                var lastAngle = -90f

                                val fuelAngle = (fuelSum / chartTotal) * 360f
                                drawArc(
                                    color = CyberCyan,
                                    startAngle = lastAngle,
                                    sweepAngle = fuelAngle,
                                    useCenter = false,
                                    style = Stroke(width = 30f, cap = StrokeCap.Round),
                                    size = Size(size.width, size.height)
                                )
                                lastAngle += fuelAngle

                                val serviceAngle = (serviceSum / chartTotal) * 360f
                                drawArc(
                                    color = ElectricGold,
                                    startAngle = lastAngle,
                                    sweepAngle = serviceAngle,
                                    useCenter = false,
                                    style = Stroke(width = 30f, cap = StrokeCap.Round),
                                    size = Size(size.width, size.height)
                                )
                                lastAngle += serviceAngle

                                val insuranceAngle = (insuranceSum / chartTotal) * 360f
                                drawArc(
                                    color = CalmGreen,
                                    startAngle = lastAngle,
                                    sweepAngle = insuranceAngle,
                                    useCenter = false,
                                    style = Stroke(width = 30f, cap = StrokeCap.Round),
                                    size = Size(size.width, size.height)
                                )
                                lastAngle += insuranceAngle

                                val repairAngle = (repairsSum / chartTotal) * 360f
                                drawArc(
                                    color = CyberPink,
                                    startAngle = lastAngle,
                                    sweepAngle = repairAngle,
                                    useCenter = false,
                                    style = Stroke(width = 30f, cap = StrokeCap.Round),
                                    size = Size(size.width, size.height)
                                )
                                lastAngle += repairAngle

                                val otherAngle = (otherSum / chartTotal) * 360f
                                drawArc(
                                    color = DarkGreyText,
                                    startAngle = lastAngle,
                                    sweepAngle = otherAngle,
                                    useCenter = false,
                                    style = Stroke(width = 30f, cap = StrokeCap.Round),
                                    size = Size(size.width, size.height)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Витрачено", color = DarkGreyText, fontSize = 10.sp)
                                Text(
                                    text = String.format("%.0f%%", (fuelSum / chartTotal) * 100),
                                    color = CyberCyan,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Паливно", color = DarkGreyText, fontSize = 9.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Chart Legend
                        val categories = listOf(
                            CategoryLegend("Паливо", fuelSum, CyberCyan, chartTotal),
                            CategoryLegend("Сервіс", serviceSum, ElectricGold, chartTotal),
                            CategoryLegend("Страховка", insuranceSum, CalmGreen, chartTotal),
                            CategoryLegend("Ремонт", repairsSum, CyberPink, chartTotal),
                            CategoryLegend("Інше", otherSum, DarkGreyText, chartTotal)
                        )

                        categories.chunked(2).forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowList.forEach { cat ->
                                    if (cat.amount > 0f) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(cat.color, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${cat.name}: ${cat.percentage}%",
                                                color = PureWhite,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = CosmicBorder, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Немає даних про витрати", color = DarkGreyText, fontSize = 13.sp)
                            Text("Додайте пальне чи ремонт для відображення", color = DarkGreyText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- Quick Links OBD Simulation banner ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("sim") }
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Route, contentDescription = null, tint = CyberCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("OBD-II Тренажер розвідок", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Здійсніть віртуальну поїздку та оцініть Еко-стиль", color = DarkGreyText, fontSize = 11.sp)
                        }
                    }
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = CyberCyan)
                }
            }
        }

        // Recent trips / events
        item {
            Text(
                "Останні поїздки",
                color = PureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (trips.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("У вас немає збережених поїздок.", color = DarkGreyText, fontSize = 12.sp)
                    }
                }
            } else {
                trips.take(3).forEach { trip ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            (if (trip.drivingStyleScore >= 85) CalmGreen else if (trip.drivingStyleScore >= 60) ModerateOrange else AggressiveRed).copy(
                                                alpha = 0.15f
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsRun,
                                        contentDescription = null,
                                        tint = if (trip.drivingStyleScore >= 85) CalmGreen else if (trip.drivingStyleScore >= 60) ModerateOrange else AggressiveRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${trip.distanceKm} км • ${trip.drivingStyleCategory}",
                                        color = PureWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = DateFormat.format("dd.MM.yyyy HH:mm", trip.date).toString(),
                                        color = DarkGreyText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Text(
                                text = "${trip.drivingStyleScore} / 100",
                                color = if (trip.drivingStyleScore >= 85) CalmGreen else if (trip.drivingStyleScore >= 60) ModerateOrange else AggressiveRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

data class CategoryLegend(val name: String, val amount: Float, val color: Color, val total: Float) {
    val percentage: Int = if (total > 0f) ((amount / total) * 100).toInt() else 0
}


// --- TAB 2: EXPENSES TIMELINE ---
@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    carMileage: Int,
    onAddExpense: (String, Double, Int, String) -> Unit,
    onDeleteExpense: (Expense) -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var sumInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var odometerInput by remember { mutableStateOf(carMileage.toString()) }
    var selectedCategory by remember { mutableStateOf("Паливо") }

    val categories = listOf("Паливо", "Обслуговування", "Страховка", "Ремонт", "Інше")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Історія сервісної книжки (${expenses.size})",
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            if (expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CosmicBorder, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("У книжці немає записів", color = DarkGreyText, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    items(expenses) { expense ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when(expense.category) {
                                                "Паливо" -> Icons.Default.LocalGasStation
                                                "Обслуговування" -> Icons.Default.Build
                                                "Страховка" -> Icons.Default.VerifiedUser
                                                "Ремонт" -> Icons.Default.Handyman
                                                else -> Icons.Default.Receipt
                                            },
                                            contentDescription = null,
                                            tint = when(expense.category) {
                                                "Паливо" -> CyberCyan
                                                "Обслуговування" -> ElectricGold
                                                "Страховка" -> CalmGreen
                                                "Ремонт" -> CyberPink
                                                else -> DarkGreyText
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = expense.category,
                                            color = PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${expense.amount} грн",
                                            color = CyberCyan,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onDeleteExpense(expense) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = CyberPink.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                if (expense.description.isNotEmpty()) {
                                    Text(expense.description, color = PureWhite, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Одометр: ${expense.odometer} км", color = DarkGreyText, fontSize = 11.sp)
                                    Text(
                                        text = DateFormat.format("dd.MM.yyyy HH:mm", expense.date).toString(),
                                        color = DarkGreyText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Floating Action Button on screen
        FloatingActionButton(
            onClick = { showAddForm = true },
            containerColor = CyberCyan,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_expense_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = CosmicBackground)
        }

        // --- Add Expense Overlaid Modal ---
        if (showAddForm) {
            AlertDialog(
                onDismissRequest = { showAddForm = false },
                containerColor = CosmicSurface,
                title = { Text("Нова сервісна подія", color = PureWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .testTag("expense_adding_form")
                    ) {
                        // Category switcher
                        Text("Оберіть категорію", color = PureWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            categories.forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .background(
                                            if (selectedCategory == cat) CyberCyan else CosmicBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (selectedCategory == cat) CosmicBackground else PureWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = sumInput,
                            onValueChange = { sumInput = it },
                            label = { Text("Сума (грн)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CosmicBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_amount_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = odometerInput,
                            onValueChange = { odometerInput = it },
                            label = { Text("Показники пробігу (км)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CosmicBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_odo_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text("Короткий коментар (напр. АЗС WOG)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CosmicBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_desc_input")
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddForm = false }) {
                        Text("Скасувати", color = CyberPink)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedAmount = sumInput.toDoubleOrNull() ?: 0.0
                            val parsedOdo = odometerInput.toIntOrNull() ?: carMileage
                            if (parsedAmount > 0.0) {
                                onAddExpense(selectedCategory, parsedAmount, parsedOdo, descInput)
                                showAddForm = false
                                sumInput = ""
                                descInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.testTag("expense_save_button")
                    ) {
                        Text("Зберегти", color = CosmicBackground, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}


// --- TAB 3: OBD-II TRIP REVEALER ---
@Composable
fun OBDSimulatorScreen(
    telemetry: LiveTripTelemetry,
    activeTripAnalysis: String?,
    isAiLoading: Boolean,
    onStartSim: () -> Unit,
    onStopSim: () -> Unit
) {
    var showResults by remember { mutableStateOf(false) }

    // Synchronize show results modal when the trip simulator turns from on to off
    LaunchedEffect(telemetry.isRunning) {
        if (!telemetry.isRunning && telemetry.elapsedSeconds > 0) {
            showResults = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // OBD Bluetooth connectivity banner status
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(if (telemetry.isRunning) CalmGreen else ElectricGold, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (telemetry.isRunning) "OBD-II: ТРАНСЛЯЦІЯ АКТИВНА" else "OBD-II: ПІДКЛЮЧЕНО (Очікування старту)",
                    color = if (telemetry.isRunning) CalmGreen else ElectricGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Huge dashboard style gauge meter
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp)
                .drawBehind {
                    // Draw digital gauge outline
                    drawArc(
                        color = CosmicBorder,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )

                    // Active sweep angle
                    val speedScalar = if (telemetry.speed > 0f) telemetry.speed / 140f else 0f
                    drawArc(
                        color = CyberCyan,
                        startAngle = 135f,
                        sweepAngle = speedScalar * 270f,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round),
                        size = Size(size.width, size.height)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Real-time speed indicator
                Text(
                    text = String.format("%.0f", telemetry.speed),
                    color = PureWhite,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "км / год",
                    color = DarkGreyText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                // RPM gauge representation
                Text(
                    text = "RPM: ${telemetry.rpm.toInt()}",
                    color = ElectricGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Live Warning Alerts overlaid HUD
        AnimatedVisibility(
            visible = telemetry.activeEventAlert != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = CyberPink.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CyberPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = telemetry.activeEventAlert ?: "",
                    color = CyberPink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )
            }
        }

        // Stats grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Час", color = DarkGreyText, fontSize = 11.sp)
                Text(
                    text = String.format("%02d:%02d", telemetry.elapsedSeconds / 60, telemetry.elapsedSeconds % 60),
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Відстань", color = DarkGreyText, fontSize = 11.sp)
                Text(
                    text = String.format("%.2f км", telemetry.distanceKm),
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Витрата", color = DarkGreyText, fontSize = 11.sp)
                Text(
                    text = String.format("%.1f л/100", telemetry.instantConsumption),
                    color = CyberCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start / Stop Command buttons
        if (!telemetry.isRunning) {
            Button(
                onClick = { onStartSim() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_trip_btn")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CosmicBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Почати поїздку OBD-II", color = CosmicBackground, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Button(
                onClick = { onStopSim() },
                colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("stop_trip_btn")
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, tint = PureWhite)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Завершити поїздку", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Triggerable prompt list if results shown
        if (showResults) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Поїздка збережена!",
                        color = ElectricGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isAiLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = CyberCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("ШІ аналізує телеметрію вашої поїздки...", color = DarkGreyText, fontSize = 13.sp)
                        }
                    } else if (activeTripAnalysis != null) {
                        Text(
                            text = activeTripAnalysis,
                            color = PureWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showResults = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Зрозуміло")
                    }
                }
            }
        }
    }
}


// --- TAB 4: ADAPTIVE REMINDERS / MAINTENANCE CHECKLEDS ---
@Composable
fun RemindersScreen(
    reminders: List<Reminder>,
    carMileage: Int,
    onAddReminder: (String, String, Long?, Int?, String) -> Unit,
    onCompleteReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetMileage by remember { mutableStateOf("") }
    var isMileageBased by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf("Oil") }

    val categories = listOf("Oil", "Insurance", "Filter", "Maintenance", "Other")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Сервісні нагадування",
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Немає запланованих нагадувань", color = DarkGreyText, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(reminders) { reminder ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (reminder.isCompleted) CosmicSurface.copy(alpha = 0.5f) else CosmicSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (reminder.isAIAdaptive) BorderStroke(1.dp, ElectricGold.copy(alpha = 0.6f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = reminder.isCompleted,
                                        onCheckedChange = { if (!reminder.isCompleted) onCompleteReminder(reminder) },
                                        colors = CheckboxDefaults.colors(checkedColor = CyberCyan, uncheckedColor = DarkGreyText),
                                        modifier = Modifier.testTag("reminder_checkbox_${reminder.id}")
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = reminder.title,
                                                color = if (reminder.isCompleted) DarkGreyText else PureWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (reminder.isAIAdaptive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = ElectricGold.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "⚡ AI Скориговано",
                                                        color = ElectricGold,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(reminder.description, color = DarkGreyText, fontSize = 12.sp)

                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (reminder.dueMileage != null) {
                                            val diff = reminder.dueMileage - carMileage
                                            Text(
                                                text = "Термін: ${reminder.dueMileage} км " +
                                                        if (diff > 0) "(Залишилось $diff км)" else "(ПРОСТРОЧЕНО на ${-diff} км)",
                                                color = if (diff > 0) CyberCyan else CyberPink,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else if (reminder.dueDate != null) {
                                            Text(
                                                text = "Термін: ${DateFormat.format("dd.MM.yyyy", reminder.dueDate)}",
                                                color = CalmGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onDeleteReminder(reminder) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Видалити",
                                        tint = CyberPink.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showForm = true },
            containerColor = CyberCyan,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_reminder_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Reminder", tint = CosmicBackground)
        }

        // --- Create Reminder dialog ---
        if (showForm) {
            AlertDialog(
                onDismissRequest = { showForm = false },
                containerColor = CosmicSurface,
                title = { Text("Нове нагадування", color = PureWhite, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .testTag("reminder_form")
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Заголовок (напр. Заміна страховки)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CosmicBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reminder_title_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Опис деталі") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CosmicBorder,
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reminder_desc_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isMileageBased,
                                onClick = { isMileageBased = true },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                            )
                            Text("По пробігу (км)", color = PureWhite, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(
                                selected = !isMileageBased,
                                onClick = { isMileageBased = false },
                                colors = RadioButtonDefaults.colors(selectedColor = CyberCyan)
                            )
                            Text("По даті", color = PureWhite, fontSize = 13.sp)
                        }

                        if (isMileageBased) {
                            OutlinedTextField(
                                value = targetMileage,
                                onValueChange = { targetMileage = it },
                                label = { Text("Цільовий пробіг (км)") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyberCyan,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reminder_target_odo_input")
                            )
                        } else {
                            Text(
                                "Подію буде встановлено на наступні 3 місяці автоматично",
                                color = ElectricGold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForm = false }) {
                        Text("Скасувати", color = CyberPink)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onAddReminder(
                                    title,
                                    description,
                                    if (isMileageBased) null else System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000), // 90 days delay
                                    if (isMileageBased) targetMileage.toIntOrNull() else null,
                                    selectedCategory
                                )
                                showForm = false
                                title = ""
                                description = ""
                                targetMileage = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.testTag("reminder_save_button")
                    ) {
                        Text("Зберегти", color = CosmicBackground, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}


// --- TAB 5: AI CONSULTANT VOICE & DIGITAL CAR CERTIFICATE ---
@Composable
fun AiConsultantScreen(
    activeCar: Car?,
    expenses: List<Expense>,
    trips: List<Trip>,
    generalInsight: String?,
    isAiLoading: Boolean,
    onGenerateInsight: () -> Unit
) {
    if (activeCar == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Інтелектуальний асистент та Звіти",
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Banner block AI description
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AutoControl Core AI", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Персоналізована нейромережа аналізує ваші витрати, стиль водіння та показники OBD тестування. Ми формуємо для вас ідеальну карту обслуговування.",
                    color = DarkGreyText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Action Trigger Button
        Button(
            onClick = { onGenerateInsight() },
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_ai_advisor_btn")
        ) {
            if (isAiLoading) {
                CircularProgressIndicator(color = CosmicBackground, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ШІ формує висновки...", color = CosmicBackground, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Analytics, contentDescription = null, tint = CosmicBackground)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Запустити AI розрахунок економії", color = CosmicBackground, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Output Block
        if (generalInsight != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡 Аналітичний звіт", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = CalmGreen, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = generalInsight,
                        color = PureWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // --- EXPORTABLE DIGITAL VEHICLE CERTIFICATE SECTION ("Цифровий паспорт авто") ---
        Text(
            "Цифровий паспорт автомобіля",
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            border = BorderStroke(1.dp, CosmicBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                Box(
                    modifier = Modifier
                        .background(CalmGreen.copy(alpha = 0.15f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = CalmGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ПАСПОРТ ВЕРИФІКОВАНО", color = CalmGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand details
                Text(
                    text = "${activeCar.brand} ${activeCar.model}",
                    color = PureWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Рік випуску: ${activeCar.year} • Паливо: ${activeCar.fuelType}",
                    color = DarkGreyText,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Пробіг підтверджено", color = DarkGreyText, fontSize = 10.sp)
                        Text("${activeCar.mileage} км", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Сервісна історія", color = DarkGreyText, fontSize = 10.sp)
                        Text("${expenses.size} подій", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Поїздки OBD", color = DarkGreyText, fontSize = 10.sp)
                        Text("${trips.size} записів", color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = CosmicBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Цей документ містить підтверджений цифровий слід пробігу автомобіля на основі регулярних телеметрій OBD-II. Використовуйте його при продажу для збільшення довіри покупців.",
                    color = DarkGreyText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Unique code & validation seals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Код контролю:", color = DarkGreyText, fontSize = 9.sp)
                        Text(
                            "ID: AC-${UUID.nameUUIDFromBytes(activeCar.brand.toByteArray()).toString().substring(0, 8).uppercase()}",
                            color = ElectricGold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { /* Simulated sharing / printing certificate */ },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Поділитись", color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


// --- TAB 6: CARPLAY / ANDROID AUTO WIDESCREEN MOCK HUD ---
@Composable
fun CarPlaySimulationScreen(
    telemetry: LiveTripTelemetry,
    reminders: List<Reminder>,
    activeCar: Car?,
    onStartStop: () -> Unit
) {
    if (activeCar == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "CarPlay / Android Auto Preview",
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            "Симуляція широкоекранного автомобільного HUD інтерфейсу під час руху",
            color = DarkGreyText,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )

        // Landscape layout mock card simulating CarPlay aspect ratio in dark environment
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF030406)), // extra black for dashboard screen contrast
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, if (telemetry.isRunning) CyberCyan else CosmicBorder),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f) // widescreen proportion
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // CarPlay Side bar (Quick control shortcuts of standard automotive UI)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(48.dp)
                        .background(Color(0xFF0F1014))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "CarPlay Menu", tint = PureWhite, modifier = Modifier.size(20.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Active Speed", tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Icon(Icons.Default.LocationOn, contentDescription = "Maps shortcut", tint = DarkGreyText, modifier = Modifier.size(18.dp))
                        Icon(Icons.Default.MusicNote, contentDescription = "Player Shortcut", tint = DarkGreyText, modifier = Modifier.size(18.dp))
                    }

                    Text("13:40", color = PureWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // CarPlay Main space content split (Half-Split Layout: Left map/gauge, Right widgets)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .background(Color(0xFF08090C))
                        .padding(8.dp)
                ) {
                    // Left Column: Active Speed dial representation
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1.1f)
                            .background(Color(0xFF111319), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format("%.0f", telemetry.speed),
                            color = PureWhite,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 44.sp
                        )
                        Text("км/год", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("RPM: ${telemetry.rpm.toInt()}", color = ElectricGold, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right column: Safety alerts + Active recommendation cards or navigation pointers
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1.9f)
                    ) {
                        if (telemetry.activeEventAlert != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CyberPink.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, CyberPink),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = telemetry.activeEventAlert ?: "",
                                        color = CyberPink,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("AutoControl ШІ Порада:", color = CyberCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (telemetry.isRunning) "Ви здійснюєте поїздку в оптимальному еко-темпі. Уникайте різких розгонів у місті."
                                        else "Підключіть автомобіль до OBD для моніторингу стилю водіння.",
                                        color = PureWhite,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111319)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val urgentReminder = reminders.firstOrNull { !it.isCompleted }
                                    if (urgentReminder != null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = ElectricGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Увага: ${urgentReminder.title}",
                                                color = ElectricGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(urgentReminder.description, color = DarkGreyText, fontSize = 9.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                                    } else {
                                        Text("✅ Всі планові заміни виконано", color = CalmGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("Ваше авто під повним контролем.", color = DarkGreyText, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Trigger simulation state on screen CarPlay preview
        Button(
            onClick = { onStartStop() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (telemetry.isRunning) CyberPink else CyberCyan
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val text = if (telemetry.isRunning) "Завершити подорож CarPlay" else "Розпочати поїздку CarPlay"
            Text(text, color = if (telemetry.isRunning) PureWhite else CosmicBackground, fontWeight = FontWeight.Bold)
        }
    }
}
