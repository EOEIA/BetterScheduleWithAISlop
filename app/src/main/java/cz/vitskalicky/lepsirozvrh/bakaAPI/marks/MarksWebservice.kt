package cz.vitskalicky.lepsirozvrh.bakaAPI.marks

import retrofit2.http.GET

interface MarksWebservice {
    @GET("api/3/marks")
    suspend fun getMarks(): MarksResponse
}
