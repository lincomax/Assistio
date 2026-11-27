package org.stypox.dicio.skills.reminder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.R
import org.stypox.dicio.sentences.Sentences.Reminder
import org.stypox.dicio.util.getString
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Reminder>,
) : StandardRecognizerSkill<Reminder>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Reminder): SkillOutput {
        return when (inputData) {
            is Reminder.Set -> {
                val what = cleanTitle(inputData.what)
                val time = inputData.time
                when {
                    what == null -> ReminderOutput.AskWhat { askedWhat ->
                        if (time == null) {
                            ReminderOutput.AskWhen { askedTime -> addReminder(ctx, askedWhat, askedTime) }
                        } else {
                            addReminder(ctx, askedWhat, time)
                        }
                    }
                    time == null -> ReminderOutput.AskWhen { askedTime ->
                        addReminder(ctx, what, askedTime)
                    }
                    else -> addReminder(ctx, what, time)
                }
            }
            is Reminder.AddTask -> {
                val what = cleanTitle(inputData.what)
                if (what == null) {
                    ReminderOutput.AskWhat { askedWhat -> addTask(ctx, askedWhat) }
                } else {
                    addTask(ctx, what)
                }
            }
        }
    }

    companion object {
        /** Used when only a day is said, e.g. "remind me tomorrow to ...". */
        private val DEFAULT_TIME_OF_DAY: LocalTime = LocalTime.of(9, 0)

        fun cleanTitle(raw: String?): String? {
            val title = raw?.trim()?.trim('.', ',', '!', '?')?.trim()
            return if (title.isNullOrEmpty()) {
                null
            } else {
                title.replaceFirstChar { it.uppercaseChar() }
            }
        }

        /**
         * Makes the parsed date and time usable for a reminder:
         * - a day without a time of day ("tomorrow", "on friday") keeps the current time of day
         *   when parsed, so it is moved to [DEFAULT_TIME_OF_DAY];
         * - a time earlier today ("at 5" said at 2 PM) is moved 12 hours later if that is still
         *   today and in the future, otherwise to the same time tomorrow.
         * @return null if the time is still in the past after these fixes
         */
        fun normalizeTime(parsed: LocalDateTime, now: LocalDateTime): LocalDateTime? {
            var time = parsed

            val sameTimeOfDayAsNow = parsed.nano != 0 && Duration.between(
                now.toLocalTime(), parsed.toLocalTime()
            ).abs() < Duration.ofMinutes(1)
            if (sameTimeOfDayAsNow && parsed.toLocalDate().isAfter(now.toLocalDate())) {
                time = parsed.toLocalDate().atTime(DEFAULT_TIME_OF_DAY)
            }

            time = time.withNano(0)
            if (time.isBefore(now) && time.toLocalDate() == now.toLocalDate()) {
                time = if (time.hour < 12 && time.plusHours(12).isAfter(now)) {
                    time.plusHours(12)
                } else {
                    time.plusDays(1)
                }
            }

            return if (time.isBefore(now)) null else time
        }

        suspend fun addReminder(
            ctx: SkillContext,
            title: String,
            parsedTime: LocalDateTime,
        ): SkillOutput {
            val now = LocalDateTime.now()
            val time = normalizeTime(parsedTime, now)
                ?: return ReminderOutput.Message(ctx.getString(R.string.skill_reminder_in_the_past))

            val result = withContext(Dispatchers.IO) {
                TasksOrg.addTask(ctx.android, title, time)
            }
            return resultToOutput(ctx, result, title, time)
        }

        suspend fun addTask(ctx: SkillContext, title: String): SkillOutput {
            val result = withContext(Dispatchers.IO) {
                TasksOrg.addTask(ctx.android, title, null)
            }
            return resultToOutput(ctx, result, title, null)
        }

        private fun resultToOutput(
            ctx: SkillContext,
            result: TasksOrg.Result,
            title: String,
            time: LocalDateTime?,
        ): SkillOutput = when (result) {
            is TasksOrg.Result.Added -> ReminderOutput.Added(title, time, result.taskUri)
            TasksOrg.Result.NotInstalled ->
                ReminderOutput.Message(ctx.getString(R.string.skill_reminder_not_installed))
            TasksOrg.Result.Failed ->
                ReminderOutput.Message(ctx.getString(R.string.skill_reminder_failed))
        }
    }
}
