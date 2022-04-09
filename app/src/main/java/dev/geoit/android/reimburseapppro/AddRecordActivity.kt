package dev.geoit.android.reimburseapppro

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ortiz.touchview.TouchImageView
import dev.geoit.android.reimburseapppro.databinding.ActivityAddRecordBinding
import dev.geoit.android.reimburseapppro.room.AppDatabase
import dev.geoit.android.reimburseapppro.room.RecapEntity
import dev.geoit.android.reimburseapppro.utils.GetReimburseDirectory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class AddRecordActivity : AppCompatActivity() {

    companion object {
        const val PREFS_REFRESH = "REFRESH"
        const val IS_REFRESH = "is_refresh"
        const val JPG = ".jpg"
    }

    private lateinit var binding: ActivityAddRecordBinding

    private lateinit var projectId: String
    private lateinit var timeStamp: String
    private var photoURI: String = ""
    private var photoID: String = ""

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val prefs =
            applicationContext.getSharedPreferences(MainActivity.PREFS_RECAP, Context.MODE_PRIVATE)
        projectId = prefs.getString(MainActivity.RECAP_PROJECT_ID, "").toString()
        timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        photoID = projectId + "_" + timeStamp + "_"

        binding.spnARType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) {
                    binding.CLParentAddRecord.setBackgroundResource(R.color.colorGreenTransparent)
                } else if (position == 1) {
                    binding.CLParentAddRecord.setBackgroundResource(R.color.colorRedTransparent)
                }
            }
        }

        binding.imgARPhoto.setOnClickListener {
            dialogChooseCamera()
        }

        binding.fabAddRecord.setOnClickListener {
            if (binding.edtARNominal.text.toString() != "" && binding.edtARDesc.text.toString() != "") {
                saveRecord()
            } else {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.nominal_and_desc_not_empty),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun dialogChooseCamera() {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_image_intent, null)
        val llCamera = view.findViewById<LinearLayout>(R.id.LLDITCamera)
        val llGallery = view.findViewById<LinearLayout>(R.id.LLDITGallery)
        builder.setView(view)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        llCamera.setOnClickListener {
            takePicture()
            dialog.dismiss()
        }
        llGallery.setOnClickListener {
            choosePicture()
            dialog.dismiss()
        }
        dialog.show()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun choosePicture() {
        val choosePictureIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        if (choosePictureIntent.resolveActivity(this.packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_failed_to_choose_picture),
                    Toast.LENGTH_LONG
                ).show()
            }
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    getString(R.string.file_provider),
                    photoFile
                )
                choosePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(choosePictureIntent, 1)
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(this.packageManager) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(
                    this,
                    getString(R.string.msg_failed_to_take_photo),
                    Toast.LENGTH_LONG
                ).show()
            }
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    getString(R.string.file_provider),
                    photoFile
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, 0)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val storageDir: File = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val image = File.createTempFile(
            photoID,
            JPG,
            storageDir
        )
        photoURI = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, imageReturnedIntent: Intent?) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent)

        val displayMetrics = DisplayMetrics()
        this.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val layoutParams = LinearLayout.LayoutParams(width / 2, width / 2)

        val newPhoto = ImageView(this)
        newPhoto.layoutParams = layoutParams
        newPhoto.tag = photoURI
        newPhoto.setPadding(2, 2, 2, 2)
        newPhoto.setOnClickListener {
            dialogImageFullscreen(newPhoto, newPhoto.tag.toString())
        }

        when (requestCode) {
            0 -> if (resultCode == RESULT_OK) {
                val bmOptions = BitmapFactory.Options()
                bmOptions.inJustDecodeBounds = true
                BitmapFactory.decodeFile(photoURI, bmOptions)
                bmOptions.inJustDecodeBounds = false
                val bitmaps = BitmapFactory.decodeFile(photoURI, bmOptions)

                newPhoto.setImageBitmap(bitmaps)
                binding.LLARPhotos.addView(newPhoto)
            }
            1 -> if (resultCode == RESULT_OK) {
                val selectedImage = imageReturnedIntent?.data
                newPhoto.setImageURI(selectedImage)
                binding.LLARPhotos.addView(newPhoto)

                val bitmaps = (newPhoto.drawable as BitmapDrawable).bitmap
                try {
                    val out = FileOutputStream(photoURI)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    bitmaps.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                    val imgBytes = byteArrayOutputStream.toByteArray()
                    out.write(imgBytes)
                    out.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun dialogImageFullscreen(img: ImageView, imgURI: String) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_image_fullscreen, null)
        val popPhoto: TouchImageView = view.findViewById(R.id.imgDIFullscreen)
        val fabDelete: FloatingActionButton = view.findViewById(R.id.fabDIFDelete)
        val fabClose: FloatingActionButton = view.findViewById(R.id.fabDIFClose)
        builder.setView(view)
        val dialog = builder.create()
        try {
            popPhoto.setImageBitmap((img.drawable as BitmapDrawable).bitmap)
            fabDelete.setOnClickListener {
                dialog.dismiss()
                binding.LLARPhotos.removeView(img)
                val file = File(imgURI)
                file.delete()
            }
            fabClose.setOnClickListener {
                dialog.dismiss()
            }
        } catch (e: Throwable) {
            Toast.makeText(this, getString(R.string.msg_failed_to_show_image), Toast.LENGTH_LONG)
                .show()
        }
        dialog.show()
    }

    private fun saveRecord() {
        val timeStampLocal =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val type = binding.spnARType.selectedItem

        val recapEntity = RecapEntity(
            date = timeStampLocal,
            id_project = projectId,
            type = type.toString(),
            description = binding.edtARDesc.text.toString(),
            receipt = binding.spnARReceipt.selectedItem.toString(),
            comment = binding.edtARComment.text.toString(),
            income = if (type == "Income") binding.edtARNominal.text.toString().toDouble() else 0.0,
            expense = if (type == "Expense") binding.edtARNominal.text.toString()
                .toDouble() else 0.0,
            photo = photoID
        )

        thread {
            val database: AppDatabase by lazy { AppDatabase.getAppDatabase(this) }
            database.recapDao().insert(recapEntity)
        }

        for (i in 0 until binding.LLARPhotos.childCount - 1) {
            val imgView: View = binding.LLARPhotos.getChildAt(i + 1)
            val bitmaps = ((imgView as ImageView).drawable as BitmapDrawable).bitmap

            val imageFileName = photoID + (0..99999999).random()
            val storageDir = GetReimburseDirectory().getPicturesDirectory(this)
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            val saveImgFile = File(storageDir, imageFileName + JPG)
            try {
                val out = FileOutputStream(saveImgFile)
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmaps.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
                val imgBytes = byteArrayOutputStream.toByteArray()
                out.write(imgBytes)
                out.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        Toast.makeText(applicationContext, getString(R.string.msg_record_saved), Toast.LENGTH_SHORT)
            .show()
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (dir!!.isDirectory) {
            val children = dir.list()
            for (child in children!!) {
                File(dir, child).delete()
            }
        }
        val editor =
            applicationContext.getSharedPreferences(PREFS_REFRESH, Context.MODE_PRIVATE).edit()
        editor.putBoolean(IS_REFRESH, true).apply()
        finish()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return true
    }
}