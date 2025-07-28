package com.ohana.domain.tags

import com.ohana.data.tags.Tag
import com.ohana.data.unitOfWork.UnitOfWorkContext
import com.ohana.shared.Guid
import java.time.Instant

class DefaultTagService {
    companion object {
        private val DEFAULT_TAGS =
            listOf(
                "metas" to "#4ECDC4",
                "adult" to "#FF6B6B",
                "work" to "#45B7D1",
                "kids" to "#96CEB4",
                "chores" to "#FFEAA7",
            )
    }

    fun createDefaultTags(
        context: UnitOfWorkContext,
        householdId: String,
    ): List<Tag> {
        val now = Instant.now()

        return DEFAULT_TAGS.map { (name, color) ->
            val tag =
                Tag(
                    id = Guid.generate(),
                    name = name,
                    color = color,
                    householdId = householdId,
                    createdAt = now,
                    updatedAt = now,
                )

            context.tags.create(tag)
        }
    }
}
