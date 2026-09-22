package com.example.lingoFlix.data

import androidx.room.*
import com.example.lingoFlix.model.DynamicSkill
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM dynamic_skills WHERE isEnabled = 1")
    fun getActiveSkills(): Flow<List<DynamicSkill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: DynamicSkill)

    @Delete
    suspend fun deleteSkill(skill: DynamicSkill)

    @Query("SELECT * FROM dynamic_skills WHERE id = :id")
    suspend fun getSkillById(id: String): DynamicSkill?
}
