package cz.vitskalicky.lepsirozvrh.bakaAPI.homework

import retrofit2.http.GET

interface HomeworkWebservice {
    @GET("api/3/homeworks")
    suspend fun getHomeworks(): HomeworksResponse
}
