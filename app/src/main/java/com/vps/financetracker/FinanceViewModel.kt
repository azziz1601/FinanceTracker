package com.vps.financetracker

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.vps.financetracker.data.Category
import com.vps.financetracker.data.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

// Data Class
data class Team(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val members: List<String> = emptyList()
)

data class Invitation(
    val id: String = "",
    val fromEmail: String = "",
    val toEmail: String = "", // Target undangan
    val teamId: String = "",
    val teamName: String = "",
    val status: String = "pending" // pending, accepted, rejected
)

class FinanceViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // --- STATE UTAMA ---
    val currentUserId: String get() = auth.currentUser?.uid ?: ""
    val currentUserEmail: String get() = auth.currentUser?.email ?: ""

    private val _currentWorkspaceId = MutableStateFlow("")
    val currentWorkspaceId: StateFlow<String> = _currentWorkspaceId.asStateFlow()

    val isPersonalMode = _currentWorkspaceId.map { it == currentUserId }

    private val _myTeams = MutableStateFlow<List<Team>>(emptyList())
    val myTeams: StateFlow<List<Team>> = _myTeams.asStateFlow()

    // Inbox Undangan
    private val _myInvitations = MutableStateFlow<List<Invitation>>(emptyList())
    val myInvitations: StateFlow<List<Invitation>> = _myInvitations.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var transactionListener: ListenerRegistration? = null

    // Filter & Search
    private val _currentMonth = MutableStateFlow(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _currentWorkspaceId.value = user.uid
                fetchMyTeams()
                listenToMyInvitations() // Dengarkan inbox
            } else {
                _transactions.value = emptyList()
                _myTeams.value = emptyList()
                _myInvitations.value = emptyList()
                _currentWorkspaceId.value = ""
                transactionListener?.remove()
            }
        }

        viewModelScope.launch {
            _currentWorkspaceId.collectLatest { workspaceId ->
                if (workspaceId.isNotEmpty()) listenToTransactions(workspaceId)
            }
        }
    }

    // --- LOGIC INBOX / UNDANGAN ---

    private fun listenToMyInvitations() {
        val email = currentUserEmail
        if (email.isEmpty()) return

        // Dengarkan undangan yang statusnya 'pending' dan ditujukan ke email saya
        db.collection("invitations")
            .whereEqualTo("toEmail", email)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val invites = snapshot?.documents?.map { doc ->
                    Invitation(
                        id = doc.id,
                        fromEmail = doc.getString("fromEmail") ?: "",
                        toEmail = doc.getString("toEmail") ?: "",
                        teamId = doc.getString("teamId") ?: "",
                        teamName = doc.getString("teamName") ?: "",
                        status = doc.getString("status") ?: "pending"
                    )
                } ?: emptyList()
                _myInvitations.value = invites
            }
    }

    fun sendInvite(toEmail: String, teamId: String, teamName: String) {
        val inviteData = hashMapOf(
            "fromEmail" to currentUserEmail,
            "toEmail" to toEmail,
            "teamId" to teamId,
            "teamName" to teamName,
            "status" to "pending"
        )
        db.collection("invitations").add(inviteData)
    }

    fun acceptInvite(invite: Invitation) {
        viewModelScope.launch {
            // 1. Tambahkan user ke member team
            val teamRef = db.collection("teams").document(invite.teamId)
            
            // Note: Kita pakai arrayUnion agar tidak duplikat
            db.runTransaction { transaction ->
                transaction.update(teamRef, "members", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                
                // 2. Update status undangan jadi 'accepted' (agar hilang dari inbox)
                val inviteRef = db.collection("invitations").document(invite.id)
                transaction.update(inviteRef, "status", "accepted")
            }.await()
            
            fetchMyTeams() // Refresh list team
        }
    }

    fun rejectInvite(invite: Invitation) {
        db.collection("invitations").document(invite.id).update("status", "rejected")
    }

    // --- LOGIC TEAM ---
    fun createTeam(teamName: String) {
        val uid = currentUserId
        if (uid.isEmpty()) return
        val newTeamRef = db.collection("teams").document()
        val teamData = hashMapOf("name" to teamName, "ownerId" to uid, "members" to listOf(uid))
        viewModelScope.launch { try { newTeamRef.set(teamData).await(); fetchMyTeams() } catch (e: Exception) { Log.e("FV", "Err", e) } }
    }

    private fun fetchMyTeams() {
        val uid = currentUserId
        if (uid.isEmpty()) return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("teams").whereArrayContains("members", uid).get().await()
                _myTeams.value = snapshot.documents.map { doc ->
                    Team(id = doc.id, name = doc.getString("name") ?: "", ownerId = doc.getString("ownerId") ?: "", members = (doc.get("members") as? List<String>) ?: emptyList())
                }
            } catch (e: Exception) { Log.e("FV", "ErrTeams", e) }
        }
    }

    fun switchWorkspace(id: String) { _currentWorkspaceId.value = id }
    fun switchToPersonal() { _currentWorkspaceId.value = currentUserId }

    // --- LOGIC TRANSAKSI & LAINNYA (SAMA) ---
    private fun listenToTransactions(workspaceId: String) {
        transactionListener?.remove()
        transactionListener = db.collection("workspaces").document(workspaceId).collection("transactions")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshot?.documents?.map { doc ->
                    Transaction(
                        firestoreId = doc.id,
                        amount = doc.getDouble("amount") ?: 0.0,
                        category = doc.getString("category") ?: "",
                        note = doc.getString("note"),
                        isIncome = doc.getBoolean("isIncome") ?: false,
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        userId = doc.getString("userId") ?: ""
                    )
                } ?: emptyList()
                _transactions.value = list
            }
    }

    val monthlyTransactions: Flow<List<Transaction>> = combine(_currentMonth, _searchQuery, _transactions) { calendar, query, list ->
        val (start, end) = getMonthRange(calendar)
        list.filter { 
            it.timestamp in start..end && (query.isBlank() || it.category.contains(query, true) || (it.note?.contains(query, true) == true))
        }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val monthlyIncome = monthlyTransactions.map { list -> list.filter { it.isIncome }.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)
    val monthlyExpense = monthlyTransactions.map { list -> list.filter { !it.isIncome }.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    fun addTransaction(amount: Double, category: String, note: String, isIncome: Boolean, timestamp: Long) {
        val wsId = _currentWorkspaceId.value; if (wsId.isEmpty()) return
        val data = hashMapOf("amount" to amount, "category" to category, "note" to note, "isIncome" to isIncome, "timestamp" to timestamp, "userId" to currentUserEmail)
        db.collection("workspaces").document(wsId).collection("transactions").add(data)
    }
    fun deleteTransaction(t: Transaction) { db.collection("workspaces").document(_currentWorkspaceId.value).collection("transactions").document(t.firestoreId).delete() }
    fun updateTransaction(id: String, amount: Double, category: String, note: String, isIncome: Boolean, timestamp: Long) {
        db.collection("workspaces").document(_currentWorkspaceId.value).collection("transactions").document(id)
            .update(mapOf("amount" to amount, "category" to category, "note" to note, "isIncome" to isIncome, "timestamp" to timestamp))
    }
    fun getCategories(isIncome: Boolean): Flow<List<Category>> = flow {
        val wsId = _currentWorkspaceId.value
        if (wsId.isNotEmpty()) { try { val snapshot = db.collection("workspaces").document(wsId).collection("categories").whereEqualTo("isIncome", isIncome).get().await(); emit(snapshot.documents.map { Category(name = it.getString("name") ?: "", isIncome = isIncome) }) } catch (e: Exception) { emit(emptyList()) } } else { emit(emptyList()) }
    }
    fun addCategory(name: String, isIncome: Boolean) { val wsId = _currentWorkspaceId.value; if (wsId.isNotEmpty()) db.collection("workspaces").document(wsId).collection("categories").add(hashMapOf("name" to name, "isIncome" to isIncome)) }
    fun deleteCategory(category: Category) {}
    fun onSearchQueryChange(q: String) { _searchQuery.value = q }
    fun nextMonth() { val c = _currentMonth.value.clone() as Calendar; c.add(Calendar.MONTH, 1); _currentMonth.value = c }
    fun previousMonth() { val c = _currentMonth.value.clone() as Calendar; c.add(Calendar.MONTH, -1); _currentMonth.value = c }
    private fun getMonthRange(calendar: Calendar): Pair<Long, Long> { val start = calendar.clone() as Calendar; start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0); val end = calendar.clone() as Calendar; end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH)); end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999); return Pair(start.timeInMillis, end.timeInMillis) }
}

class FinanceViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) return FinanceViewModel() as T
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
