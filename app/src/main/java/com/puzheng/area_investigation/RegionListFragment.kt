package com.puzheng.area_investigation

import android.app.ProgressDialog
import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bignerdranch.android.multiselector.MultiSelector
import com.puzheng.area_investigation.databinding.FragmentAreaListBinding
import com.puzheng.area_investigation.model.Region
import com.puzheng.area_investigation.store.POITypeStore
import com.puzheng.area_investigation.store.RegionStore
import kotlinx.android.synthetic.main.fragment_area_list.*
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.successUi

/**
 * A fragment representing a list of Items.
 *
 *
 * Activities containing this fragment MUST implement the [OnAreaListFragmentInteractionListener]
 * interface.
 */
class RegionListFragment : Fragment(), OnPermissionGrantedListener {
    override fun onPermissionGranted(permission: String, requestCode: Int) {
        val pb = ProgressDialog.show(activity, "", "第一次启动，正在创建测试数据", false, false)
        poiTypeStore.fakePoiTypes().success {
            // must "get" task in main thread
            regionStore.fakeAreas().successUi {
                pb.dismiss()
                Toast.makeText(activity, "测试数据创建成功", Toast.LENGTH_SHORT).show()
                setupAreas()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setupAreas()
    }

    private var listener: OnAreaListFragmentInteractionListener? = null

    val multiSelector = MultiSelector()

    companion object {
        val REQUEST_WRITE_EXTERNAL_STORAGE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    lateinit private var binding: FragmentAreaListBinding
    lateinit private var regions: List<Region>


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_area_list,
                container, false)
        binding.args = Args(ObservableField(true), ObservableField(0))
        return binding.root
    }

    private val regionStore: RegionStore by lazy {
        RegionStore.with(activity)
    }

    private val poiTypeStore: POITypeStore by lazy {
        POITypeStore.with(activity)
    }


    fun setupAreas() {
        regionStore.list successUi {
            if (it != null && it.isNotEmpty()) {
                this@RegionListFragment.regions = it
                (binding.args as Args).itemNo.set(it.size)
                list.adapter = RegionRecyclerViewAdapter(it, listener!!, multiSelector)
                list.layoutManager = (list.adapter as RegionRecyclerViewAdapter).LayoutManager(activity, 2)
            } else if (BuildConfig.DEBUG) {
                activity.assertPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        REQUEST_WRITE_EXTERNAL_STORAGE).successUi {
                    onPermissionGranted(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            REQUEST_WRITE_EXTERNAL_STORAGE)
                }
            }
        } alwaysUi {
            (binding.args as Args).loading.set(false)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnAreaListFragmentInteractionListener) {
            listener = context as OnAreaListFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
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
    interface OnAreaListFragmentInteractionListener {
        fun onClickItem(region: Region)
        fun onLongClickItem(region: Region): Boolean
    }


    data class Args(val loading: ObservableField<Boolean>, val itemNo: ObservableField<Int>)

    fun removeSelectedAreas() {
        val adapter = (list.adapter as RegionRecyclerViewAdapter)
        val selectedAreas = adapter.selectedRegions
        adapter.removeSelectedAreas()
        RegionStore.with(activity).removeAreas(selectedAreas) successUi {
            Toast.makeText(activity, "区域已被删除!", Toast.LENGTH_SHORT).show()
        }
    }

}
