package es.unex.giiis.asee.tiviclone.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import es.unex.giiis.asee.tiviclone.data.model.Show
import es.unex.giiis.asee.tiviclone.data.model.UserShowCrossRef
import es.unex.giiis.asee.tiviclone.data.model.UserWithShows

@Dao
interface ShowDao {

    @Query("SELECT * FROM show WHERE showId = :id")
    suspend fun findById(id: Int): Show

    @Query("SELECT * FROM show")
    fun getShows(): LiveData<List<Show>>

    @Query("SELECT count(*) FROM show")
    suspend fun getNumberOfShows(): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(shows: List<Show>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(show: Show)

    @Delete
    suspend fun delete(show: Show)

    @Update
    suspend fun update(show: Show)

    @Delete
    suspend fun delete(userShow: UserShowCrossRef)

    @Transaction
    @Query("SELECT * FROM User where userId = :userId")
    suspend fun getUserWithShows(userId: Long): UserWithShows

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserShow(crossRef: UserShowCrossRef)

    @Transaction
    suspend fun insertAndRelate(show: Show, userId: Long) {
        insert(show)
        insertUserShow(UserShowCrossRef(userId, show.showId))
    }
}