package com.couchlist.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.couchlist.app.core.data.local.entity.LibraryMediaRow
import com.couchlist.app.core.data.local.entity.ListGroupEntity
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
        "SELECT lists.*, COUNT(media_list_joins.id) AS item_count, " +
            "COALESCE(cover.artwork_uri, (" +
            "SELECT preview_media.artwork_uri FROM media_list_joins preview_join " +
            "INNER JOIN library_items preview_library " +
            "ON preview_library.id = preview_join.library_item_id " +
            "INNER JOIN media_items preview_media ON preview_media.id = preview_library.media_id " +
            "WHERE preview_join.list_id = lists.id " +
            "ORDER BY preview_join.added_at DESC, preview_join.id DESC LIMIT 1" +
            ")) AS cover_artwork_uri " +
            "FROM lists " +
            "LEFT JOIN media_list_joins ON lists.id = media_list_joins.list_id " +
            "LEFT JOIN media_items cover ON cover.id = lists.cover_media_id " +
            "GROUP BY lists.id ORDER BY lists.is_pinned DESC, lists.sort_order ASC, lists.name ASC",
    )
    fun observeSummaries(): Flow<List<MediaListSummaryRow>>

    @Query("SELECT * FROM media_list_joins WHERE library_item_id = :libraryItemId")
    suspend fun getMemberships(libraryItemId: Long): List<MediaListJoinEntity>

    @Query("DELETE FROM media_list_joins WHERE library_item_id = :libraryItemId")
    suspend fun deleteMemberships(libraryItemId: Long)

    @Query("DELETE FROM lists WHERE id = :listId")
    suspend fun deleteList(listId: Long)

    @Query("DELETE FROM media_list_joins WHERE list_id = :listId AND library_item_id = :libraryItemId")
    suspend fun removeFromList(listId: Long, libraryItemId: Long)

    @Query(
        "SELECT library_items.media_id FROM media_list_joins " +
            "INNER JOIN library_items ON library_items.id = media_list_joins.library_item_id " +
            "WHERE media_list_joins.list_id = :listId",
    )
    fun observeListMediaIds(listId: Long): Flow<List<Long>>

    @Query("SELECT list_id FROM media_list_joins WHERE library_item_id = :libraryItemId")
    fun observeMembershipListIds(libraryItemId: Long): Flow<List<Long>>

    @Query(
        LIBRARY_MEDIA_COLUMNS +
            "FROM media_list_joins " +
            "INNER JOIN library_items ON library_items.id = media_list_joins.library_item_id " +
            "INNER JOIN media_items ON library_items.media_id = media_items.id " +
            "LEFT JOIN video_metadata ON video_metadata.media_id = media_items.id " +
            "WHERE media_list_joins.list_id = :listId " +
            "ORDER BY media_list_joins.added_at DESC, media_list_joins.id DESC",
    )
    fun observeListItems(listId: Long): Flow<List<LibraryMediaRow>>

    @Query(
        "SELECT media_items.source, media_items.category, media_items.external_id " +
            "FROM media_list_joins " +
            "INNER JOIN library_items ON library_items.id = media_list_joins.library_item_id " +
            "INNER JOIN media_items ON media_items.id = library_items.media_id " +
            "INNER JOIN lists ON lists.id = media_list_joins.list_id " +
            "WHERE lists.type = 'PILE'",
    )
    fun observePileMediaReferences(): Flow<List<com.couchlist.app.core.data.local.entity.MediaReferenceRow>>

    @Query("SELECT * FROM media_list_joins WHERE list_id = :listId")
    suspend fun getListMemberships(listId: Long): List<MediaListJoinEntity>

    @Query("SELECT * FROM lists WHERE id = :listId")
    suspend fun getListById(listId: Long): MediaListEntity?

    @Query("SELECT * FROM lists WHERE id = :listId")
    fun observeListById(listId: Long): Flow<MediaListEntity?>

    @Query("SELECT * FROM lists WHERE type = 'PILE' ORDER BY id ASC LIMIT 1")
    suspend fun getPile(): MediaListEntity?

    @Query(
        "UPDATE lists SET type = 'COLLECTION', is_pinned = 0, updated_at = :updatedAt " +
            "WHERE type = 'PILE' AND id != :canonicalPileId",
    )
    suspend fun convertDuplicatePiles(canonicalPileId: Long, updatedAt: Long)

    @Query(
        "UPDATE lists SET name = 'The Pile', is_pinned = 1, group_id = NULL, " +
            "updated_at = :updatedAt WHERE id = :pileId AND type = 'PILE'",
    )
    suspend fun normalizePile(pileId: Long, updatedAt: Long)

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM lists")
    suspend fun nextListSortOrder(): Int

    @Query(
        "UPDATE lists SET name = :name, description = :description, type = :type, " +
            "group_id = :groupId, updated_at = :updatedAt WHERE id = :listId",
    )
    suspend fun updateList(
        listId: Long,
        name: String,
        description: String?,
        type: MediaListType,
        groupId: Long?,
        updatedAt: Long,
    )

    @Query("UPDATE lists SET is_pinned = :isPinned, updated_at = :updatedAt WHERE id = :listId")
    suspend fun updatePinned(listId: Long, isPinned: Boolean, updatedAt: Long)

    @Query("UPDATE lists SET group_id = :groupId, updated_at = :updatedAt WHERE id = :listId")
    suspend fun updateGroup(listId: Long, groupId: Long?, updatedAt: Long)

    @Query("UPDATE lists SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :listId")
    suspend fun updateListSortOrder(listId: Long, sortOrder: Int, updatedAt: Long)

    @Query("SELECT * FROM lists")
    suspend fun getAllLists(): List<MediaListEntity>

    @Query("SELECT * FROM media_list_joins")
    suspend fun getAllJoins(): List<MediaListJoinEntity>

    @Query("SELECT * FROM list_groups ORDER BY sort_order ASC, name ASC")
    fun observeGroups(): Flow<List<ListGroupEntity>>

    @Query("SELECT * FROM list_groups ORDER BY sort_order ASC, name ASC")
    suspend fun getAllGroups(): List<ListGroupEntity>

    @Query("SELECT * FROM list_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): ListGroupEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(group: ListGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllGroups(groups: List<ListGroupEntity>): List<Long>

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM list_groups")
    suspend fun nextGroupSortOrder(): Int

    @Query("UPDATE list_groups SET name = :name, updated_at = :updatedAt WHERE id = :groupId")
    suspend fun updateGroupName(groupId: Long, name: String, updatedAt: Long)

    @Query("UPDATE list_groups SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :groupId")
    suspend fun updateGroupSortOrder(groupId: Long, sortOrder: Int, updatedAt: Long)

    @Query("DELETE FROM list_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: Long)

    @Query("DELETE FROM list_groups")
    suspend fun deleteAllGroups()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllLists(lists: List<MediaListEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllJoins(joins: List<MediaListJoinEntity>): List<Long>

    @Query("DELETE FROM media_list_joins")
    suspend fun deleteAllJoins()

    @Query("DELETE FROM lists")
    suspend fun deleteAllLists()
}
