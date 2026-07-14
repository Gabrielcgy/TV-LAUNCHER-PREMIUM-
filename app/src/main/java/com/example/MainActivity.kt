package com.example

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.databinding.ActivityMainBinding
import com.example.models.AppModel
import com.example.ui.adapters.AppAdapter
import com.example.utils.AppLoader
import com.example.utils.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val clockHandler = Handler(Looper.getMainLooper())
    private lateinit var updateManager: UpdateManager

    // Adapters for each TV category row
    private lateinit var gamesAdapter: AppAdapter
    private lateinit var appsAdapter: AppAdapter
    private lateinit var toolsAdapter: AppAdapter
    private lateinit var systemAdapter: AppAdapter

    // Clock update runnable
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000) // Update every second for accuracy
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize adapters with click listeners
        initAdapters()

        // Setup layouts & D-pad behaviors
        setupRecyclerViews()

        // Load apps asynchronously to keep UI thread extremely fluid
        refreshInstalledApps()

        // Start clock timer
        clockHandler.post(clockRunnable)

        // Initialize and check for premium OTA updates
        setupUpdateSystem()
    }

    override fun onResume() {
        super.onResume()
        // Refresh apps list when returning to launcher (in case an app was installed/uninstalled)
        refreshInstalledApps()
    }

    override fun onDestroy() {
        super.onDestroy()
        clockHandler.removeCallbacks(clockRunnable)
    }

    /**
     * Prevent exiting the launcher using the back button
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Consumed to keep TV launcher as the persistent home screen
    }

    private fun initAdapters() {
        val clickAction: (AppModel) -> Unit = { app ->
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    Toast.makeText(this, "No se puede abrir ${app.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al abrir la app", Toast.LENGTH_SHORT).show()
            }
        }

        gamesAdapter = AppAdapter(emptyList(), clickAction)
        appsAdapter = AppAdapter(emptyList(), clickAction)
        toolsAdapter = AppAdapter(emptyList(), clickAction)
        systemAdapter = AppAdapter(emptyList(), clickAction)
    }

    private fun setupRecyclerViews() {
        // Helper function to apply high performance layouts
        val setupRow = { rv: androidx.recyclerview.widget.RecyclerView, adapter: AppAdapter ->
            rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rv.adapter = adapter
            rv.setHasFixedSize(true) // Improves scrolling performance & reduces RAM overhead
            rv.itemAnimator = null   // Disables animations for fast TV layout changes
        }

        setupRow(binding.rvGames, gamesAdapter)
        setupRow(binding.rvApps, appsAdapter)
        setupRow(binding.rvTools, toolsAdapter)
        setupRow(binding.rvSystem, systemAdapter)
    }

    private fun refreshInstalledApps() {
        CoroutineScope(Dispatchers.IO).launch {
            val allApps = AppLoader.loadInstalledApps(this@MainActivity)

            // Split apps into their corresponding categorized lists
            val games = allApps.filter { it.category == AppLoader.CATEGORY_GAMES }
            val apps = allApps.filter { it.category == AppLoader.CATEGORY_APPS }
            val tools = allApps.filter { it.category == AppLoader.CATEGORY_TOOLS }
            val system = allApps.filter { it.category == AppLoader.CATEGORY_SYSTEM }

            withContext(Dispatchers.Main) {
                // Update adapters with new lists
                gamesAdapter.updateData(games)
                appsAdapter.updateData(apps)
                toolsAdapter.updateData(tools)
                systemAdapter.updateData(system)

                // Hide empty rows dynamically for clean visual layout
                binding.layoutGamesRow.visibility = if (games.isEmpty()) View.GONE else View.VISIBLE
                binding.layoutAppsRow.visibility = if (apps.isEmpty()) View.GONE else View.VISIBLE
                binding.layoutToolsRow.visibility = if (tools.isEmpty()) View.GONE else View.VISIBLE
                binding.layoutSystemRow.visibility = if (system.isEmpty()) View.GONE else View.VISIBLE

                // Set App counter
                binding.tvAppCount.text = "${allApps.size} Apps"
            }
        }
    }

    private fun updateClock() {
        val calendar = Calendar.getInstance()
        
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.getDefault())

        binding.tvTime.text = timeFormat.format(calendar.time)
        binding.tvDate.text = dateFormat.format(calendar.time)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun setupUpdateSystem() {
        updateManager = UpdateManager(this)
        
        // Check for updates automatically
        updateManager.checkForUpdates { hasUpdate ->
            if (hasUpdate) {
                binding.tvUpdateIndicator.visibility = View.VISIBLE
                // Click on the indicator to trigger the update dialog manually
                binding.tvUpdateIndicator.setOnClickListener {
                    updateManager.checkForUpdates()
                }
            } else {
                binding.tvUpdateIndicator.visibility = View.GONE
            }
        }
    }
}
