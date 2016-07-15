package com.puzheng.region_investigation.model

import android.content.ContentValues
import android.os.Parcel
import android.os.Parcelable
import android.provider.BaseColumns
import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.orhanobut.logger.Logger
import com.puzheng.region_investigation.CSVUtil
import com.puzheng.region_investigation.DBHelper
import com.puzheng.region_investigation.MyApplication
import com.puzheng.region_investigation.getPOIRow
import com.puzheng.region_investigation.store.AccountStore
import nl.komponents.kovenant.task
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


data class Region(val id: Long?, var name: String, var extras: Map<String, String>, var outline: List<LatLng>, val created: Date,
                  var updated: Date? = null, var synced: Date? = null) : Parcelable {

    class Model : BaseColumns {

        companion object {
            val TABLE_NAME = "region"
            val COL_NAME = "name"
            val COL_EXTRAS = "extras"
            val COL_OUTLINE = "outline"
            val COL_CREATED = "created"
            val COL_UPDATED = "updated"
            val COL_SYNCED = "synced"

            val CREATE_SQL: String
                get() = """
                    CREATE TABLE $TABLE_NAME (
                        ${BaseColumns._ID}  INTEGER PRIMARY KEY AUTOINCREMENT,
                        $COL_NAME TEXT NOT NULL UNIQUE,
                        $COL_EXTRAS TEXT,
                        $COL_OUTLINE TEXT NOT NULL,
                        $COL_CREATED TEXT NOT NULL,
                        $COL_UPDATED TEXT,
                        $COL_SYNCED TEXT
                    )
                """

            fun makeValues(region: Region) = ContentValues().apply {
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                put(COL_NAME, region.name)
                val jo = JSONObject().apply {
                    for ((key, value) in region.extras) {
                        put(key, value)
                    }
                }
                put(COL_EXTRAS, jo.toString())
                put(COL_OUTLINE, encodeOutline(region.outline))
                put(COL_CREATED, format.format(region.created))
                put(COL_UPDATED, if (region.updated != null) format.format(region.updated) else null)
                put(COL_SYNCED, if (region.synced != null) format.format(region.synced) else null)
            }
        }
    }

    /**
     * 是否为脏数据，如果从来没有同步过；同步时间早于更新时间
     */
    val isDirty: Boolean
        get() = (synced == null || (updated != null && synced!! < updated))


    constructor(source: Parcel) : this(
            source.readSerializable() as Long?,
            source.readString(),
            {
               jo: JSONObject ->
                val map = mutableMapOf<String, String>()
                for (key in jo.keys()) {
                    map.put(key, jo.getString(key))
                }
                map
            }(JSONObject(source.readString())),
            decodeOutline(source.readString()),
            source.readSerializable() as Date,
            source.readSerializable() as Date?,
            source.readSerializable() as Date?)

    /**
     * 区域面积， 参考http://www.wikihow.com/Calculate-the-Area-of-a-Polygon
     */
    val area: Double
        get() {
            if (outline.isEmpty()) {
                return 0.0
            }
            // 以出发点作为(0, 0), 注意，由于火星坐标系的原因，这里不能直接采用mercator投影
            val start = outline[0]
            val t = listOf(Pair(0.0, 0.0)) + outline.subList(1, outline.size).map {
                Pair(
                        (if (it.latitude > start.latitude) 1 else -1) *
                                AMapUtils.calculateLineDistance(start, LatLng(it.latitude, start.longitude)).toDouble(),
                        (if (it.longitude > start.longitude) 1 else -1) *
                                AMapUtils.calculateLineDistance(start, LatLng(start.latitude, it.longitude)).toDouble()
                )
            } + listOf(Pair(0.0, 0.0))
            return Math.abs(((0..t.size - 2).map {
                t[it].first * t[it + 1].second
            }.sum() - (0..t.size - 2).map {
                t[it].second * t[it + 1].first
            }.sum())) / 2
        }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeSerializable(id)
        dest?.writeString(name)
        val jo = JSONObject()
        for ((key, value) in extras) {
            jo.put(key, value)
        }
        dest?.writeString(jo.toString())
        dest?.writeString(encodeOutline(outline))
        dest?.writeSerializable(created)
        dest?.writeSerializable(updated)
        dest?.writeSerializable(synced)
    }


    companion object {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        @JvmField final val CREATOR: Parcelable.Creator<Region> = object : Parcelable.Creator<Region> {
            override fun createFromParcel(source: Parcel): Region {
                return Region(source)
            }

            override fun newArray(size: Int): Array<Region?> {
                return arrayOfNulls(size)
            }
        }


        fun decodeOutline(s: String) = s.split(":").map {
            val (lat, lng) = it.split(",").map { it.toDouble() }
            LatLng(lat, lng)
        }

        fun encodeOutline(outline: List<LatLng>) =
                outline.map { "${it.latitude},${it.longitude}" }.joinToString(":")
    }

    fun jsonizeSync(jsonObject: JSONObject) {
        jsonObject.apply {
            put("id", id)
            put("name", name)
            put("extras", JSONObject().apply {
                for ((key, value) in extras) {
                    put(key, value)
                }
            })
            put("outline", encodeOutline(outline))
            put("created", format.format(created))
            put("updated", if (updated != null) format.format(updated) else null)
            put("pois", JSONArray().apply {
                poiListSync?.forEach {
                    put(JSONObject().apply {
                        it.jsonize(this)
                    })
                }
            })
        }
    }

    private val dbHelper: DBHelper by lazy {
        DBHelper(MyApplication.context)
    }

    val poiListSync: List<POI>?
        get() = dbHelper.withDb {
            db ->
            try {
                val cursor = db.query(POI.Model.TABLE_NAME, null, "${POI.Model.COL_REGION_ID}=?", arrayOf(id.toString()),
                        null, null, null)
                var rows: List<POI>? = null
                if (cursor.moveToFirst()) {
                    rows = mutableListOf()
                    do {
                        rows.add(cursor.getPOIRow())
                    } while (cursor.moveToNext())
                }
                cursor.close()
                rows
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }


    val poiList = task {
        poiListSync
    }

    fun setSyncedSync() {
        dbHelper.withWritableDb {
            db ->
            db.update(Region.Model.TABLE_NAME, ContentValues().apply {
                put(Region.Model.COL_SYNCED, format.format(Date()))
            }, "${BaseColumns._ID}=?", arrayOf(id.toString()))
        }
    }


    val csvString: String
    get() {
        val sb = StringBuilder()
        val fieldSep = CSVUtil.fieldSep
        val lineSep = CSVUtil.lineSep
        sb.append(listOf("项", "值").joinToString(fieldSep) + lineSep)
        sb.append(listOf("编号", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("分类", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("名称", CSVUtil.quote(name)).joinToString(fieldSep) + lineSep)
        sb.append(listOf("地址", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("省", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("市", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("区", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("经度", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("纬度", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf(
                "重点区域边界",
                CSVUtil.quote(outline.map { "${it.longitude},${it.latitude}" }.joinToString(";"))
        ).joinToString(fieldSep) + lineSep)
        sb.append(listOf("说明", extras["说明"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("联系方式", extras["联系方式"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("所属派出所", extras["所属派出所"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("派出所联系电话", extras["派出所联系电话"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("所属居委会", extras["所属居委会"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("居委会联系电话", extras["居委会联系电话"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("工作关系姓名", extras["工作关系姓名"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("工作关系电话", extras["工作关系电话"] ?: "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("工作关系说明", extras["工作关系说明"] ?: "").joinToString(fieldSep) + lineSep)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        AccountStore.with(MyApplication.context).account.let {
            sb.append(listOf("采集人", CSVUtil.quote(it!!.username)).joinToString(fieldSep) + lineSep)
            sb.append(listOf("采集时间", CSVUtil.quote(sdf.format(created))).joinToString(fieldSep) + lineSep)
            sb.append(listOf("采集单位名称", CSVUtil.quote(it.orgName)).joinToString(fieldSep) + lineSep)
            sb.append(listOf("采集单位编码", CSVUtil.quote(it.orgCode)).joinToString(fieldSep) + lineSep)
        }
        sb.append(listOf("录入人", "").joinToString(fieldSep) + lineSep)
        sb.append(listOf("更新时间", "").joinToString(fieldSep) + lineSep)
        return sb.toString()
    }


}