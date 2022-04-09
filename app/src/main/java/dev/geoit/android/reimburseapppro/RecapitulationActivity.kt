package dev.geoit.android.reimburseapppro

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dev.geoit.android.reimburseapppro.AddRecordActivity.Companion.IS_REFRESH
import dev.geoit.android.reimburseapppro.AddRecordActivity.Companion.JPG
import dev.geoit.android.reimburseapppro.AddRecordActivity.Companion.PREFS_REFRESH
import dev.geoit.android.reimburseapppro.MainActivity.Companion.PREFS_RECAP
import dev.geoit.android.reimburseapppro.MainActivity.Companion.RECAP_PROJECT_ID
import dev.geoit.android.reimburseapppro.databinding.ActivityRecapitulationBinding
import dev.geoit.android.reimburseapppro.model.Recap
import dev.geoit.android.reimburseapppro.model.RecapAdapter
import dev.geoit.android.reimburseapppro.room.AppDatabase
import dev.geoit.android.reimburseapppro.utils.GetReimburseDirectory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class RecapitulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecapitulationBinding

    private lateinit var projectId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecapitulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        binding.rvRecapitulation.adapter = RecapAdapter(this)

        val prefs = applicationContext.getSharedPreferences(PREFS_RECAP, Context.MODE_PRIVATE)
        projectId = prefs.getString(RECAP_PROJECT_ID, "").toString()

        binding.rvRecapitulation.setHasFixedSize(true)
        binding.rvRecapitulation.layoutManager = LinearLayoutManager(this)

        binding.btnAddRecap.setOnClickListener {
            val intent = Intent(this, AddRecordActivity::class.java)
            startActivity(intent)
        }

        binding.fabAddRecapDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(getString(R.string.msg_delete_project))
                .setTitle(getString(R.string.title_delete_project))
            builder.setPositiveButton(getString(R.string.tag_delete)) { dialog, _ ->
                dialog.dismiss()
                deleteProject()
            }
            builder.setNegativeButton(getString(R.string.tag_cancel)) { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }

        binding.fabAddRecapDownload.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.title_download_project))
            builder.setPositiveButton(getString(R.string.tag_download)) { dialog, _ ->
                dialog.dismiss()
                downloadProject()
            }
            builder.setNegativeButton(getString(R.string.tag_cancel)) { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }

        loadRecapData()
    }

    @SuppressLint("SimpleDateFormat")
    private fun downloadProject() {
        Toast.makeText(this, getString(R.string.msg_downloading), Toast.LENGTH_SHORT).show()
        val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        val storageDir = GetReimburseDirectory().getDownloadsDirectory(this, projectId, timeStamp)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val storagePictureDir = GetReimburseDirectory().getPicturesDirectory(this)
        try {
            val dataCSV = File(storageDir, "$projectId.csv")
            val fOut = FileOutputStream(dataCSV)
            val entryHeader =
                """
                No,Date,Type,Description,Receipt,Comment,Income,Expense,Balance
                
                """.trimIndent()
            fOut.write(entryHeader.toByteArray())

            thread {
                val database: AppDatabase by lazy { AppDatabase.getAppDatabase(this) }
                val listRecap = database.recapDao().getProjectRecap(projectId)

                runOnUiThread {
                    val arrayType = resources.getStringArray(R.array.type)
                    val formatter: NumberFormat = DecimalFormat("#.##")
                    var no = 0
                    var balance = 0.0
                    var totalIncome = 0.0
                    var totalExpense = 0.0
                    for (recap in listRecap) {
                        no++
                        var nominal: Double
                        if (recap.type == arrayType[0]) {
                            nominal = recap.income
                            totalIncome += nominal
                            balance += nominal
                        } else if (recap.type == arrayType[1]) {
                            nominal = recap.expense
                            totalExpense += nominal
                            balance -= nominal
                        }

                        val entryBody =
                            """
                                $no,"
                                ${recap.date}","
                                ${recap.type}","
                                ${recap.description}","
                                ${recap.receipt}","
                                ${recap.comment}","
                                ${recap.income}","
                                ${recap.expense}","
                                ${formatter.format(balance)}",
                                
                                """.trimIndent()
                        fOut.write(entryBody.toByteArray())

                        if (storagePictureDir.exists()) {
                            if (storagePictureDir.isDirectory) {
                                val children = storagePictureDir.listFiles()
                                for (child in children!!) {
                                    if (child.toString().contains(recap.photo)) {
                                        val imageFileName =
                                            projectId + "_" + no + "_" + (1111..9999).random()
                                        val saveImgFile = File(storageDir, imageFileName + JPG)
                                        child.copyTo(saveImgFile)
                                    }
                                }
                            }
                        }
                    }
                    val entryFooter =
                        """
                            ,,,,,Total = ,${formatter.format(totalIncome)},${
                            formatter.format(
                                totalExpense
                            )
                        },
                            
                            """.trimIndent()
                    fOut.write(entryFooter.toByteArray())
                    fOut.close()
                    Toast.makeText(
                        this,
                        getString(R.string.msg_download_finished),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun deleteProject() {
        thread {
            val database: AppDatabase by lazy { AppDatabase.getAppDatabase(this) }
            database.projectDao().deleteProjectByIDProject(projectId)
            database.recapDao().deleteAllRecapByIDProject(projectId)
        }

        val storageDir = GetReimburseDirectory().getPicturesDirectory(this)
        if (storageDir.exists()) {
            if (storageDir.isDirectory) {
                val children = storageDir.listFiles()
                for (child in children!!) {
                    if (child.toString().contains(projectId)) {
                        child.delete()
                    }
                }
            }
        }
        Toast.makeText(this, getString(R.string.msg_project_deleted), Toast.LENGTH_LONG).show()
        finish()
    }

    private fun loadRecapData() {
        thread {
            val database: AppDatabase by lazy { AppDatabase.getAppDatabase(this) }
            val listRecap = database.recapDao().getProjectRecap(projectId)

            val recapList = ArrayList<Recap>()

            val arrayType = resources.getStringArray(R.array.type)
            var balance = 0.0
            var totalIncome = 0.0
            var totalExpense = 0.0
            for (recap in listRecap) {
                var nominal = 0.0
                if (recap.type == arrayType[0]) {
                    nominal = recap.income
                    totalIncome += nominal
                    balance += nominal
                } else if (recap.type == arrayType[1]) {
                    nominal = recap.expense
                    totalExpense += nominal
                    balance -= nominal
                }
                recapList.add(
                    Recap(
                        recap.id,
                        recap.date,
                        recap.id_project,
                        recap.type,
                        recap.description,
                        recap.receipt,
                        recap.comment,
                        nominal.toString(),
                        balance.toString(),
                        recap.photo
                    )
                )
            }

            runOnUiThread {
                val adapter = RecapAdapter(this)
                adapter.setData(recapList)
                binding.rvRecapitulation.adapter = adapter
                val formatter: NumberFormat = DecimalFormat("#,###.##")
                binding.tvTotalIncome.text = formatter.format(totalIncome)
                binding.tvTotalExpense.text = formatter.format(totalExpense)
                binding.tvBalance.text = formatter.format(balance)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val prefs = applicationContext.getSharedPreferences(PREFS_REFRESH, Context.MODE_PRIVATE)
        if (prefs.getBoolean(IS_REFRESH, false)) {
            loadRecapData()
            prefs.edit().putBoolean(IS_REFRESH, false).apply()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }
}