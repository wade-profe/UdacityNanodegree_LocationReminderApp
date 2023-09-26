package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @Test
    fun insertReminderAndGetById() = runTest {
        // Given - a reminder is inserted into the db
        val reminder = ReminderDTO(
            "Test title", "Test description",
            "Test location", 123.123, 456.456
        )
        localDataSource.saveReminder(reminder)
        // When - the reminder is loaded by id
        val loaded = localDataSource.getReminder(reminder.id)
        // Then - the returned reminder contains the expected values
        assertThat(loaded as Result.Success, notNullValue())
        assertThat(loaded.data.id, `is`(reminder.id))
        assertThat(loaded.data.title, `is`(reminder.title))
        assertThat(loaded.data.description, `is`(reminder.description))
        assertThat(loaded.data.location, `is`(reminder.location))
        assertThat(loaded.data.latitude, `is`(reminder.latitude))
        assertThat(loaded.data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminderById_idDoesNotExist() = runTest {
        // Given - no reminder with id = 1 exists in the db
        // When - the getReminder method is called with the non-existent id
        val loaded = localDataSource.getReminder("1")
        // Then - an error is returned
        assertThat(loaded as Result.Error, notNullValue())
        assertThat(loaded.message, `is`("Reminder not found!"))
    }

    @Test
    fun saveRemindersAndGetAll() = runTest {
        // Given - Reminders exist in the db
        val reminderOne = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        localDataSource.saveReminder(reminderOne)
        localDataSource.saveReminder(reminderTwo)
        localDataSource.saveReminder(reminderThree)
        // When - getReminders is called
        val retrievedList = localDataSource.getReminders()
        // Then - all existing reminders are returned
        assertThat(retrievedList as Result.Success, notNullValue())
        assertThat(retrievedList.data.size, `is`(3))
        assertThat(retrievedList.data.contains(reminderOne), `is`(true))
        assertThat(retrievedList.data.contains(reminderTwo), `is`(true))
        assertThat(retrievedList.data.contains(reminderThree), `is`(true))
    }

    @Test
    fun deleteAllReminders() = runTest {
        // Given - Reminders exist in the db
        val reminderOne = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        localDataSource.saveReminder(reminderOne)
        localDataSource.saveReminder(reminderTwo)
        localDataSource.saveReminder(reminderThree)
        // When - the deleteAllReminders method is called
        localDataSource.deleteAllReminders()
        // Then - all reminders are removed from the db
        val loaded = localDataSource.getReminders()
        assertThat((loaded as Result.Success).data.size, `is`(0))
    }

    @After
    fun cleanUp() {
        database.close()
    }
}