package org.stypox.dicio.skills.reminder

import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.AlwaysBestScore
import org.dicio.skill.skill.AlwaysWorstScore
import org.dicio.skill.skill.InteractionPlan
import org.dicio.skill.skill.Score
import org.dicio.skill.skill.Skill
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.skill.Specificity
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.util.RecognizeEverythingSkill
import org.stypox.dicio.util.getString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

sealed interface ReminderOutput : SkillOutput {

    class AskWhat(
        private val onGotWhat: suspend (String) -> SkillOutput,
    ) : ReminderOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_reminder_ask_what)

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            val whatSkill = object : RecognizeEverythingSkill(ReminderInfo) {
                override suspend fun generateOutput(
                    ctx: SkillContext,
                    inputData: String
                ): SkillOutput {
                    val title = ReminderSkill.cleanTitle(inputData)
                        ?: return Message(ctx.getString(R.string.skill_reminder_failed))
                    return onGotWhat(title)
                }
            }
            return InteractionPlan.StartSubInteraction(
                reopenMicrophone = true,
                nextSkills = listOf(whatSkill),
            )
        }
    }

    class AskWhen(
        private val onGotTime: suspend (LocalDateTime) -> SkillOutput,
    ) : ReminderOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_reminder_ask_when)

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            val timeSkill = object : Skill<LocalDateTime?>(ReminderInfo, Specificity.HIGH) {
                override fun score(
                    ctx: SkillContext,
                    input: String
                ): Pair<Score, LocalDateTime?> {
                    val time = ctx.parserFormatter
                        ?.extractDateTime(input)
                        ?.now(LocalDateTime.now())
                        ?.parseFirst()
                    return Pair(if (time == null) AlwaysWorstScore else AlwaysBestScore, time)
                }

                override suspend fun generateOutput(
                    ctx: SkillContext,
                    inputData: LocalDateTime?
                ): SkillOutput {
                    return if (inputData == null) {
                        Message(ctx.getString(R.string.skill_reminder_failed))
                    } else {
                        onGotTime(inputData)
                    }
                }
            }
            return InteractionPlan.StartSubInteraction(
                reopenMicrophone = true,
                nextSkills = listOf(timeSkill),
            )
        }
    }

    class Added(
        private val title: String,
        private val time: LocalDateTime?,
        private val taskUri: Uri,
    ) : ReminderOutput {
        override fun getSpeechOutput(ctx: SkillContext): String {
            if (time == null) {
                return ctx.getString(R.string.skill_reminder_task_added, title)
            }
            val spokenTime = ctx.parserFormatter
                ?.niceDateTime(time)
                ?.now(LocalDate.now())
                ?.use24Hour(DateFormat.is24HourFormat(ctx.android))
                ?.showAmPm(true)
                ?.get()
                ?: shownTime(time)
            return ctx.getString(R.string.skill_reminder_set, title, spokenTime)
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column {
                Headline(text = title)
                if (time != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Body(text = shownTime(time))
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { TasksOrg.openTask(ctx.android, taskUri) }) {
                    Text(text = ctx.getString(R.string.skill_reminder_open_tasks))
                }
            }
        }

        private fun shownTime(time: LocalDateTime): String =
            time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }

    class Message(
        private val speech: String,
    ) : ReminderOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = speech
    }
}
