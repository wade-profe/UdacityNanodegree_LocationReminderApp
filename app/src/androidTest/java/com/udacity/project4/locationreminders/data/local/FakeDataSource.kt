package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var shouldReturnError: Boolean = false
    val remindersList: ArrayList<ReminderDTO> = arrayListOf()

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        return Result.Success(remindersList.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        try {
            if(shouldReturnError){
                throw Exception("Test exception")
            }
            var reminder: ReminderDTO? = null
            with(remindersList.filter { it.id == id }){
                if(size > 0){
                    reminder = this[0]
                }
            }
            if (reminder != null) {
                return Result.Success(reminder!!)
            } else {
                return Result.Error("Reminder not found!")
            }
        } catch (e: Exception) {
            return Result.Error(e.localizedMessage)
        }
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }

}