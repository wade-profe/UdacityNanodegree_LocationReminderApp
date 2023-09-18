package com.udacity.project4.locationreminders.reminderslist

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by viewModel()
    private lateinit var binding: FragmentRemindersBinding

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isEnabled ->
            if(isEnabled){
                navigateToAddReminder()
            } else{
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    raiseExplanationDialogue()
                } else {
                    _viewModel.notificationEnabled.value = false
                    navigateToAddReminder()
                }
            }
        }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("FragmentLiveDataObserve")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener {
            _viewModel.loadReminders()
        }

        _viewModel.showLoading.observe(this) {
            binding.refreshLayout.isRefreshing = it
        }

        _viewModel.notificationEnabled.observe(viewLifecycleOwner){
            when(it){
                false -> raisePermissionDeniedSnackBar(getString(R.string.notification_permission_denied))
                else -> {}
            }
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            if (!permissionCheck(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.d("WADE", "Requesting permission")
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                navigateToAddReminder()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {

        }

//        setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                AuthUI.getInstance().signOut(requireContext())
                val intent = Intent(requireContext(), AuthenticationActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
        return super.onOptionsItemSelected(item)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun raiseExplanationDialogue() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.permission_required))
            .setMessage(R.string.notification_permission_rationale)
            .setPositiveButton(
                getString(R.string.accept)
            ) { dialog, _ ->
                requestNotificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.decline
            ) { dialog, _ ->
                _viewModel.notificationEnabled.value = false
                dialog.dismiss()
            }
            .show()
    }

}
