package org.stypox.dicio.skills.reminder

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Permission
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences

val PERMISSION_TASKS_ORG_WRITE = Permission.NormalPermission(
    name = R.string.perm_tasks_org,
    id = TasksOrg.PERMISSION_WRITE,
)

object ReminderInfo : SkillInfo("reminder") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_reminder)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_reminder)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.NotificationAdd)

    override val neededPermissions: List<Permission>
            = listOf(PERMISSION_TASKS_ORG_WRITE)

    override fun build(ctx: SkillContext): Skill<*>? {
        // dates and times can only be understood in languages with a number parser
        if (ctx.parserFormatter == null) return null
        val data = Sentences.Reminder[ctx.sentencesLanguage] ?: return null
        return ReminderSkill(ReminderInfo, data)
    }
}
