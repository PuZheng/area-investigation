package com.puzheng.area_investigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.LocationSource
import com.amap.api.maps2d.model.CameraPosition
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.fragment_create_area_step2.*


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CreateAreaStep2Fragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [CreateAreaStep2Fragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateAreaStep2Fragment : Fragment() {

    // TODO: Rename and change types of parameters

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_create_area_step2, container, false)
        return view;
    }


    private var onLocationChangeListener: LocationSource.OnLocationChangedListener? = null

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map.onCreate(savedInstanceState)
        map.map.setLocationSource(object: LocationSource {
            override fun deactivate() {
                onLocationChangeListener = null
            }

            override fun activate(p0: LocationSource.OnLocationChangedListener?) {
                // 这里将获取map默认的OnLocationChangedListener, map不能直接移动中心点，要通过操作这个对象来
                // 实现定位
                onLocationChangeListener = p0
            }
        })
        map.map.isMyLocationEnabled = true
        map.map.moveCamera(CameraUpdateFactory.zoomTo(INIT_ZOOM_LEVEL))
        map.map.setOnMapLongClickListener {
            listener?.onMapLongClick()
        }


//        val permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
        val permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            startLocation()
        } else {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // TODO Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERSISSION_TO_ACCESS_FINE_LOCATION)
            }
        }


    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERSISSION_TO_ACCESS_FINE_LOCATION ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocation()
                }
        }
    }

    private fun startLocation() {
        //初始化定位
        val locationClient = AMapLocationClient(activity)
        //设置定位回调监听
        locationClient.setLocationListener({
            if (it?.errorCode == 0) {
                Logger.e(it.toStr())
                onLocationChangeListener?.onLocationChanged(it)
            } else {
                Logger.e("定位失败, ${it.errorCode}: ${it.errorInfo}");
            }
        })
        //初始化定位参数
        val locationOption = AMapLocationClientOption()
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        locationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy;
        locationOption.isOnceLocation = true // 只需要定位一次
        locationClient.setLocationOption(locationOption);
        //启动定位
        locationClient.startLocation();
    }


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onMapLongClick()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment CreateAreaStep2Fragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance() = CreateAreaStep2Fragment()
        private val PERSISSION_TO_ACCESS_FINE_LOCATION: Int = 100
        private val INIT_ZOOM_LEVEL: Float = 16.0F

    }
}
