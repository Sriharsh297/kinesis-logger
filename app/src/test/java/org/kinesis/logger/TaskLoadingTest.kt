package org.kinesis.logger

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TaskLoadingTest {

    @Test
    fun testTasksJsonSchemaAndContents() {
        val tasksFile = File("src/main/assets/tasks.json")
        assertTrue("tasks.json must exist in assets", tasksFile.exists())

        val jsonString = tasksFile.readText()
        val jsonArray = JSONArray(jsonString)

        assertTrue("tasks.json must have tasks", jsonArray.length() >= 12)

        val taskIds = mutableSetOf<String>()
        val validFamilies = setOf("GENUINE_MOVEMENT", "BENIGN_NON_WALK", "FRAUD")

        for (i in 0 until jsonArray.length()) {
            val task = jsonArray.getJSONObject(i)
            val taskId = task.getString("task_id")
            val family = task.getString("label_family")
            val boutPlannedS = task.getInt("bout_planned_s")

            assertTrue("Duplicate task id: $taskId", taskIds.add(taskId))
            assertTrue("Invalid label family: $family", validFamilies.contains(family))
            assertTrue("Bout duration must be positive", boutPlannedS > 0)
            assertNotNull(task.getString("placement"))
            assertNotNull(task.getString("speed_label"))
        }

        // T01 must be genuine walking
        val t01 = jsonArray.getJSONObject(0)
        assertEquals("T01", t01.getString("task_id"))
        assertEquals("GENUINE_MOVEMENT", t01.getString("label_family"))
        assertEquals("G1", t01.getString("label_type"))
        assertEquals(1, t01.getInt("reward_eligible"))
        assertEquals(0, t01.getInt("label_binary_fraud"))
    }
}
