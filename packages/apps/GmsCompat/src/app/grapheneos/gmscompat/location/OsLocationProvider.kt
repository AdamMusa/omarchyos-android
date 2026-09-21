package app.grapheneos.gmscompat.location

import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import com.android.server.location.fudger.LocationFudger
import com.google.android.gms.location.LocationRequest

class OsLocationProvider(val name: String, val properties: ProviderProperties?, val fudger: LocationFudger?, val permission: Permission) {

    fun maybeFudge(location: Location): Location {
        if (fudger == null) {
            return location
        }

        return fudger.createCoarse(location)
    }

    companion object {
        fun get(client: Client, req: LocationRequest) = get(client, req.priority, req.granularity)

        fun get(client: Client, priority: Int, granularity: Int): OsLocationProvider {
            val name = if (priority == LocationRequest.PRIORITY_NO_POWER) {
                LocationManager.PASSIVE_PROVIDER
            } else if (priority == LocationRequest.PRIORITY_LOW_POWER) {
                if (client.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    // GmsCore location service doesn't use GNSS for low-power requests
                    LocationManager.NETWORK_PROVIDER
                } else {
                    // fused provider doesn't use GNSS for low-power requests, i.e. it doesn't
                    // provide any locations for low-power requests when network location is off
                    LocationManager.GPS_PROVIDER
                }
            } else {
                LocationManager.FUSED_PROVIDER
            }
            return get(client, name, granularity)
        }

        fun getPassive(client: Client, granularity: Int): OsLocationProvider {
            return get(client, LocationManager.PASSIVE_PROVIDER, granularity)
        }

        fun get(client: Client, name: String, granularity: Int): OsLocationProvider {
            val properties: ProviderProperties? = client.locationManager.getProviderProperties(name)

            var permission = client.permission

            val fudger: LocationFudger? = when (client.permission) {
                Permission.COARSE -> {
                    when (granularity) {
                        LocationRequest.GRANULARITY_COARSE,
                        LocationRequest.GRANULARITY_PERMISSION_LEVEL, -> null
                        LocationRequest.GRANULARITY_FINE -> throw SecurityException()
                        else -> throw IllegalArgumentException()
                    }
                }
                Permission.FINE -> {
                    when (granularity) {
                        LocationRequest.GRANULARITY_COARSE -> {
                            permission = Permission.COARSE
                            if (properties == null) {
                                createLocationFudger()
                            } else when (properties.accuracy) {
                                ProviderProperties.ACCURACY_FINE -> createLocationFudger()
                                ProviderProperties.ACCURACY_COARSE -> null
                                else -> throw IllegalStateException()
                            }
                        }
                        LocationRequest.GRANULARITY_PERMISSION_LEVEL,
                        LocationRequest.GRANULARITY_FINE ->
                            null
                        else -> throw IllegalArgumentException()
                    }
                }
            }

            return OsLocationProvider(name, properties, fudger, permission)
        }

        private fun createLocationFudger(): LocationFudger {
            /** @see com.android.server.location.injector.SystemSettingsHelper */
            val DEFAULT_COARSE_LOCATION_ACCURACY_M = 2000.0f
            return LocationFudger(DEFAULT_COARSE_LOCATION_ACCURACY_M)
        }
    }
}
