package dev.geoit.android.reimburseapppro

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import dev.geoit.android.reimburseapppro.databinding.ActivityMainBinding
import dev.geoit.android.reimburseapppro.room.AppDatabase
import dev.geoit.android.reimburseapppro.room.ProjectEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_RECAP = "RECAP"
        const val RECAP_PROJECT_ID = "id_project"
        const val RECAP_PROJECT_NAME = "project_name"
    }

    private lateinit var binding: ActivityMainBinding

    private lateinit var data: ArrayList<MutableMap<String, String>>

    private val _name = "name"
    private val _date = "date"
    private val _idProject = "id_project"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.setLogo(R.drawable.ic_baseline_attach_money_24)
        supportActionBar!!.setDisplayUseLogoEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        binding.fabMainAdd.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                newProjectDialog()
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                Toast.makeText(
                    this,
                    getString(R.string.allow_write_permission),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        refreshListProject()
    }

    private fun newProjectDialog() {
        val builder = AlertDialog.Builder(this)
        @SuppressLint("InflateParams") val view: View =
            layoutInflater.inflate(R.layout.dialog_create_project, null)
        val edtName = view.findViewById<EditText>(R.id.edtNameProject)
        builder.setView(view)
        builder.setPositiveButton(getString(R.string.tag_create)) { _, _ ->
            val strName = edtName.text.toString()
            if (strName != "" && strName.length >= 5) {
                createNewProject(strName)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.min_project_name),
                    Toast.LENGTH_SHORT
                ).show()
                newProjectDialog()
            }
        }
        builder.setNegativeButton(getString(R.string.tag_cancel)) { it, _ -> it.dismiss() }
        val dialog = builder.create()
        dialog.show()
    }

    private fun createNewProject(strName: String) {
        val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val timeStampID = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val ranID = (1000..9999).random()

        val projectEntity = ProjectEntity(
            date = timeStamp,
            id_project = timeStampID + "_" + ranID.toString(),
            project_name = strName
        )

        thread {
            val database: AppDatabase by lazy { AppDatabase.getAppDatabase(this) }
            database.projectDao().insert(projectEntity)

            runOnUiThread {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.project_created),
                    Toast.LENGTH_SHORT
                )
                    .show()
                refreshListProject()
            }
        }
    }

    private fun refreshListProject() {
        thread {
            val database: AppDatabase by lazy { AppDatabase.getAppDatabase(this) }
            val listProject = database.projectDao().getAllProject()

            data = ArrayList()

            if (listProject.isEmpty()) {
                val datum: MutableMap<String, String> = HashMap()
                datum[_name] = getString(R.string.project_not_exist)
                datum[_date] = ""
                datum[_idProject] = ""
                data.add(datum)
            } else {
                for (project in listProject) {
                    val datum: MutableMap<String, String> = HashMap()
                    datum[_name] = project.project_name
                    datum[_date] = project.date
                    datum[_idProject] = project.id_project
                    data.add(datum)
                }
            }

            runOnUiThread {
                val adapter = SimpleAdapter(
                    this,
                    data,
                    android.R.layout.simple_list_item_2,
                    arrayOf(_name, _date),
                    intArrayOf(
                        android.R.id.text1,
                        android.R.id.text2
                    )
                )

                binding.lvMainProject.adapter = adapter
                binding.lvMainProject.isSelected = true
                binding.lvMainProject.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, position, _ ->
                        if (listProject.isNotEmpty()) {
                            val editor = getSharedPreferences(PREFS_RECAP, MODE_PRIVATE).edit()
                            editor.putString(RECAP_PROJECT_NAME, data[position][_name])
                            editor.putString(RECAP_PROJECT_ID, data[position][_idProject])
                            editor.apply()

                            val intent = Intent(this, RecapitulationActivity::class.java)
                            startActivity(intent)
                        }
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshListProject()
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, R.string.double_back_pressed_to_exit, Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }
}