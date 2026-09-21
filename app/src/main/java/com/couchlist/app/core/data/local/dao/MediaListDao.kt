package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListJoinEntity
import com.couchlist.app.core.data.local.entity.MediaListSummaryRow
import com.couchlist.app.core.domain.model.MediaListType
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaListDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertListIgnore(list: MediaListEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertJoinIgnore(join: MediaListJoinEntity): Long

    @Query(
        "SELECT id FROM lists WHERE name = :name AND type = :type " +
            "ORDER BY id ASC LIMIT 1",
    )
    suspend fun findListId(name: String, type: MediaListType): Long?

    @Query(
        "SELECT lists.*, COUNT(media_list_joins.id) AS item_count " +
            "FROM lists LEFT JOIN media_list_joins ON lists.id = media_list_joins.list_id " +
            "GROUP BY lists.id ORDER BY lists.is_pinned DESC, lists.sort_order ASC, lists.name ASC",
    )
    fun observeSummaries(): Flow<List<MediaListSummaryRow>>

    @Query("SELECT * FROM media_list_joins WHERE media_id = :mediaId")
    suspend fun getMemberships(mediaId: Long): List<MediaListJoinEntity>

    @Query("DELETE FROM media_list_joins WHERE media_id = :mediaId")
    suspend fun deleteMemberships(mediaId: Long)

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("DELETE FROM media_list_joins WHERE list_id = :listId AND media_id = :mediaId")
    suspend fun removeFromList(listId: Long, mediaId: Long)

    @Query("SELECT media_id FROM media_list_joins WHERE list_id = :listId")
    fun observeListMediaIds(listId: Long): Flow<List<Long>>

    @Query("SELECT * FROM media_list_joins WHERE list_id = :listId")
    suspend fun getListMemberships(listId: Long): List<MediaListJoinEntity>

    @Query("SELECT * FROM lists WHERE id = :listId")
    suspend fun getListById(listId: Long): MediaListEntity?

    @Query("SELECT * FROM lists")
    suspend fun getAllLists(): List<MediaListEntity>

    @Query("SELECT * FROM media_list_joins")
    suspend fun getAllJoins(): List<MediaListJoinEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllLists(lists: List<MediaListEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllJoins(joins: List<MediaListJoinEntity>): List<Long>

    @Query("DELETE FROM media_list_joins")
    suspend fun deleteAllJoins()

    @Query("DELETE FROM lists")
    suspend fun deleteAllLists()
}
