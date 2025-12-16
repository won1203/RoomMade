package com.example.roommade.data

import com.example.roommade.vm.FloorPlanViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface BoardRepository {
    fun observeBoards(uid: String): Flow<List<FloorPlanViewModel.GeneratedBoard>>
    suspend fun upsert(uid: String, board: FloorPlanViewModel.GeneratedBoard)
    suspend fun clearAll(uid: String)
    suspend fun delete(uid: String, boardId: String)
}

class FirestoreBoardRepository(
    private val db: FirebaseFirestore = Firebase.firestore
) : BoardRepository {

    private fun boards(uid: String) =
        db.collection("users").document(uid).collection("boards")

    override fun observeBoards(uid: String): Flow<List<FloorPlanViewModel.GeneratedBoard>> = callbackFlow {
        val registration = boards(uid)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val mapped = snapshot?.documents.orEmpty().mapNotNull { it.toBoard() }
                trySend(mapped)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun upsert(uid: String, board: FloorPlanViewModel.GeneratedBoard) {
        boards(uid).document(board.id).set(board.toMap()).await()
    }

    override suspend fun clearAll(uid: String) {
        val batch = db.batch()
        val docs = boards(uid).get().await().documents
        docs.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    override suspend fun delete(uid: String, boardId: String) {
        boards(uid).document(boardId).delete().await()
    }
}

private fun FloorPlanViewModel.GeneratedBoard.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "imageUrl" to imageUrl,
    "concept" to concept,
    "styleLabel" to styleLabel,
    "roomCategory" to roomCategory,
    "createdAt" to createdAt
)

private fun DocumentSnapshot.toBoard(): FloorPlanViewModel.GeneratedBoard? = try {
    FloorPlanViewModel.GeneratedBoard(
        id = getString("id").orEmpty(),
        imageUrl = getString("imageUrl").orEmpty(),
        concept = getString("concept").orEmpty(),
        styleLabel = getString("styleLabel").orEmpty(),
        roomCategory = getString("roomCategory").orEmpty(),
        createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    )
} catch (_: Throwable) {
    null
}
