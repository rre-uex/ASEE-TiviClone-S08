package es.unex.giiis.asee.tiviclone.data


import es.unex.giiis.asee.tiviclone.api.APIError
import es.unex.giiis.asee.tiviclone.api.TVShowAPI
import es.unex.giiis.asee.tiviclone.database.dao.ShowDao
import es.unex.giiis.asee.tiviclone.database.dao.UserDao

class Repository private constructor(
    private val userDao: UserDao,
    private val showDao: ShowDao,
    private val networkService: TVShowAPI
) {
    private var lastUpdateTimeMillis: Long = 0L

    val shows = showDao.getShows()

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
        private const val MIN_TIME_FROM_LAST_FETCH_MILLIS: Long = 30000

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