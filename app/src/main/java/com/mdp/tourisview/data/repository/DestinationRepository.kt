package com.mdp.tourisview.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.mdp.tourisview.data.api.ApiService
import com.mdp.tourisview.data.local.room.dao.DestinationDao
import com.mdp.tourisview.data.local.room.model.RoomDestination
import com.mdp.tourisview.data.mock.server.MockServer
import com.mdp.tourisview.data.mock.server.model.MockServerDestination
import com.mdp.tourisview.data.mock.server.model.MockServerReview
import com.mdp.tourisview.data.mock.server.model.convertToLocalDestination
import com.mdp.tourisview.util.ApiResult
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.util.Date

class DestinationRepository private constructor(
    private val apiService: ApiService,
    private val destinationDao: DestinationDao
) {
//    suspend fun uploadDestination(
//        name: String, image: Uri, description: String,
//        latitude: Double, longitude: Double, poster: String
//    ): ApiResult<UploadDestinationResult>{
//        return try{
//            val result = MockDB.uploadDestination(
//                name = name, image = image,
//                description = description, latitude = latitude,
//                longitude = longitude, poster = poster
//            )
//            ApiResult.Success(result)
//        }catch (exc: Exception){
//            ApiResult.Error(exc.message ?: "Upload failed")
//        }
//    }
//
//    suspend fun getAllDestinations(name: String? = null): ApiResult<List<Destination>>{
//        return try{
//            val result = MockDB.getAllDestinations(name)
//            ApiResult.Success(result)
//        }catch (exc: Exception){
//            ApiResult.Error(exc.message ?: "Failed to fetch destinations")
//        }
//    }

    suspend fun insertDestination(
        name: RequestBody, image: MultipartBody.Part, description: RequestBody,
        latitude: RequestBody, longitude: RequestBody,
        locationName: RequestBody, poster: RequestBody
    ): ApiResult<String>{
        return try{
            val result = apiService.uploadDestination(
                name = name,
                image = image,
                description = description,
                latitude = latitude,
                longitude = longitude,
                locationName = locationName,
                poster = poster
            )
            val roomDestination = RoomDestination(
                id = result.data.id,
                poster = result.data.poster,
                name = result.data.name,
                imageUrl = result.data.imageUrl,
                description = result.data.description,
                latitude = result.data.latitude,
                longitude = result.data.longitude,
                locationName = result.data.locationName,
                createdAt = Date().toString(), false,
                avgStar = 0.0
            )
            destinationDao.insertDestination(roomDestination)
            ApiResult.Success("Destination added successfully")
        }catch(exc: Exception){
            ApiResult.Error("Failed to add destination ${exc.message}")
        }
    }

    suspend fun insertAllDestinations(roomDestinations: List<RoomDestination>){
        destinationDao.insertAllDestinations(roomDestinations)
    }

    suspend fun toggleDestinationBookmark(id: Int, email: String): ApiResult<String>{
        return try {
            destinationDao.toggleBookmark(id)
            apiService.toggleBookmark(email, id)
            ApiResult.Success("Bookmarked success")
        }catch(exc: Exception){
            ApiResult.Error("Failed to bookmarked the destination")
        }
    }

    suspend fun toggleBookmark(email: String, destinationId: Int): ApiResult<Boolean>{
        return try{
            val result = apiService.toggleBookmark(email, destinationId)
            return ApiResult.Success(result.isBookmarked)
        }catch(exc: Exception){
            ApiResult.Error("Failed to bookmarked the destination")
        }
    }

    fun getDestinations(name: String): LiveData<List<RoomDestination>>{
        return destinationDao.getDestinations(name)
    }

    fun getAllDestinations(): LiveData<List<RoomDestination>>{
        return destinationDao.getAllDestinations()
    }

    fun getBookmarkedDestinations(): LiveData<List<RoomDestination>>{
        return destinationDao.getBookmarkedDestination()
    }

    suspend fun fetchDestinationsFromServer(email: String): ApiResult<String>{
        return try {
//            val result = MockServer.getAllDestinations()
            val result = apiService.getAllDestinations(email = email)
            destinationDao.deleteDestinations()
            destinationDao.insertAllDestinations(
                result.map { it.convertToLocalDestination() }
            )
            Log.d("Test Fetching", "Hallo2")

            ApiResult.Success("Sync success")
        }catch(exc: Exception) {
            exc.message?.let { Log.d("Test Fetching", it) }

            ApiResult.Error(exc.message ?: "Sync Failed")
        }
    }

    suspend fun getAllHistory(email:String):ApiResult<List<MockServerDestination>>{
        return try {
//            val result = MockServer.getAllHistory(email)
            val result = apiService.getAllHistory(email)
            ApiResult.Success(result)
        }catch(exc: Exception) {
            ApiResult.Error("fetch failed")
        }
    }

    suspend fun insertReview(
        reviewer: String, destinationId: Int, reviewText: String, star: Int
    ): ApiResult<String>{
        Log.i("reviewer", reviewer)
        Log.i("destinationID", destinationId.toString())
        Log.i("reviewText", reviewText)
        Log.i("star", star.toString())

        return try{
//            val roomReview = RoomReview(
//                id = "REV_${UUID.randomUUID()}",
//                destinationId = destinationId,
//                reviewText = reviewText,
//                star = star
//            )
            apiService.insertReview(reviewer, destinationId.toInt(), reviewText, star)
//            destinationDao.insertReview(roomReview)
            ApiResult.Success("Review added successfully")
        }catch(exc: Exception){
            ApiResult.Error("Failed to add review")
        }
    }

    suspend fun getAllReviews(destinationId: Int): ApiResult<List<MockServerReview>>{
//        return destinationDao.getReviews(destinationId)
        return try {
//            val result = MockServer.getAllHistory(email)
            val result = apiService.getAllReview(destinationId)
            ApiResult.Success(result)
        }catch(exc: Exception) {
            ApiResult.Error("fetch failed")
        }
    }

    companion object{
        @Volatile
        private var instance: DestinationRepository? = null
        fun getInstance(
            apiService: ApiService,
            destinationDao: DestinationDao
        ): DestinationRepository{
            return instance ?: synchronized(this){
                instance ?: DestinationRepository(apiService, destinationDao)
            }.also { instance = it }
        }
    }
}