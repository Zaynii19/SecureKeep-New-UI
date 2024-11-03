package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.HomeRCV

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.MainActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.R
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.antipocket.AntiPocketActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.chargingdetect.ChargeDetectActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.databinding.CatagoryItemsBinding
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.earphonedetection.EarphonesActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.PermissionActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.overchargedetection.OverChargeActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.touchdetection.TouchPhoneActivity
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.wifidetection.WifiActivity

class RvAdapter(val context: Context, private var categoryList: ArrayList<RCVModel>): Adapter<RvAdapter.MyCatViewHolder>() {
    class MyCatViewHolder(val binding: CatagoryItemsBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyCatViewHolder {
        return MyCatViewHolder(CatagoryItemsBinding.inflate(LayoutInflater.from(parent.context),parent, false))
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    override fun onBindViewHolder(holder: MyCatViewHolder, position: Int) {
        //Set Category
        val dataList = categoryList[position]
        holder.binding.categoryPic.setImageResource(dataList.catImage)
        holder.binding.categoryText.text = dataList.catText
        holder.binding.categoryDescrip.text = dataList.catDescrip

        when(dataList.catText){
            "Intruder Alert" -> holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.intruder_item_background)
            "Don't Touch My Phone" -> holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.touch_item_background)
            "Anti Pocket Detection" -> holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.pocket_item_background)
            "Charging Detection" -> holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.charge_item_background)
            "Wifi Detection" -> holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.wifi_item_background)
            "Avoid Over Charging" -> holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.overcharge_item_background)
            "Earphones Detection" -> holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.earphone_item_background)
        }

        holder.binding.categoryBtn.setOnClickListener {
            val hasPermission = MainActivity.checkPermissionsForService(context) // Check permission dynamically here

            val intent = when (dataList.catText) {
                "Intruder Alert" ->
                    if (hasPermission) {
                        Intent(context, IntruderActivity::class.java)
                    } else {
                        Intent(context, PermissionActivity::class.java)
                    }
                "Don't Touch My Phone" -> Intent(context, TouchPhoneActivity::class.java)
                "Anti Pocket Detection" -> Intent(context, AntiPocketActivity::class.java)
                "Charging Detection" -> Intent(context, ChargeDetectActivity::class.java)
                "Wifi Detection" -> Intent(context, WifiActivity::class.java)
                "Avoid Over Charging" -> Intent(context, OverChargeActivity::class.java)
                "Earphones Detection" -> Intent(context, EarphonesActivity::class.java)
                else -> Intent(context, TouchPhoneActivity::class.java) // Default case
            }
            ContextCompat.startActivity(context, intent, null)
        }

    }
}

