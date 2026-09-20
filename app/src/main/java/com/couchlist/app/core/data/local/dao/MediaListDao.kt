package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.couchlist.app.core.data.local.entity.MediaListEntity
import com.couchlist.app.core.data.local.entity.MediaListJoinEntity
import com.couchlist.app.core.domain.model.MediaListType

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

    @Query("DELETE FROM media_list_joins WHERE media_id = :mediaId")
    suspend fun deleteMemberships(mediaId: Long)
}
