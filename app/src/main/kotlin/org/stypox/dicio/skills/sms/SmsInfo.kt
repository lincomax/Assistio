package org.stypox.dicio.skills.sms

import android.Manifest
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.Permission
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillInfo
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.PERMISSION_READ_CONTACTS

val PERMISSION_SEND_SMS = Permission.NormalPermission(
    name = R.string.perm_send_sms,
    id = Manifest.permission.SEND_SMS,
)

object SmsInfo : SkillInfo("sms") {
    override fun name(context: Context) =
        context.getString(R.string.skill_name_sms)

    override fun sentenceExample(context: Context) =
        context.getString(R.string.skill_sentence_example_sms)

    @Composable
    override fun icon() =
        rememberVectorPainter(Icons.Default.Sms)

    override val neededPermissions: List<Permission>
            = listOf(PERMISSION_READ_CONTACTS, PERMISSION_SEND_SMS)

    override fun build(ctx: SkillContext): Skill<*>? {
        val data = Sentences.Sms[ctx.sentencesLanguage] ?: return null
        val yesNoData = Sentences.UtilYesNo[ctx.sentencesLanguage] ?: return null
        return SmsSkill(SmsInfo, data, yesNoData)
    }
}
