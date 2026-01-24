package ch.opum.tricktrack.data

import java.time.DayOfWeek

data class ScheduleSettings(
    val target: ScheduleTarget,
    val dailySchedules: Map<DayOfWeek, DaySchedule>
)

data class DaySchedule(
    val isEnabled: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)
