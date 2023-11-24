package es.unex.giiis.asee.tiviclone.data


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import es.unex.giiis.asee.tiviclone.api.APIError
import es.unex.giiis.asee.tiviclone.api.TVShowAPI
import es.unex.giiis.asee.tiviclone.api.getNetworkService
import es.unex.giiis.asee.tiviclone.data.api.TvShow
import es.unex.giiis.asee.tiviclone.data.model.Show
import es.unex.giiis.asee.tiviclone.data.model.UserShowCrossRef
import es.unex.giiis.asee.tiviclone.data.model.UserWithShows
import es.unex.giiis.asee.tiviclone.database.dao.ShowDao
import es.unex.giiis.asee.tiviclone.database.dao.UserDao

class Repository private constructor(
    private val userDao: UserDao,
    private val showDao: ShowDao,
    private val networkService: TVShowAPI
) {
    private var lastUpdateTimeMillis: Long = 0L

    val shows = showDao.getShows()

    private val userFilter = MutableLiveData<Long>()

    val showsInLibrary: LiveData<UserWithShows> =
        userFilter.switchMap{ userid -> showDao.getUserWithShows(userid) }

    fun setUserid(userid: Long) {
        userFilter.value = userid
    }

    suspend fun deleteShowFromLibrary(show: Show, userId: Long) {
        showDao.delete(UserShowCrossRef(userId, show.showId))
        showDao.update(show)
    }
    suspend fun showToLibrary(show: Show, userId: Long) {
        showDao.update(show)
        showDao.insertUserShow(UserShowCrossRef(userId, show.showId))
    }

    suspend fun fetchShowDetail(showId: Int): TvShow {
        var show = TvShow()
        try {
            show = getNetworkService().getShowDetail(showId).tvShow ?: TvShow()
        } catch (cause: Throwable) {
            throw APIError("Unable to fetch data from API", cause)
        }
        return show
    }

    /**
     * Update the shows cache.
     *
     * This function may decide to avoid making a network requests on every call based on a
     * cache-invalidation policy.
     */
    suspend fun tryUpdateRecentShowsCache() {
        if (shouldUpdateShowsCache()) fetchRecentShows()
    }

    /**
     * Fetch a new list of shows from the network, and append them to [ShowDao]
     */
    private suspend fun fetchRecentShows() {
        try {
            val shows = networkService.getShows(1).tvShows.map { it.toShow()}
            showDao.insertAll(shows)
            lastUpdateTimeMillis = System.currentTimeMillis()
        } catch (cause: Throwable) {
            throw APIError("Unable to fetch data from API", cause)
        }
    }

    /**
     * Returns true if we should make a network request.
     */
    private suspend fun shouldUpdateShowsCache(): Boolean {
        val lastFetchTimeMillis = lastUpdateTimeMillis
        val timeFromLastFetch = System.currentTimeMillis() - lastFetchTimeMillis
        return timeFromLastFetch > MIN_TIME_FROM_LAST_FETCH_MILLIS || showDao.getNumberOfShows() == 0L
    }

    companion object {
        private const val MIN_TIME_FROM_LAST_FETCH_MILLIS: Long = 3000

        @Volatile
        private var INSTANCE: Repository? = null

        fun getInstance(
            userDao: UserDao,
            showDao: ShowDao,
            showAPI: TVShowAPI
        ): Repository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Repository(userDao, showDao, showAPI).also { INSTANCE = it }
            }
        }
    }
}