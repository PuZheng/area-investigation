package com.puzheng.area_investigation

import android.content.Context
import android.inputmethodservice.Keyboard
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.fragment_create_area_step1.*


class CreateAreaStep1Fragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_create_area_step1, container, false)

        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        name.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                mListener?.afterTextChanged(s)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        mListener?.afterTextChanged(name.editableText)


    }

    private var mListener: CreateAreaStep1Fragment.OnFragmentInteractionListener? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CreateAreaStep1Fragment.OnFragmentInteractionListener) {
            mListener = context as CreateAreaStep1Fragment.OnFragmentInteractionListener?
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
        fun newInstance() = CreateAreaStep1Fragment()
    }


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun afterTextChanged(s: Editable?)
    }
}
