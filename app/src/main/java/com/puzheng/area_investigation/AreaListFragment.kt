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
import com.puzheng.area_investigation.model.Area
import com.puzheng.area_investigation.store.AreaStore
import kotlinx.android.synthetic.main.fragment_area_list.*
import rx.Observer
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * A fragment representing a list of Items.
 *
 *
 * Activities containing this fragment MUST implement the [OnAreaListFragmentInteractionListener]
 * interface.
 */
class AreaListFragment : Fragment() {
    private var listener: OnAreaListFragmentInteractionListener? = null

    val multiSelector = MultiSelector()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    lateinit private var binding: FragmentAreaListBinding

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_area_list,
                container, false)
        binding.args = Args(ObservableField(true), ObservableField(0))
        fetchAreas()
        return binding.root
    }

    fun update() = fetchAreas()

    private fun fetchAreas() {
        val store = AreaStore.with(activity)
        store.areas.observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<List<Area>> {

            override fun onError(e: Throwable?) {
                throw e!!
            }

            override fun onCompleted() {
                (binding.args as Args).loading.set(false)
            }

            override fun onNext(areas: List<Area>?) {
                if (areas != null && areas.isNotEmpty()) {
                    (binding.args as Args).itemNo.set(areas.size)
                    list.adapter = AreaRecyclerViewAdapter(areas, listener!!, multiSelector)
                    list.layoutManager = (list.adapter as AreaRecyclerViewAdapter).LayoutManager(activity, 2)
                } else if (BuildConfig.DEBUG) {
                    var pb: ProgressDialog? = null
                    store.fakeAreas().observeOn(AndroidSchedulers.mainThread()).doOnSubscribe({
                        pb = ProgressDialog.show(activity, "", "第一次启动，正在创建测试数据", false, false)
                    }).doOnCompleted {
                        pb?.dismiss()
                        Toast.makeText(activity, "测试数据创建成功", Toast.LENGTH_SHORT).show()
                    }.subscribe(object : Observer<Void> {

                        override fun onError(e: Throwable?) {
                            throw e!!
                        }

                        override fun onNext(t: Void?) {

                        }

                        override fun onCompleted() {
                            fetchAreas()
                        }
                    })
                }
            }

        })
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
        fun onClickItem(area: Area)
        fun onLongClickItem(area: Area): Boolean
    }

    companion object {

        // TODO: Customize parameter argument names
        private val ARG_COLUMN_COUNT = "column-count"

        // TODO: Customize parameter initialization
        @SuppressWarnings("unused")
        fun newInstance(columnCount: Int): AreaListFragment {
            val fragment = AreaListFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }

    data class Args(val loading: ObservableField<Boolean>, val itemNo: ObservableField<Int>)
}
