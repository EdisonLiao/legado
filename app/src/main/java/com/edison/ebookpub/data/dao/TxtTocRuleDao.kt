package com.edison.ebookpub.data.dao

import androidx.room.*
import com.edison.ebookpub.data.entities.TxtTocRule
import kotlinx.coroutines.flow.Flow

@Dao
interface TxtTocRuleDao {

    @Query("select * from txtTocRules order by serialNumber")
    fun observeAll(): Flow<List<TxtTocRule>>

    @get:Query("select * from txtTocRules order by serialNumber")
    val all: List<TxtTocRule>

    @get:Query("select * from txtTocRules where enable = 1 order by serialNumber")
    val enabled: List<TxtTocRule>

    @get:Query("select * from txtTocRules where enable != 1 order by serialNumber")
    val disabled: List<TxtTocRule>

    @Query("select * from txtTocRules where id = :id")
    fun get(id: Long): TxtTocRule?

    @get:Query("select ifNull(min(serialNumber), 0) from txtTocRules")
    val minOrder: Int

    @get:Query("select ifNull(max(serialNumber), 0) from txtTocRules")
    val maxOrder: Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rule: TxtTocRule)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(vararg rule: TxtTocRule)

    @Delete
    fun delete(vararg rule: TxtTocRule)

    @Query("delete from txtTocRules where id < 0")
    fun deleteDefault()
}