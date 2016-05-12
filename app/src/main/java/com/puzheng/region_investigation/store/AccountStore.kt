package com.puzheng.region_investigation.store

import android.content.Context
import android.content.SharedPreferences
import com.puzheng.region_investigation.R
import com.puzheng.region_investigation.model.Account

class AccountStore private constructor(val context: Context) {
    companion object {
        fun with(context: Context) = AccountStore(context)
        private const val ORG_CODE = "ORG_CODE"
        private const val USERNAME = "USERNAME"
        private const val ORG_NAME = "ORG_NAME"
    }

    private val sharedPref: SharedPreferences by lazy {
        context.getSharedPreferences(context.getString(R.string.preference_account),
                Context.MODE_PRIVATE)
    }

    var account: Account?
        get() = if (sharedPref.contains(USERNAME)) {
            Account(sharedPref.getString(USERNAME, null),
                    sharedPref.getString(ORG_CODE, null),
                    sharedPref.getString(ORG_NAME, null))
        } else {
            null
        }
        set(value) {
            sharedPref.edit().apply {
                putString(USERNAME, value?.username)
                putString(ORG_CODE, value?.orgCode)
                putString(ORG_NAME, value?.orgName)
                commit()
            }
        }

}