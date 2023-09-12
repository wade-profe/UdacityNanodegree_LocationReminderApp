package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    override val _viewModel: SaveReminderViewModel by inject()

    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var map: GoogleMap

    private lateinit var myLocation: FusedLocationProviderClient

    val zoomLevel = 15f

    private var permissionStateHolder: Boolean? = null

    private var selectedLatLng: LatLng? = null

    private var selectedName: String? = null

    @SuppressLint("NewApi")
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        )
        { isGranted ->
            if (isGranted) {
                enableLocation()
                Toast.makeText(requireActivity(), R.string.select_poi, Toast.LENGTH_LONG).show()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle(getString(R.string.permission_required))
                        .setMessage(R.string.permission_denied_explanation)
                        .setPositiveButton(
                            getString(R.string.accept)
                        ) { dialog, _ ->
                            enableLocation()
                            dialog.dismiss()
                        }
                        .setNegativeButton(
                            R.string.decline
                        ) { dialog, _ ->
                            raisePermissionDeniedSnackBar()
                            dialog.dismiss()
                        }
                        .show()

                } else {
                    raisePermissionDeniedSnackBar()
                }
            }

        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        myLocation = LocationServices.getFusedLocationProviderClient(requireContext())

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected


//        TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setPoiClick(map)
        setMapLongClick(map)
        enableLocation()
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }

        R.id.hybrid_map -> {
            true
        }

        R.id.satellite_map -> {
            true
        }

        R.id.terrain_map -> {
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun permissionCheck(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (permissionCheck()) {
            enableSaveButton(true)
            map.isMyLocationEnabled = true
            var currentLocation: LatLng?
            myLocation.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel))
                }
            }
        } else {
            enableSaveButton(false)
            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun raisePermissionDeniedSnackBar() {
        Snackbar.make(
            binding.root,
            R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                permissionStateHolder = permissionCheck()
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data =
                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    override fun onResume() {
        super.onResume()
        permissionStateHolder?.let {
            if (permissionCheck()) {
                enableLocation()
            } else {
                raisePermissionDeniedSnackBar()
            }
        }
    }

    private fun enableSaveButton(permissionGranted: Boolean){
        if(_viewModel.fineLocationPermissionGranted.value != permissionGranted){
            _viewModel.fineLocationPermissionGranted.value = permissionGranted
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            map.clear()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .snippet(getString(R.string.poi_snippet))
            )
            poiMarker?.showInfoWindow()
            selectedLatLng = poi.latLng
            selectedName = poi.name
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(getString(R.string.poi_snippet))
            ).apply{
                this?.showInfoWindow()
            }
            selectedLatLng = latLng
            selectedName = getString(R.string.dropped_pin)
        }
    }
}
