package com.example.ticket.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.ticket.R
import com.example.ticket.data.room.entity.Category
import com.example.ticket.utils.common.Constants.LANGUAGE_ENGLISH
import com.example.ticket.utils.common.Constants.LANGUAGE_HINDI
import com.example.ticket.utils.common.Constants.LANGUAGE_KANNADA
import com.example.ticket.utils.common.Constants.LANGUAGE_MALAYALAM
import com.example.ticket.utils.common.Constants.LANGUAGE_MARATHI
import com.example.ticket.utils.common.Constants.LANGUAGE_PUNJABI
import com.example.ticket.utils.common.Constants.LANGUAGE_SINHALA
import com.example.ticket.utils.common.Constants.LANGUAGE_TAMIL
import com.example.ticket.utils.common.Constants.LANGUAGE_TELUGU


class CategoryAdapter(
    private val context: Context,
    private val selectedLanguage: String,
    private val onCategoryClickListener: OnCategoryClickListener
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var categories: List<Category> = listOf()
    private var selectedItemPosition = 0
    interface OnCategoryClickListener {
        fun onCategoryClick(category: Category)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_ticket_category_single_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, position)
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryNameTextView: TextView = itemView.findViewById(R.id.tvTitle)

        fun bind(category: Category, position: Int) {
            categoryNameTextView.text = when (selectedLanguage) {
                LANGUAGE_ENGLISH -> category.categoryName
                LANGUAGE_MALAYALAM -> category.categoryNameMa
                LANGUAGE_TAMIL -> category.categoryNameTa
                LANGUAGE_KANNADA -> category.categoryNameKa
                LANGUAGE_TELUGU -> category.categoryNameTe
                LANGUAGE_HINDI -> category.categoryNameHi
                LANGUAGE_SINHALA -> category.categoryNameSi
                LANGUAGE_MARATHI -> category.categoryNameMr
                LANGUAGE_PUNJABI -> category.categoryNamePa
                else -> category.categoryName
            }

            val isSelected = position == selectedItemPosition
            itemView.setBackgroundResource(
                if (isSelected) R.drawable.category_select_bg
                else R.drawable.category_unselect_bg
            )

            categoryNameTextView.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected) R.color.white else R.color.black
                )
            )

            itemView.setOnClickListener {
                val previousSelectedItem = selectedItemPosition
                val currentPosition = bindingAdapterPosition

                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                selectedItemPosition = currentPosition

                notifyItemChanged(previousSelectedItem)
                notifyItemChanged(selectedItemPosition)

                onCategoryClickListener.onCategoryClick(category)
            }

        }
    }
}
