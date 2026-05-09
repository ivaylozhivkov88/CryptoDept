package com.cryptodept.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NewsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsEntity>)

    @Query("SELECT * FROM news ORDER BY publishedAt DESC")
    fun getPagingSource(): PagingSource<Int, NewsEntity>

    @Query("SELECT * FROM news ORDER BY publishedAt DESC LIMIT :limit")
    fun getLatestNews(limit: Int): kotlinx.coroutines.flow.Flow<List<NewsEntity>>

    @Query("DELETE FROM news WHERE publishedAt < :olderThan")
    suspend fun deleteOldNews(olderThan: Long)
}
