package com.example.lingoFlix.data.repository

import com.example.lingoFlix.data.SkillDao
import com.example.lingoFlix.model.DynamicSkill
import kotlinx.coroutines.flow.Flow

class SkillRepository(private val skillDao: SkillDao) {
    
    fun getActiveSkills(): Flow<List<DynamicSkill>> = skillDao.getActiveSkills()

    suspend fun seedDefaultSkills() {
        val defaultSkills = listOf(
            DynamicSkill(
                id = "typo_corrector",
                name = "מתקן שגיאות כתיב (Typo Fixer)",
                description = "סורק את הכתוביות שלך ומתקן שגיאות כתיב ודקדוק בעזרת AI.",
                iconName = "Spellcheck",
                category = "Tools",
                logicInstructions = "Analyze the provided subtitle text, identify spelling errors, and return a corrected version while preserving timestamps."
            ),
            DynamicSkill(
                id = "s22_optimizer",
                name = "אופטימיזציית Samsung S22",
                description = "מבצע בדיקת חומרה ותוכנה כדי לוודא שהאפליקציה רצה ב-120Hz וניצול מסך מלא.",
                iconName = "Samsung",
                category = "System",
                logicInstructions = "Check current device model, refresh rate settings, and window insets to ensure Edge-to-Edge compatibility with S22 display."
            ),
            DynamicSkill(
                id = "code_quality_sentinel",
                name = "שומר איכות הקוד (Code Guard)",
                description = "מוודא שקבצים לא עוברים את ה-300 שורות, שמות משתנים ברורים, ושימוש ב-Try/Catch.",
                iconName = "Security",
                category = "Development",
                logicInstructions = "Analyze Kotlin files for: 1. Line count (>300 is fail). 2. Variable naming clarity. 3. Existence of try-catch blocks in non-trivial functions. 4. Presence of descriptive logging."
            )
        )
        
        defaultSkills.forEach { 
            if (skillDao.getSkillById(it.id) == null) {
                skillDao.insertSkill(it)
            }
        }
    }
}
