package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.koin.test.inject
import org.robolectric.annotation.Config
import kotlin.test.assertNull
import com.udacity.project4.R


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class SaveReminderViewModelTest : AutoCloseKoinTest() {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: FakeDataSource
    private lateinit var appContext: Application
    val viewModel: SaveReminderViewModel by inject()

    @Before
    fun init() {
        stopKoin()
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module {
            viewModel {
                SaveReminderViewModel(
                    appContext,
                    get() as FakeDataSource
                )
            }
            single { FakeDataSource() }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        repository = get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun onClear_ReminderValuesNotNull_SetToNull() {
        // Given - Reminder values are not null
        viewModel.reminderTitle.value = "Test title"
        viewModel.reminderDescription.value = "Test description"
        viewModel.selectedPOI.value = SelectedLocation("Test location", LatLng(123.123, 456.456))
        // When - onClear is called
        viewModel.onClear()
        // Then - Reminder values are null
        assertNull(viewModel.reminderTitle.value)
        assertNull(viewModel.reminderDescription.value)
        assertNull(viewModel.selectedPOI.value)
    }

    @Test
    fun validateEnteredData_withNullTitle_UpdatesSnackbarAndReturnsFalse() {
        // Given - ReminderDataItem with null title
        val reminderDataItem = ReminderDataItem(
            null,
            "Test description",
            "Test location",
            123.123,
            456.456
        )
        // When - validateEnteredData is called
        val result = viewModel.validateEnteredData(reminderDataItem)
        // Then - showSnackBarInt value is set to "Please enter title"
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
        //Then - method returns false
        assertThat(result, `is`(false))
    }

    @Test
    fun validateEnteredData_withNullLocation_UpdatesSnackbarAndReturnsFalse() {
        // Given - ReminderDataItem with null title
        val reminderDataItem = ReminderDataItem(
            "Test title",
            "Test description",
            null,
            123.123,
            456.456
        )
        // When - validateEnteredData is called
        val result = viewModel.validateEnteredData(reminderDataItem)
        // Then - showSnackBarInt value is set to "Please enter title"
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
        //Then - method returns false
        assertThat(result, `is`(false))
    }



    /**
     * Given - ReminderDataItem with valid values
     * When - validateEnteredData is called
     * Then - method returns true
     *
     * (Requires coroutine) Given - the SaveReminder function is called
     * When - the method has started
     * Then - showLoading is set to true
     * When - the method has finished
     * Then - showLoading is set to false
     *
     * Given - a valid ReminderDataItem
     * When - saveReminder is called
     * Then - showToast value is set to R.string.reminderSaved
     *
     * Given - a valid ReminderDataItem
     * When - saveReminder is called
     * Then - navigationCommand value is set to NavigationCommand.Back
     *
     * Given - a valid ReminderDataItem
     * When - Save Reminder is called
     * Then - reminder is saved in repository
     *
     * Not sure if below is necessary:
     * Given - a reminderDataItem with missing values
     * When - validateAndSaveReminder is called
     * Then - reminder is not saved
     *
     * Given - a reminderDataItem with valid values
     * When - validateAndSaveReminder is called
     * Then - reminder is saved
     */


}