package com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.IntruderAdapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.R
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.databinding.IntruderSelfieItemBinding
import com.wearessc.theift_alrm.dont_touch_phone.anti_theift_2022.intruderdetection.FullPictureActivity
import java.io.File

class SelfieAdapter(
    val context: Context,
    private var selfieList: MutableList<SelfieModel>,
    private val onSelectionModeChanged: () -> Unit
) : RecyclerView.Adapter<SelfieAdapter.CollectionsHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    var isSelectionMode = false

    inner class CollectionsHolder(val binding: IntruderSelfieItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnLongClickListener {
                toggleSelection(adapterPosition)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionsHolder {
        return CollectionsHolder(IntruderSelfieItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CollectionsHolder, position: Int) {
        val selfie = selfieList[position]

        // Directly use the formatted dateTime from selfieList
        val formattedDateTime = selfie.dateTime

        // Split the formatted dateTime into date and time
        val dateTimeParts = formattedDateTime.split(" ", limit = 2)
        val date = dateTimeParts.getOrElse(0) { "" }
        val time = dateTimeParts.getOrElse(1) { "" }

        // Set date and time on separate lines
        holder.binding.dateTime.text = buildString {
            append(date)
            append("\n")
            append(time)
        }

        Glide.with(context)
            .load(selfie.imageUri)
            .apply(RequestOptions().placeholder(R.drawable.camera))
            .into(holder.binding.intruderSelfie)

        holder.binding.intruderSelfie.setOnClickListener {
            val intent = Intent(context, FullPictureActivity::class.java).apply {
                putExtra("SelfieUri", selfie.imageUri.toString())
            }
            context.startActivity(intent)
        }

        // Change background color based on selection state
        if (selectedItems.contains(position)) {
            holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.selected_item_round_boarder)
            holder.binding.checked.visibility = View.VISIBLE
        } else {
            holder.binding.root.background = ContextCompat.getDrawable(context, R.drawable.simple_round_boarder)
            holder.binding.checked.visibility = View.INVISIBLE
        }

        holder.binding.root.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                // Normal click action here
                val intent = Intent(context, FullPictureActivity::class.java).apply {
                    putExtra("SelfieUri", selfie.imageUri.toString())
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = selfieList.size

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        isSelectionMode = selectedItems.isNotEmpty()
        onSelectionModeChanged() // Notify the activity to update UI elements
        notifyItemChanged(position)
    }

    fun selectAll() {
        selectedItems.clear()
        selfieList.indices.forEach { selectedItems.add(it) }
        isSelectionMode = true
        onSelectionModeChanged()
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = false
        onSelectionModeChanged()
        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int = selectedItems.size

    fun deleteSelectedItems(onItemsDeleted: (Int) -> Unit) {
        val selectedIndices = selectedItems.sortedDescending()
        selectedIndices.forEach { index ->
            val selfieModel = selfieList[index]
            val file = File(selfieModel.imageUri.path ?: return@forEach)
            if (file.exists()) {
                file.delete()
            }
            selfieList.removeAt(index)
        }
        val deletedCount = selectedIndices.size
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onItemsDeleted(deletedCount)
    }
}
