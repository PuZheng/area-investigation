package com.puzheng.region_investigation

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import com.orhanobut.logger.Logger
import com.squareup.picasso.Picasso
import java.io.File
import java.util.*

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

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        // Set up the ViewPager with the sections adapter.
        viewPager = findView(R.id.container)
        viewPager.adapter = SectionsPagerAdapter(supportFragmentManager)
        viewPager.currentItem = pos

        findView<FloatingActionButton>(R.id.fab).setOnClickListener {
            Picasso.with(this).invalidate(images[viewPager.currentItem])
            images = images.filterIndexed { i, s -> i != viewPager.currentItem }
            if (images.isEmpty()) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra(TAG_IMAGES, ArrayList(images))
                })
                finish()
            } else {
                (viewPager.adapter as SectionsPagerAdapter).apply {
                    fragments = fragments.filterIndexed { i, fragment -> i != viewPager.currentItem }
                    viewPager.adapter = viewPager.adapter
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(RESULT_OK, Intent().apply {
                putExtra(TAG_IMAGES, ArrayList(images))
            })
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResult(RESULT_OK, Intent().apply {
            putExtra(TAG_IMAGES, ArrayList(images))
        })
        super.onBackPressed()
    }



    /**
     * A placeholder fragment containing a simple view.
     */
    private class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?) =
                inflater!!.inflate(R.layout.fragment_carousel, container, false).apply {
                    picasso.load(File(arguments.getString(ARG_IMAGE_PATH))).fit().centerInside().into(findView<ImageView>(R.id.imageView))
                }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            val ARG_IMAGE_PATH = "image_path"
            val ARG_INDEX = "index"


            private val picasso: Picasso by lazy {
                Picasso.with(MyApplication.context)
            }

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(index: Int, imagePath: String): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putString(ARG_IMAGE_PATH, imagePath)
                args.putLong(ARG_INDEX, index.toLong())
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

        lateinit var fragments: List<Fragment>

        init {
            fragments = images.mapIndexed { i, s ->
                PlaceholderFragment.newInstance(i, s)
            }
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemId(position: Int): Long {
            return fragments[position].arguments.getLong(PlaceholderFragment.ARG_INDEX)
        }

        override fun getCount() = fragments.size

    }
}
