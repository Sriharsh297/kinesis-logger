package org.kinesis.logger.model

data class Task(
    val taskId: String,
    val title: String,
    val labelFamily: String,
    val labelType: String,
    val labelBinaryFraud: Int,
    val rewardEligible: Int,
    val speedLabel: String,
    val placement: String,
    val orientationInstruction: String,
    val surface: String,
    val environment: String,
    val boutPlannedS: Int,
    val boutType: String,
    val attackMode: String,
    val targetSteps: Int? = null,
    val description: String
)
