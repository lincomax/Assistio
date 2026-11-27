package org.stypox.dicio.skills.reminder

import android.content.ContentProviderOperation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Talks to Tasks.org through its content provider API (Tasks.org 15.12 and later).
 * See https://github.com/tasks/tasks/blob/main/CONTENT_PROVIDER.md
 */
object TasksOrg {
    private val TAG = TasksOrg::class.simpleName

    const val AUTHORITY = "org.tasks.api"
    const val PERMISSION_WRITE = "org.tasks.permission.WRITE_TASKS"
    private val TASKS_URI: Uri = Uri.parse("content://$AUTHORITY/v0/tasks")
    private val REMINDERS_URI: Uri = Uri.parse("content://$AUTHORITY/v0/reminders")

    sealed interface Result {
        data class Added(val taskUri: Uri) : Result
        data object NotInstalled : Result
        data object Failed : Result
    }

    fun isInstalled(context: Context): Boolean =
        context.packageManager.resolveContentProvider(AUTHORITY, 0) != null

    /**
     * Creates a task in the user's default Tasks.org list. If [due] is not null, the task gets
     * that due date and time, plus a reminder that fires when it is due.
     */
    fun addTask(context: Context, title: String, due: LocalDateTime?): Result {
        if (!isInstalled(context)) {
            return Result.NotInstalled
        }

        val task = ContentProviderOperation.newInsert(TASKS_URI)
            .withValue("title", title)
        if (due != null) {
            val dueMillis = due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            task.withValue("due_date", dueMillis)
                .withValue("due_all_day", 0)
        }

        val operations = arrayListOf(task.build())
        if (due != null) {
            operations.add(
                ContentProviderOperation.newInsert(REMINDERS_URI)
                    .withValueBackReference("task_id", 0)
                    .withValue("type", "relative_due")
                    .withValue("offset_ms", 0L)
                    .build()
            )
        }

        return try {
            val results = context.contentResolver.applyBatch(AUTHORITY, operations)
            val taskUri = results.firstOrNull()?.uri
            if (taskUri == null) Result.Failed else Result.Added(taskUri)
        } catch (e: Exception) {
            Log.e(TAG, "Could not add task to Tasks.org", e)
            Result.Failed
        }
    }

    fun openTask(context: Context, taskUri: Uri) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, taskUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not open task in Tasks.org", e)
        }
    }
}
