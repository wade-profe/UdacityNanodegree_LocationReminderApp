package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding

    private val runningQOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private var permissionStateHolder: Boolean? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (isGranted) {
                enableSaving()
            } else {
                raisePermissionDeniedSnackBar()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {

            // todo wrap the code in a method and then do a permission check, request permission if necessary and on success re-run the aforementioned method
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription
            val selectLocation = _viewModel.selectedPOI

            val reminderDataItem = ReminderDataItem(
                title,
                description.value,
                selectLocation.value?.name,
                selectLocation.value?.latlng?.latitude,
                selectLocation.value?.latlng?.longitude
            )

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db

            _viewModel.validateAndSaveReminder(reminderDataItem)

        }

        enableSaving()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun enableSaving() {
        if (permissionCheck(null)) {
            enableSaveButton(true)
        } else {
            enableSaveButton(false)
            raiseExplanationDialogue()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun raisePermissionDeniedSnackBar() {
        Snackbar.make(
            binding.root,
            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                permissionStateHolder = permissionCheck(null)
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data =
                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun raiseExplanationDialogue() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.permission_required))
            .setMessage(R.string.background_permission_rationale)
            .setPositiveButton(
                getString(R.string.accept)
            ) { dialog, _ ->
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.decline
            ) { dialog, _ ->
                raisePermissionDeniedSnackBar()
                dialog.dismiss()
            }
            .show()
    }

    private fun enableSaveButton(permissionGranted: Boolean) {
        if (_viewModel.backgroundLocationEnabled.value != permissionGranted) {
            _viewModel.backgroundLocationEnabled.value = permissionGranted
        }
    }


    // Returns true if background location is not required
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun permissionCheck(permission: String?): Boolean {
        return !runningQOrLater || ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        permissionStateHolder?.let {
            if (permissionCheck(null)) {
                enableSaveButton(true)
            } else {
                raisePermissionDeniedSnackBar()
            }
        }
    }
}
