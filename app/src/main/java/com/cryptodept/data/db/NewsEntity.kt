package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.NewsSentiment
import kotlinx.collections.immutable.toImmutableList

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val sentiment: String,
    val currencies: String,
) {
    fun toDomain() =
        NewsItem(
            id = id,
            title = title,
            url = url,
            source = source,
            publishedAt = publishedAt,
            sentiment = NewsSentiment.valueOf(sentiment),
            currencies = if (currencies.isBlank()) kotlinx.collections.immutable.persistentListOf() else currencies.split(",").toImmutableList(),
        )

    companion object {
        fun fromDomain(item: NewsItem) =
            NewsEntity(
                id = item.id,
                title = item.title,
                url = item.url,
                source = item.source,
                publishedAt = item.publishedAt,
                sentiment = item.sentiment.name,
                currencies = item.currencies.joinToString(","),
            )
    }
}
