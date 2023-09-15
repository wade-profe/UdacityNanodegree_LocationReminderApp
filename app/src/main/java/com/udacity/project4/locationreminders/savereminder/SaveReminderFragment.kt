package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
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
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

const val GEOFENCE_RADIUS = 100F

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var reminderDataItem:ReminderDataItem

    private val runningQOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private var snackbarWasTapped: Boolean? = null

    lateinit var geofencingClient: GeofencingClient

    @RequiresApi(Build.VERSION_CODES.Q)
    val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (isGranted) {
                enableSaveButton(true)
            } else {
                enableSaveButton(false)
                raisePermissionDeniedSnackBar()
            }
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    val requestFineLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (isGranted) {
                if (!permissionCheck(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    raiseExplanationDialogue()
                } else {
                    enableSaveButton(true)
                }
            } else {
                enableSaveButton(false)
                raisePermissionDeniedSnackBar()
            }
        }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

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

            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription
            val selectLocation = _viewModel.selectedPOI

            reminderDataItem = ReminderDataItem(
                title,
                description.value,
                selectLocation.value?.name,
                selectLocation.value?.latlng?.latitude,
                selectLocation.value?.latlng?.longitude
            )

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                if (permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (permissionCheck(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        createGeofence()
                    } else {
                        raiseExplanationDialogue()
                    }
                } else {
                    requestFineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }


//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db


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
                snackbarWasTapped = true
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
                requestBackgroundLocationPermissionLauncher.launch(
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.decline
            ) { dialog, _ ->
                enableSaveButton(false)
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
    override fun permissionCheck(permission: String): Boolean {
        return if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION) {
            !runningQOrLater || ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()
        if(snackbarWasTapped == true){
            if(permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)){
                enableSaveButton(true)
            }
        }
    }

    @SuppressLint("VisibleForTests", "MissingPermission")
    private fun createGeofence(){
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GEOFENCE_RADIUS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofenceRequest = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // todo remove when finished
            .build()

        geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.validateAndSaveReminder(reminderDataItem)
            }
            addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.geofences_not_added), Toast.LENGTH_SHORT).show()
            }
        }

    }
}
