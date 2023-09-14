package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import com.udacity.project4.locationreminders.savereminder.SelectedLocation
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

    private var selectedLocation: SelectedLocation? = null

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

        binding.save.setOnClickListener {
            onLocationSelected()
        }

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle(map)
        setPoiClick(map)
        setMapLongClick(map)
        enableLocation()
    }

    private fun onLocationSelected() {
        selectedLocation?.let {
            _viewModel.selectedPOI.value = selectedLocation
        }
        parentFragmentManager.popBackStack()
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)!!) {
            map.isMyLocationEnabled = true
            var currentLocation: LatLng?
            myLocation.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoomLevel))
                }
            }
        } else {
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
                permissionStateHolder = permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)
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
            if (permissionCheck(Manifest.permission.ACCESS_FINE_LOCATION)!!) {
                enableLocation()
            } else {
                raisePermissionDeniedSnackBar()
            }
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
            selectedLocation = SelectedLocation(poi.name, poi.latLng)
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
            ).apply {
                this?.showInfoWindow()
            }
            selectedLocation = SelectedLocation(getString(R.string.dropped_pin), latLng)
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
        } catch (e: RuntimeRemoteException) {
            Toast.makeText(requireContext(), getString(R.string.error_happened), Toast.LENGTH_SHORT)
                .show()
        }
    }
}
