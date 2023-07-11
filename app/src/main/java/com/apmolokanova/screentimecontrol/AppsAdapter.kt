package com.apmolokanova.screentimecontrol

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsAdapter(
    context: Context) : ListAdapter<App, AppsAdapter.AppViewHolder>(AppsCallback()){
    private val inflater : LayoutInflater = LayoutInflater.from(context)
    private val appsViewModel : AppsViewModel = ViewModelProvider(context as ViewModelStoreOwner)[AppsViewModel::class.java]
    private var onItemClickListener: OnItemClickListener<App>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder(inflater.inflate(R.layout.listed_app,parent,false))
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener<App>?) {
        this.onItemClickListener = onItemClickListener
    }

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val appNameView: MaterialTextView = itemView.findViewById(R.id.appName)
        private val onOffToggle: SwitchMaterial = itemView.findViewById(R.id.onOffToggle)
        private val appIconView: ShapeableImageView = itemView.findViewById(R.id.appIcon)

        fun bind(app: App) {
            onOffToggle.setOnCheckedChangeListener { _, isChecked -> app.limitsActive = isChecked}
            appNameView.text = app.name
            onOffToggle.isChecked = app.limitsActive
            CoroutineScope(Dispatchers.IO).launch {
                val appIcon = appsViewModel.getAppIcon(app.appId, itemView.context)
                withContext(Dispatchers.Main) { appIconView.setImageDrawable(appIcon) }
            }
            if(onItemClickListener != null) itemView.setOnClickListener {onItemClickListener!!.onItemClick(app) }
        }
    }
}

class AppsCallback: DiffUtil.ItemCallback<App>() {
    override fun areItemsTheSame(oldItem: App, newItem: App): Boolean = oldItem.appId == newItem.appId

    override fun areContentsTheSame(oldItem: App, newItem: App): Boolean {
        return oldItem.limitsActive == newItem.limitsActive &&
                oldItem.name == newItem.name
    }
}