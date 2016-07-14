package com.puzheng.region_investigation

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_create_region_step1.*


class CreateRegionStep1Fragment : Fragment() {

    fun findTextViewById(id: Int) = view!!.findViewById(id) as TextView

    val extras: Map<String, String>
        get() = mutableMapOf<String, String>().apply {
            put("说明", findTextViewById(R.id.brief).text.toString())
            put("联系方式", findTextViewById(R.id.contact).text.toString())
            put("所属派出所", findTextViewById(R.id.policeStation).text.toString())
            put("派出所联系电话", findTextViewById(R.id.policeStationPhoneNumber).text.toString())
            put("所属居委会", findTextViewById(R.id.neighborhoodCommittee).text.toString())
            put("居委会联系电话", findTextViewById(R.id.neighborhoodCommitteePhoneNumber).text.toString())
            put("工作关系姓名", findTextViewById(R.id.contactPersonName).text.toString())
            put("工作关系电话", findTextViewById(R.id.contactPersonPhoneNumber).text.toString())
            put("工作关系说明", findTextViewById(R.id.contactPersonBrief).text.toString())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_create_region_step1, container, false)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        name.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mListener?.afterTextChanged(s, name)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        mListener?.afterTextChanged(name.editableText, name)


    }

    private var mListener: CreateRegionStep1Fragment.OnFragmentInteractionListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CreateRegionStep1Fragment.OnFragmentInteractionListener) {
            mListener = context as CreateRegionStep1Fragment.OnFragmentInteractionListener?
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        fun newInstance() = CreateRegionStep1Fragment()
    }


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun afterTextChanged(s: Editable?, editText: EditText)
    }
}
