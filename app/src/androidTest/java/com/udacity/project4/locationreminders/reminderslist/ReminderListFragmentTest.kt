package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config
import atPosition

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class ReminderListFragmentTest: AutoCloseKoinTest() {

    // Save reminders in repo, check they are loaded

    // No reminders in repo, check the no data image is displayed

    // Check navigation when fab is pressed

    // Set repo to return error, check that snackbar is displayed

    private lateinit var repository: FakeDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
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
    fun remindersInRepository_displayedInList() = runTest {
        // Given - Reminders saved in Repo
        val reminderOne = ReminderDTO("Test title", "Test description",
            "Test location", 123.123, 456.456)
        val reminderTwo = ReminderDTO("Test title 2", "Test description 2",
            "Test location 2", 124.124, 457.457)
        val reminderThree = ReminderDTO("Test title 3", "Test description 3",
            "Test location 3", 125.125, 458.458)
        repository.saveReminder(reminderOne)
        repository.saveReminder(reminderTwo)
        repository.saveReminder(reminderThree)
        // When - Reminders fragment is launched
        launchFragmentInContainer<ReminderListFragment>(themeResId =  R.style.AppTheme)
        // Then - reminders are displayed on the screen
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(0, hasDescendant(
            withText("Test title")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(0, hasDescendant(
            withText("Test description")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(0, hasDescendant(
            withText("Test location")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(1, hasDescendant(
            withText("Test title 2")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(1, hasDescendant(
            withText("Test description 2")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(1, hasDescendant(
            withText("Test location 2")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(2, hasDescendant(
            withText("Test title 3")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(2, hasDescendant(
            withText("Test description 3")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(2, hasDescendant(
            withText("Test location 3")))))
    }

    @Test
    fun noReminders_displaysNoDataView(){
        // Given - no reminders in the repository
        // When - Reminders fragment is launched
        launchFragmentInContainer<ReminderListFragment>(themeResId =  R.style.AppTheme)
        // Then - the no data text view is displayed
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun fabPressed_navigatesToSaveReminder(){
        // Given - Reminders fragment is launched
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // When - The fab is pressed
        onView(withId(R.id.addReminderFAB)).perform(click())
        // Then - navigates to save
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun repositoryReturnsError_snackbarIsDisplayed(){
        // Given - the repository returns an error
        repository.shouldReturnError = true
        // When - the Reminders fragment is launched
        launchFragmentInContainer<ReminderListFragment>(themeResId =  R.style.AppTheme)
        // A snackbar is displayed
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Test exception")))
    }





//    TODO: test the navigation of the fragments.
//    TODO: test the displayed data on the UI.
//    TODO: add testing for the error messages.

    // Reminder: switch off device animations
}