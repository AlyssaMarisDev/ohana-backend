package com.ohana.shared.enums

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    ;

    companion object {
        fun allTaskStatusesString(): String {
            return TaskStatus.values().joinToString(", ")
        }
    }
}
