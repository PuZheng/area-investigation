package com.puzheng.area_investigation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.amap.api.maps2d.MapView
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

    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_create_area_step2, container, false)

//        val aMap = map.getMap();
//        aMap.setLocationSource(new LocationSource() {
//
//            @Override
//            public void activate(OnLocationChangedListener onLocationChangedListener) {
//                AMapFragment.this.onLocationChangedListener = onLocationChangedListener;
//                centerAt(((NearbyActivity) getActivity()).getLnglat());
//            }
//
//            @Override
//            public void deactivate() {
//                AMapFragment.this.onLocationChangedListener = null;
//            }
//        });
//        aMap.setMyLocationEnabled(true);

        return view;
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map.onCreate(savedInstanceState)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context as OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
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
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         * @return A new instance of fragment CreateAreaStep2Fragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance() = CreateAreaStep2Fragment()
    }
}// Required empty public constructor
