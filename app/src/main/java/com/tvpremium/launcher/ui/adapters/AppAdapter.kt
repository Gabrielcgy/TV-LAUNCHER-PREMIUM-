package com.tvpremium.launcher.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tvpremium.launcher.R
import com.tvpremium.launcher.models.AppModel
import com.google.android.material.card.MaterialCardView

class AppAdapter(
    private var apps: List<AppModel>,
    private val onAppClicked: (AppModel) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    fun updateData(newApps: List<AppModel>) {
        this.apps = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, onAppClicked)
    }

    override fun getItemCount(): Int = apps.size

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_view)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_app_name)

        fun bind(app: AppModel, onAppClicked: (AppModel) -> Unit) {
            tvName.text = app.name
            if (app.icon != null) {
                ivIcon.setImageDrawable(app.icon)
            } else {
                ivIcon.setImageResource(R.mipmap.ic_launcher)
            }

            itemView.setOnClickListener {
                onAppClicked(app)
            }

            // High performance, zero lag selection focus animation and illumination glow
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Scale up by 1.10x for selected item
                    itemView.animate()
                        .scaleX(1.10f)
                        .scaleY(1.10f)
                        .translationZ(8f) // Adds depth/shadow shadow on focus
                        .setDuration(150)
                        .start()

                    // Gold accent selection highlight border and illumination glow
                    cardView.strokeColor = itemView.context.getColor(R.color.accent_gold_light)
                    cardView.strokeWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.focus_stroke_width)
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.card_bg))
                } else {
                    // Scale down to 1.0x when losing focus
                    itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(150)
                        .start()

                    // Standard card appearance (no border)
                    cardView.strokeColor = itemView.context.getColor(R.color.black)
                    cardView.strokeWidth = itemView.context.resources.getDimensionPixelSize(R.dimen.default_stroke_width)
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.card_bg))
                }
            }
        }
    }
}
