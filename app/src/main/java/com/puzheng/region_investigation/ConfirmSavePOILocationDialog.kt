package com.puzheng.region_investigation

import android.app.Dialog
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import com.amap.api.maps.model.LatLng
import com.puzheng.region_investigation.model.POI
import com.puzheng.region_investigation.store.POIStore
import com.puzheng.region_investigation.store.RegionStore
import nl.komponents.kovenant.combine.and
import nl.komponents.kovenant.ui.successUi

class ConfirmSavePOILocationDialog(val poi: POI, val position: LatLng, val afterConfirmed: () -> Unit): AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity).setTitle(R.string.confirm_save_poi_location)
                .setPositiveButton(R.string.confirm, {
                    dialog, which ->
                    POIStore.with(context).update(poi, mapOf(POI.Model.COL_LAT_LNG to position)) and
                            RegionStore.with(context).touch(poi.regionId) successUi {
                        afterConfirmed()
                    }
                }).setNegativeButton(R.string.cancel, null).create()
    }
}