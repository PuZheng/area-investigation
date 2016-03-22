package com.puzheng.area_investigation

import android.content.Context
import android.databinding.DataBindingUtil
import android.databinding.ObservableField
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.puzheng.area_investigation.databinding.FragmentAreaListBinding
import com.puzheng.area_investigation.model.Area
import com.puzheng.area_investigation.store.areas
import kotlinx.android.synthetic.main.fragment_area_list.*
import rx.Observer
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * A fragment representing a list of Items.
 *
 *
 * Activities containing this fragment MUST implement the [OnListFragmentInteractionListener]
 * interface.
 */
class AreaListFragment : Fragment() {
    private var mListener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding: FragmentAreaListBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_area_list,
                container, false)
        binding.args = Args(ObservableField(true), ObservableField(0))


        areas.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(object : Observer<List<Area>> {

            override fun onError(e: Throwable?) {
            }

            override fun onCompleted() {
                (binding.args as Args).loading.set(false)
            }

            override fun onNext(areas: List<Area>?) {
                if (areas != null) {
                    (binding.args as Args).itemNo.set(areas.size)
                    list.adapter = AreaRecyclerViewAdapter(areas, mListener)
                    list.layoutManager = (list.adapter as AreaRecyclerViewAdapter).LayoutManager(activity, 2)
                }
            }

        })

        return binding.root
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context as OnListFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
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
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(area: Area)
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
