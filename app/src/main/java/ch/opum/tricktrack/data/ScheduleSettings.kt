package ch.opum.tricktrack.data

import java.time.DayOfWeek

data class ScheduleSettings(
    val target: ScheduleTarget = ScheduleTarget.AUTOMATIC,
    val isCustomizeIndividualDays: Boolean = false,
    val globalStartHour: Int = 8,
    val globalStartMinute: Int = 0,
    val globalEndHour: Int = 17,
    val globalEndMinute: Int = 0,
    val dailySchedules: Map<DayOfWeek, DaySchedule> = emptyMap()
)

data class DaySchedule(
    val isEnabled: Boolean,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
)
