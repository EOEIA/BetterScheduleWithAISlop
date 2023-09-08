package cz.vitskalicky.lepsirozvrh.schoolPicker

import retrofit2.http.GET

interface SchoolsWebservice {
    @GET("schoolsList.json")
    suspend fun fetchSchools(): List<SchoolInfo>
}