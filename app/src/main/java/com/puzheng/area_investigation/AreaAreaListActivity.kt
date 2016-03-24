package com.puzheng.area_investigation

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.orhanobut.logger.Logger
import com.puzheng.area_investigation.model.Area
import kotlinx.android.synthetic.main.activity_area_list.*

class AreaAreaListActivity : AppCompatActivity(),
        AreaListFragment.OnAreaListFragmentInteractionListener {


    private var actionMode: ActionMode? = null

    override fun onLongClickItem(area: Area): Boolean {
        if (actionMode != null) {
            return false;
        }

        // Start the CAB using the ActionMode.Callback defined above
        actionMode = startSupportActionMode(object : ActionMode.Callback {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return true
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.context_menu_area_list, menu);
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                actionMode = null;
            }

        });
        return true;
    }

    override fun onClickItem(area: Area) {
        throw UnsupportedOperationException()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init()

        setContentView(R.layout.activity_area_list)
        setSupportActionBar(toolbar)

        fab.setOnClickListener({ view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show() })

        Logger.i(listOf("username: ${intent.getStringExtra("USERNAME")}",
                "org name: ${intent.getStringExtra("ORG_NAME")}",
                "org code: ${intent.getStringExtra("ORG_CODE")}").joinToString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_area_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
