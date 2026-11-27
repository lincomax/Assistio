package org.stypox.dicio.skills.sms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import org.dicio.skill.standard.StandardRecognizerData
import org.stypox.dicio.R
import org.stypox.dicio.io.graphical.Body
import org.stypox.dicio.io.graphical.Headline
import org.stypox.dicio.io.graphical.HeadlineSpeechSkillOutput
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.util.RecognizeEverythingSkill
import org.stypox.dicio.util.RecognizeYesNoSkill
import org.stypox.dicio.util.StringUtils
import org.stypox.dicio.util.getString

sealed interface SmsOutput : SkillOutput {

    /** Several contacts match: list them and let the user pick one by name or by number. */
    class ChooseContact(
        private val contacts: List<Pair<String, List<String>>>,
        private val onChosen: suspend (String, String) -> SkillOutput,
    ) : SmsOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = if (contacts.isEmpty()) {
            ctx.getString(R.string.skill_sms_unknown_contact)
        } else {
            ctx.getString(R.string.skill_sms_found_contacts, contacts.size)
        }

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            if (contacts.isEmpty()) {
                return InteractionPlan.FinishInteraction
            }

            val nextSkills = mutableListOf<Skill<*>>(
                ChooserByName(contacts.map { Pair(it.first, it.second[0]) }, onChosen)
            )
            if (ctx.parserFormatter != null) {
                nextSkills.add(
                    ChooserByIndex(
                        contacts.flatMap { c -> c.second.map { number -> Pair(c.first, number) } },
                        onChosen,
                    )
                )
            }
            return InteractionPlan.StartSubInteraction(
                reopenMicrophone = true,
                nextSkills = nextSkills,
            )
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            if (contacts.isEmpty()) {
                Headline(text = getSpeechOutput(ctx))
                return
            }
            Column {
                Headline(text = getSpeechOutput(ctx))
                var index = 1
                for (contact in contacts) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = contact.first,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    for (number in contact.second) {
                        Text(
                            text = "${index++}. $number",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }

    /** The recipient is known but the message is not: ask for it and take whatever is said. */
    class AskMessage(
        private val name: String,
        private val number: String,
        private val yesNoData: StandardRecognizerData<Sentences.UtilYesNo>,
    ) : SmsOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_sms_ask_message, name)

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            val messageSkill = object : RecognizeEverythingSkill(SmsInfo) {
                override suspend fun generateOutput(
                    ctx: SkillContext,
                    inputData: String
                ): SkillOutput {
                    return Confirm(name, number, SmsSkill.capitalize(inputData), yesNoData)
                }
            }
            return InteractionPlan.StartSubInteraction(
                reopenMicrophone = true,
                nextSkills = listOf(messageSkill),
            )
        }
    }

    /** Reads the message back and sends it only after a spoken "yes". */
    class Confirm(
        private val name: String,
        private val number: String,
        private val message: String,
        private val yesNoData: StandardRecognizerData<Sentences.UtilYesNo>,
    ) : SmsOutput {
        override fun getSpeechOutput(ctx: SkillContext): String =
            ctx.getString(R.string.skill_sms_confirm, name, message)

        override fun getInteractionPlan(ctx: SkillContext): InteractionPlan {
            val confirmSkill = object : RecognizeYesNoSkill(SmsInfo, yesNoData) {
                override suspend fun generateOutput(
                    ctx: SkillContext,
                    inputData: Boolean
                ): SkillOutput {
                    return if (!inputData) {
                        Done(ctx.getString(R.string.skill_sms_not_sent))
                    } else if (SmsSkill.send(ctx.android, number, message)) {
                        Done(ctx.getString(R.string.skill_sms_sent, name))
                    } else {
                        Done(ctx.getString(R.string.skill_sms_failed))
                    }
                }
            }
            return InteractionPlan.ReplaceSubInteraction(
                reopenMicrophone = true,
                nextSkills = listOf(confirmSkill),
            )
        }

        @Composable
        override fun GraphicalOutput(ctx: SkillContext) {
            Column {
                Headline(text = ctx.getString(R.string.skill_sms_confirm_title, name))
                Spacer(modifier = Modifier.height(4.dp))
                Body(text = number)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = message, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    class Done(
        private val speech: String,
    ) : SmsOutput, HeadlineSpeechSkillOutput {
        override fun getSpeechOutput(ctx: SkillContext): String = speech
    }
}

private class ChooserByIndex(
    private val contacts: List<Pair<String, String>>,
    private val onChosen: suspend (String, String) -> SkillOutput,
) : Skill<Int>(SmsInfo, Specificity.HIGH) {
    override fun score(ctx: SkillContext, input: String): Pair<Score, Int> {
        val index = ctx.parserFormatter!!
            .extractNumber(input)
            .preferOrdinal(true)
            .integerOnly(true)
            .parseFirstIfInteger() ?: 0
        return Pair(
            if (index <= 0 || index > contacts.size) AlwaysWorstScore else AlwaysBestScore,
            index.toInt()
        )
    }

    override suspend fun generateOutput(ctx: SkillContext, inputData: Int): SkillOutput {
        val contact = contacts[inputData - 1]
        return onChosen(contact.first, contact.second)
    }
}

// low specificity, to prefer the index-based chooser
private class ChooserByName(
    private val contacts: List<Pair<String, String>>,
    private val onChosen: suspend (String, String) -> SkillOutput,
) : Skill<Pair<String, String>?>(SmsInfo, Specificity.LOW) {
    override fun score(ctx: SkillContext, input: String): Pair<Score, Pair<String, String>?> {
        val trimmedInput = input.trim()
        val best = contacts
            .map { Pair(it, StringUtils.contactStringDistance(trimmedInput, it.first)) }
            .filter { it.second < -7 }
            .minByOrNull { it.second }
            ?.first
        return Pair(if (best == null) AlwaysWorstScore else AlwaysBestScore, best)
    }

    override suspend fun generateOutput(
        ctx: SkillContext,
        inputData: Pair<String, String>?
    ): SkillOutput {
        return inputData?.let { onChosen(it.first, it.second) }
            ?: SmsOutput.Done(ctx.getString(R.string.skill_sms_unknown_contact))
    }
}
