package com.vps.financetracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vps.financetracker.data.Transaction
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authViewModel: AuthViewModel by viewModels()
        val financeViewModel: FinanceViewModel by viewModels { FinanceViewModelFactory() }
        setContent { MaterialTheme { AppNavigation(authViewModel, financeViewModel) } }
    }
}

// --- NAVIGASI UTAMA ---
@Composable
fun AppNavigation(authViewModel: AuthViewModel, financeViewModel: FinanceViewModel) {
    val navController = rememberNavController()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("home") { popUpTo("login") { inclusive = true } }
        } else {
            navController.navigate("login") { popUpTo("home") { inclusive = true } }
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController, authViewModel) }
        composable("register") { RegisterScreen(navController, authViewModel) }
        composable("home") { HomeScreen(navController, authViewModel, financeViewModel) }
        composable("analytics") { AnalyticsScreen(navController, financeViewModel) }
    }
}

// --- SCREEN: LOGIN ---
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMsg by viewModel.errorMessage.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Selamat Datang", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Masuk untuk mengelola keuangan", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (errorMsg != null) { Spacer(modifier = Modifier.height(8.dp)); Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error) }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp)) { if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Masuk") }
            Spacer(modifier = Modifier.height(16.dp))
            Row { Text("Belum punya akun? "); ClickableText(text = AnnotatedString("Daftar Sekarang"), onClick = { navController.navigate("register") }, style = TextStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) }
        }
    }
}

// --- SCREEN: REGISTER ---
@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Buat Akun Baru", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.register(email, password) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp)) { if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Text("Daftar") }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { navController.popBackStack() }) { Text("Kembali ke Login") }
        }
    }
}

// --- SCREEN: HOME ---
@Composable
fun HomeScreen(navController: NavController, authViewModel: AuthViewModel, financeViewModel: FinanceViewModel) {
    var selectedTab by remember { mutableStateOf(0) } 
    
    LaunchedEffect(selectedTab) { if (selectedTab == 0) financeViewModel.switchToPersonal() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Pribadi") }, selected = selectedTab == 0, onClick = { selectedTab = 0 })
                NavigationBarItem(icon = { Icon(Icons.Outlined.Group, null) }, label = { Text("Team") }, selected = selectedTab == 1, onClick = { selectedTab = 1 })
                NavigationBarItem(icon = { Icon(Icons.Outlined.AccountCircle, null) }, label = { Text("Akun") }, selected = selectedTab == 2, onClick = { selectedTab = 2 })
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> PersonalFinanceScreen(navController, financeViewModel)
                1 -> TeamManagementScreen(navController, financeViewModel) 
                2 -> AccountScreen(authViewModel)
            }
        }
    }
}

// --- TAB 1: KEUANGAN PRIBADI ---
@Composable
fun PersonalFinanceScreen(navController: NavController, viewModel: FinanceViewModel) {
    SharedFinanceContent(navController = navController, viewModel = viewModel, title = "Keuangan Pribadi", isPersonal = true, onBack = null)
}

// --- TAB 2: MANAJEMEN TIM ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamManagementScreen(navController: NavController, viewModel: FinanceViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showInboxDialog by remember { mutableStateOf(false) }
    var selectedTeamIdForInvite by remember { mutableStateOf("") }
    var selectedTeamNameForInvite by remember { mutableStateOf("") }
    var newTeamName by remember { mutableStateOf("") }
    val myTeams by viewModel.myTeams.collectAsState()
    val myInvitations by viewModel.myInvitations.collectAsState()
    var selectedTeamName by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedTeamName != null) { selectedTeamName = null; viewModel.switchToPersonal() }

    if (selectedTeamName != null) {
        SharedFinanceContent(navController = navController, viewModel = viewModel, title = selectedTeamName!!, isPersonal = false, onBack = { selectedTeamName = null; viewModel.switchToPersonal() })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Kelola Tim") },
                    actions = {
                        IconButton(onClick = { showInboxDialog = true }) {
                            BadgedBox(badge = { if (myInvitations.isNotEmpty()) Badge { Text(myInvitations.size.toString()) } }) { Icon(Icons.Default.Mail, contentDescription = "Inbox") }
                        }
                    }
                )
            },
            floatingActionButton = { FloatingActionButton(onClick = { showCreateDialog = true }) { Icon(Icons.Default.Add, "Buat") } }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                if (myTeams.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada tim.", color = Color.Gray) } } 
                else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(myTeams) { team ->
                            var menuExpanded by remember { mutableStateOf(false) }
                            Card(modifier = Modifier.fillMaxWidth().clickable { viewModel.switchWorkspace(team.id); selectedTeamName = team.name }) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) { Text(team.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) { Text(team.name, fontWeight = FontWeight.Bold); Text("ID: ${team.id.take(6)}...", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                    Box { IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "More") }; DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) { DropdownMenuItem(text = { Text("Undang") }, onClick = { menuExpanded = false; selectedTeamIdForInvite = team.id; selectedTeamNameForInvite = team.name; showInviteDialog = true }, leadingIcon = { Icon(Icons.Default.PersonAdd, null) }) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showCreateDialog) { AlertDialog(onDismissRequest = { showCreateDialog = false }, title = { Text("Buat Tim") }, text = { OutlinedTextField(value = newTeamName, onValueChange = { newTeamName = it }, label = { Text("Nama Tim") }) }, confirmButton = { Button(onClick = { if (newTeamName.isNotEmpty()) { viewModel.createTeam(newTeamName); showCreateDialog = false; newTeamName = "" } }) { Text("Buat") } }, dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Batal") } }) }
    if (showInviteDialog) { var inviteEmail by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showInviteDialog = false }, title = { Text("Undang ke $selectedTeamNameForInvite") }, text = { OutlinedTextField(value = inviteEmail, onValueChange = { inviteEmail = it }, label = { Text("Email Teman") }) }, confirmButton = { Button(onClick = { if (inviteEmail.isNotEmpty()) { viewModel.sendInvite(inviteEmail, selectedTeamIdForInvite, selectedTeamNameForInvite); showInviteDialog = false } }) { Text("Kirim") } }, dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("Batal") } }) }
    if (showInboxDialog) { AlertDialog(onDismissRequest = { showInboxDialog = false }, title = { Text("Inbox Undangan") }, text = { if (myInvitations.isEmpty()) Text("Tidak ada pesan.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(myInvitations) { invite -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Column(modifier = Modifier.padding(12.dp)) { Text("Tim: ${invite.teamName}", fontWeight = FontWeight.Bold); Text("Dari: ${invite.fromEmail}", style = MaterialTheme.typography.bodySmall); Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = { viewModel.rejectInvite(invite) }) { Text("Tolak", color = Color.Red) }; Spacer(modifier = Modifier.width(8.dp)); Button(onClick = { viewModel.acceptInvite(invite) }) { Text("Terima") } } } } } } }, confirmButton = { TextButton(onClick = { showInboxDialog = false }) { Text("Tutup") } }) }
}

// --- TAB 3: AKUN ---
@Composable
fun AccountScreen(authViewModel: AuthViewModel) {
    val user by authViewModel.currentUser.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(user?.email ?: "Tamu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { authViewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.ExitToApp, null); Spacer(modifier = Modifier.width(8.dp)); Text("Logout") }
    }
}

// --- SHARED UI: DRAWER & CONTENT ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedFinanceContent(navController: NavController, viewModel: FinanceViewModel, title: String, isPersonal: Boolean, onBack: (() -> Unit)?) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Menu Fitur", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                Divider()
                NavigationDrawerItem(
                    label = { Text("Grafik Keuangan") },
                    selected = false,
                    icon = { Icon(Icons.Outlined.BarChart, null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("analytics")
                    }
                )
            }
        }
    ) {
        val transactionList by viewModel.monthlyTransactions.collectAsState(initial = emptyList())
        val incomeTotal by viewModel.monthlyIncome.collectAsState(initial = 0.0)
        val expenseTotal by viewModel.monthlyExpense.collectAsState(initial = 0.0)
        val currentMonth by viewModel.currentMonth.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val context = LocalContext.current
        var showDialog by remember { mutableStateOf(false) }
        var isIncomeTransaction by remember { mutableStateOf(true) }
        var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }
        val exportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? -> uri?.let { writeCsvToUri(context, it, transactionList, currentMonth) } }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
                        navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, contentDescription = "Menu") } },
                        actions = {
                            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                            IconButton(onClick = { val fileName = "Laporan.csv"; exportLauncher.launch(fileName) }) { Icon(Icons.Default.Save, contentDescription = "Export") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isPersonal) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFE0B2))
                    )
                    MonthSelector(currentMonth, { viewModel.previousMonth() }, { viewModel.nextMonth() })
                }
            },
            bottomBar = { 
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { 
                    Button(onClick = { isIncomeTransaction = true; transactionToEdit = null; showDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Icon(Icons.Default.ArrowUpward, null); Spacer(Modifier.width(8.dp)); Text("Pemasukan") }
                    Button(onClick = { isIncomeTransaction = false; transactionToEdit = null; showDialog = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) { Icon(Icons.Default.ArrowDownward, null); Spacer(Modifier.width(8.dp)); Text("Pengeluaran") } 
                } 
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                OutlinedTextField(value = searchQuery, onValueChange = { viewModel.onSearchQueryChange(it) }, label = { Text("Cari Transaksi...") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Pemasukan", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold); Text(formatRupiah(incomeTotal), style = MaterialTheme.typography.titleLarge) }; Column(horizontalAlignment = Alignment.End) { Text("Pengeluaran", color = Color(0xFFC62828), fontWeight = FontWeight.Bold); Text(formatRupiah(expenseTotal), style = MaterialTheme.typography.titleLarge) } }; Divider(modifier = Modifier.padding(vertical = 12.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Keuntungan:", fontWeight = FontWeight.SemiBold); Text(formatRupiah(incomeTotal - expenseTotal), fontWeight = FontWeight.Bold, color = if((incomeTotal - expenseTotal) >= 0) Color.Black else Color.Red) } } }
                Spacer(modifier = Modifier.height(16.dp))
                if (transactionList.isEmpty()) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (isPersonal) "Belum ada data Pribadi" else "Belum ada data Tim", color = Color.Gray) }
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(transactionList) { t -> TransactionItem(transaction = t, onDelete = { viewModel.deleteTransaction(t) }, onClick = { transactionToEdit = t; isIncomeTransaction = t.isIncome; showDialog = true }) } }
            }
        }
        if (showDialog) InputTransactionDialog(viewModel = viewModel, isIncome = isIncomeTransaction, existingTransaction = transactionToEdit, defaultDate = currentMonth.timeInMillis, onDismiss = { showDialog = false }, onConfirm = { amt, cat, note, time -> if (transactionToEdit == null) viewModel.addTransaction(amt, cat, note, isIncomeTransaction, time) else viewModel.updateTransaction(transactionToEdit!!.firestoreId, amt, cat, note, isIncomeTransaction, time); showDialog = false })
    }
}

// --- SCREEN BARU: ANALYTICS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController, viewModel: FinanceViewModel) {
    val transactionList by viewModel.monthlyTransactions.collectAsState(initial = emptyList())
    val expenses = transactionList.filter { !it.isIncome }
    val totalExpense = expenses.sumOf { it.amount }
    val grouped = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
    val colors = listOf(Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFFFA726), Color(0xFFAB47BC), Color(0xFF8D6E63))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grafik Keuangan") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (expenses.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada pengeluaran bulan ini.", color = Color.Gray) } } 
            else {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(200.dp)) {
                                var startAngle = -90f
                                grouped.forEachIndexed { index, entry ->
                                    val sweepAngle = (entry.second / totalExpense * 360).toFloat()
                                    drawArc(color = colors[index % colors.size], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = 50f))
                                    startAngle += sweepAngle
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Total", style = MaterialTheme.typography.bodySmall); Text(formatRupiah(totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Detail Pengeluaran", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(grouped.size) { index ->
                        val entry = grouped[index]
                        val percentage = (entry.second / totalExpense * 100).toInt()
                        Card(elevation = CardDefaults.cardElevation(2.dp)) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(16.dp).background(colors[index % colors.size], CircleShape))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(entry.first, fontWeight = FontWeight.Bold); Text("$percentage%", style = MaterialTheme.typography.bodySmall, color = Color.Gray) }
                                Text(formatRupiah(entry.second), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER FUNCTIONS ---
@Composable fun MonthSelector(currentMonth: Calendar, onPrevious: () -> Unit, onNext: () -> Unit) { Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "Bulan Lalu") }; Text(text = formatMonthYear(currentMonth.timeInMillis), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "Bulan Depan") } } }
@Composable fun TransactionItem(transaction: Transaction, onDelete: (Transaction) -> Unit, onClick: () -> Unit) { Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), modifier = Modifier.clickable { onClick() }) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(text = transaction.category, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold); if (!transaction.note.isNullOrEmpty()) Text(text = transaction.note, style = MaterialTheme.typography.bodySmall); Text(text = formatDate(transaction.timestamp), style = MaterialTheme.typography.labelSmall, color = Color.Gray) }; Column(horizontalAlignment = Alignment.End) { Text(text = (if (transaction.isIncome) "+ " else "- ") + formatRupiah(transaction.amount), color = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336), fontWeight = FontWeight.Bold); IconButton(onClick = { onDelete(transaction) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Gray) } } } } }
@OptIn(ExperimentalMaterial3Api::class) @Composable fun InputTransactionDialog(viewModel: FinanceViewModel, isIncome: Boolean, existingTransaction: Transaction?, defaultDate: Long, onDismiss: () -> Unit, onConfirm: (Double, String, String, Long) -> Unit) { var amountText by remember { mutableStateOf(existingTransaction?.amount?.toString()?.replace(".0", "") ?: "") }; var noteText by remember { mutableStateOf(existingTransaction?.note ?: "") }; var selectedDate by remember { mutableStateOf(existingTransaction?.timestamp ?: defaultDate) }; var categoryText by remember { mutableStateOf(existingTransaction?.category ?: "") }; var expanded by remember { mutableStateOf(false) }; val categoryList by viewModel.getCategories(isIncome).collectAsState(initial = emptyList()); var showAddCategoryDialog by remember { mutableStateOf(false) }; var showDatePicker by remember { mutableStateOf(false) }; val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate); if (showAddCategoryDialog) { var newCategoryName by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { showAddCategoryDialog = false }, title = { Text("Tambah Kategori Baru") }, text = { OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("Nama Kategori") }, singleLine = true) }, confirmButton = { Button(onClick = { if (newCategoryName.isNotEmpty()) { viewModel.addCategory(newCategoryName, isIncome); categoryText = newCategoryName; showAddCategoryDialog = false } }) { Text("Simpan") } }, dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Batal") } }) }; if (showDatePicker) { DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { selectedDate = it }; showDatePicker = false }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Batal") } }) { DatePicker(state = datePickerState) } }; Dialog(onDismissRequest = onDismiss) { Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(24.dp)) { Text(text = if (existingTransaction != null) "Edit Transaksi" else (if (isIncome) "Catat Pemasukan" else "Catat Pengeluaran"), style = MaterialTheme.typography.headlineSmall); Spacer(modifier = Modifier.height(16.dp)); OutlinedCard(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Column { Text("Tanggal Transaksi", style = MaterialTheme.typography.labelSmall); Text(formatDateOnly(selectedDate), style = MaterialTheme.typography.bodyLarge) } } }; Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = amountText, onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it }, label = { Text("Jumlah (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(8.dp)); ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) { OutlinedTextField(value = categoryText, onValueChange = { categoryText = it; expanded = true }, label = { Text("Kategori") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(), modifier = Modifier.menuAnchor().fillMaxWidth()); ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Tambah Kategori Baru...", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }, onClick = { expanded = false; showAddCategoryDialog = true }); Divider(); val filteredOptions = categoryList.filter { it.name.contains(categoryText, ignoreCase = true) }; filteredOptions.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { categoryText = category.name; expanded = false }) }; if (categoryList.none { it.name.equals(categoryText, ignoreCase = true) } && categoryText.isNotEmpty()) { DropdownMenuItem(text = { Text("Gunakan: \"$categoryText\"") }, onClick = { expanded = false }) } } }; Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Catatan (Opsional)") }, modifier = Modifier.fillMaxWidth()); Spacer(modifier = Modifier.height(24.dp)); Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { TextButton(onClick = onDismiss) { Text("Batal") }; Spacer(modifier = Modifier.width(8.dp)); Button(onClick = { val amount = amountText.toDoubleOrNull(); if (amount != null && categoryText.isNotEmpty()) onConfirm(amount, categoryText, noteText, selectedDate) }) { Text(if (existingTransaction != null) "Update" else "Simpan") } } } } } }
fun writeCsvToUri(context: Context, uri: Uri, transactions: List<Transaction>, currentMonth: Calendar) { try { context.contentResolver.openOutputStream(uri)?.use { outputStream -> val writer = OutputStreamWriter(outputStream); val monthName = formatMonthYear(currentMonth.timeInMillis); writer.append("Laporan Keuangan - $monthName\nID,Tanggal,Tipe,Kategori,Jumlah,Catatan\n"); val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); for (t in transactions) { val tipe = if (t.isIncome) "Pemasukan" else "Pengeluaran"; val date = sdf.format(Date(t.timestamp)); val cleanNote = t.note?.replace(",", " ") ?: ""; writer.append("${t.id},$date,$tipe,${t.category},${t.amount.toLong()},$cleanNote\n") }; writer.flush(); Toast.makeText(context, "Berhasil disimpan!", Toast.LENGTH_LONG).show() } } catch (e: Exception) { Toast.makeText(context, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show() } }
fun formatRupiah(amount: Double): String { return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount) }
fun formatDate(timestamp: Long): String { return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(timestamp)) }
fun formatDateOnly(timestamp: Long): String { return SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date(timestamp)) }
fun formatMonthYear(timestamp: Long): String { return SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date(timestamp)) }
fun formatMonthFile(calendar: Calendar): String { return SimpleDateFormat("MMM_yyyy", Locale("id", "ID")).format(calendar.time) }
