package org.stypox.dicio.skills.sms

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import org.dicio.skill.context.SkillContext
import org.dicio.skill.skill.SkillInfo
import org.dicio.skill.skill.SkillOutput
import org.dicio.skill.standard.StandardRecognizerData
import org.dicio.skill.standard.StandardRecognizerSkill
import org.stypox.dicio.sentences.Sentences
import org.stypox.dicio.sentences.Sentences.Sms
import org.stypox.dicio.skills.telephone.Contact

class SmsSkill(
    correspondingSkillInfo: SkillInfo,
    data: StandardRecognizerData<Sms>,
    private val yesNoData: StandardRecognizerData<Sentences.UtilYesNo>,
) : StandardRecognizerSkill<Sms>(correspondingSkillInfo, data) {

    override suspend fun generateOutput(ctx: SkillContext, inputData: Sms): SkillOutput {
        val (userContactName, message) = when (inputData) {
            is Sms.Send -> Pair(
                inputData.who?.trim() ?: "",
                inputData.message?.trim()?.takeIf { it.isNotEmpty() },
            )
        }

        val onChosen: suspend (String, String) -> SkillOutput = { name, number ->
            recipientChosen(name, number, message, yesNoData)
        }

        val contentResolver = ctx.android.contentResolver
        val contacts = Contact.getFilteredSortedContacts(contentResolver, userContactName)
        val validContacts = ArrayList<Pair<String, List<String>>>()

        var i = 0
        while (validContacts.size < 5 && i < contacts.size) {
            val contact = contacts[i]
            val numbers = contact.getNumbers(contentResolver)
            if (numbers.isEmpty()) {
                ++i
                continue
            }
            if (validContacts.isEmpty()
                && contact.distance < 3
                && numbers.size == 1
                && (contacts.size <= i + 1
                        || contacts[i + 1].distance - 2 > contact.distance)
            ) {
                // very close match with just one number and no ties: use it directly
                return onChosen(contact.name, numbers[0])
            }
            validContacts.add(Pair(contact.name, numbers))
            ++i
        }

        if (validContacts.size == 1
            && (validContacts[0].second.size == 1 || ctx.parserFormatter == null)
        ) {
            val contact = validContacts[0]
            return onChosen(contact.first, contact.second[0])
        }

        return SmsOutput.ChooseContact(validContacts, onChosen)
    }

    companion object {
        private val TAG = SmsSkill::class.simpleName

        fun recipientChosen(
            name: String,
            number: String,
            message: String?,
            yesNoData: StandardRecognizerData<Sentences.UtilYesNo>,
        ): SkillOutput {
            return if (message == null) {
                SmsOutput.AskMessage(name, number, yesNoData)
            } else {
                SmsOutput.Confirm(name, number, capitalize(message), yesNoData)
            }
        }

        fun capitalize(message: String): String =
            message.trim().replaceFirstChar { it.uppercaseChar() }

        /**
         * Sends the SMS through the system. Since Android 4.4 the system itself saves messages sent
         * by apps that are not the default SMS app, so they show up in the messaging app.
         * @return true if the message was handed to the system successfully
         */
        @SuppressLint("MissingPermission") // checked by Dicio before the skill runs
        fun send(context: Context, number: String, message: String): Boolean {
            return try {
                var smsManager: SmsManager? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = context.getSystemService(SmsManager::class.java)
                }
                if (smsManager == null) {
                    @Suppress("DEPRECATION")
                    smsManager = SmsManager.getDefault()
                }

                val parts = smsManager!!.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(number, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(number, null, message, null, null)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Could not send SMS", e)
                false
            }
        }
    }
}
