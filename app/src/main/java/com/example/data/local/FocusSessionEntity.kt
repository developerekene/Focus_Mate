package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionType: String,
    val durationMinutes: Int,
    val tag: String,
    val completedAtMillis: Long,
    val dateKey: String
)
