package com.puzheng.region_investigation

import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import android.widget.TextView
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.model.POI
import com.squareup.picasso.Picasso
import java.io.File

class CarouselActivity : AppCompatActivity() {

    companion object {
        const val TAG_IMAGES = "CarouselActivity.POI"
        const val TAG_POS = "CarouselActivity.IMAGE"
    }


    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var sectionsPagerAdapter: SectionsPagerAdapter? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    lateinit private var viewPager: ViewPager

    lateinit private var images: List<String>
    private var pos: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.init("CarouselActivity")
        setContentView(R.layout.activity_carousel)

        setSupportActionBar(findView(R.id.toolbar))
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        pos = intent.getIntExtra(TAG_POS, 0)
        images = intent.getStringArrayListExtra(TAG_IMAGES)
        Logger.v(images.toString())

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        // Set up the ViewPager with the sections adapter.
        viewPager = findView(R.id.container)
        viewPager.adapter = sectionsPagerAdapter
        viewPager.setCurrentItem(pos)



        findView<FloatingActionButton>(R.id.fab).setOnClickListener {
            // TODO delete
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    private class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?) =
                inflater!!.inflate(R.layout.fragment_carousel, container, false).apply {
                }

        override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
            picasso.load(File(arguments.getString(ARG_IMAGE_PATH))).into(view?.findView<ImageView>(R.id.imageView))
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_IMAGE_PATH = "image_path"


            private val picasso: Picasso by lazy {
                Picasso.with(MyApplication.context)
            }

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(imagePath: String): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putString(ARG_IMAGE_PATH, imagePath)
                fragment.arguments = args
                return fragment
            }
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(images[position])
        }

        override fun getCount() = images.size

        override fun getPageTitle(position: Int) = images[position]
    }
}
