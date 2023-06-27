package cz.vitskalicky.lepsirozvrh.database

import androidx.lifecycle.LiveData
import androidx.room.*
import cz.vitskalicky.lepsirozvrh.model.Account

@Dao
abstract class AccountDao {
    @Update
    abstract suspend fun updateAccount(account: Account)
    @Insert
    abstract suspend fun insertAccount(account: Account): Int //todo test if returning primary key works as expected or change to handle rowID, see https://developer.android.com/training/data-storage/room/accessing-data#convenience-insert and https://www.sqlite.org/rowidtable.html
    @Delete
    abstract suspend fun deleteAccount(account: Account): Int
    @Query("SELECT * FROM account")
    abstract fun loadAllAccountsLD(): LiveData<List<Account>>
    @MapInfo(keyColumn = "id", valueColumn = "")
    @Query("SELECT * FROM account")
    //todo check if it is working as intended
    abstract fun loadAllAccountsLDMap(): LiveData<Map<Int, Account>>
    @Query("SELECT * FROM account")
    abstract suspend fun loadAllAccounts(): List<Account>
    @Query("SELECT * FROM account WHERE id = :id")
    abstract fun loadAccountLD(id: Int): LiveData<Account?>
    @Query("SELECT * FROM account WHERE id = :id")
    abstract suspend fun loadAccount(id: Int): Account?

}