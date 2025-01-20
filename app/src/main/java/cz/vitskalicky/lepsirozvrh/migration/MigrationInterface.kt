package cz.vitskalicky.lepsirozvrh.migration

import android.content.Context

interface MigrationInterface {
    /** Apply this migration if last seen version code is lower that this */
    val versionCode: Int;
    /** Perform the migration */
    suspend fun migrate(context: Context);
}