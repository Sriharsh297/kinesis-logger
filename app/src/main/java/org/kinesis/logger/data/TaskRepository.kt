package org.kinesis.logger.data

import android.content.Context
import org.json.JSONArray
import org.kinesis.logger.model.Task
import java.io.BufferedReader
import java.io.InputStreamReader

class TaskRepository(private val context: Context) {

    private var cachedTasks: List<Task>? = null

    fun getTasks(): List<Task> {
        cachedTasks?.let { return it }

        val tasks = mutableListOf<Task>()
        try {
            val inputStream = context.assets.open("tasks.json")
            val jsonString = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                tasks.add(
                    Task(
                        taskId = obj.getString("task_id"),
                        title = obj.getString("title"),
                        labelFamily = obj.getString("label_family"),
                        labelType = obj.getString("label_type"),
                        labelBinaryFraud = obj.getInt("label_binary_fraud"),
                        rewardEligible = obj.getInt("reward_eligible"),
                        speedLabel = obj.getString("speed_label"),
                        placement = obj.getString("placement"),
                        orientationInstruction = obj.getString("orientation_instruction"),
                        surface = obj.getString("surface"),
                        environment = obj.getString("environment"),
                        boutPlannedS = obj.getInt("bout_planned_s"),
                        boutType = obj.getString("bout_type"),
                        attackMode = obj.getString("attack_mode"),
                        targetSteps = if (obj.has("target_steps") && !obj.isNull("target_steps")) obj.getInt("target_steps") else null,
                        description = obj.getString("description")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedTasks = tasks
        return tasks
    }
}
