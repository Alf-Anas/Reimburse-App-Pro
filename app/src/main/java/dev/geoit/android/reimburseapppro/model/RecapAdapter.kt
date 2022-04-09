package dev.geoit.android.reimburseapppro.model

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ortiz.touchview.TouchImageView
import dev.geoit.android.reimburseapppro.R
import dev.geoit.android.reimburseapppro.RecapitulationActivity
import dev.geoit.android.reimburseapppro.databinding.RecapListBinding
import dev.geoit.android.reimburseapppro.room.AppDatabase
import dev.geoit.android.reimburseapppro.utils.GetReimburseDirectory
import java.text.DecimalFormat
import java.text.NumberFormat
import kotlin.concurrent.thread

class RecapAdapter(
    private val activity: Activity
) : RecyclerView.Adapter<RecapAdapter.RecapViewHolder>() {

    private val mData = ArrayList<Recap>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecapViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.recap_list, parent, false)
        return RecapViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecapViewHolder, position: Int) {
        holder.bind(mData[position])
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(items: ArrayList<Recap>) {
        mData.clear()
        mData.addAll(items)
        notifyDataSetChanged()
    }

    inner class RecapViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val binding = RecapListBinding.bind(itemView)
        fun bind(recap: Recap) {
            binding.tvRecapDate.text = recap.date
            binding.tvRecapType.text = recap.type
            binding.tvRecapDesc.text = recap.description
            binding.tvRecapReceipt.text = recap.receipt
            if (recap.receipt == activity.resources.getStringArray(R.array.receipt)[0]) {
                binding.tvRecapReceipt.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG
            }
            val formatter: NumberFormat = DecimalFormat("#,###.##")
            binding.edtRecapNominal.setText(formatter.format(recap.nominal.toDouble()))
            binding.edtRecapBalance.setText(formatter.format(recap.balance.toDouble()))
            if (recap.photo != "") {
                val storageDir = GetReimburseDirectory().getPicturesDirectory(activity)
                if (storageDir.exists()) {
                    if (storageDir.isDirectory) {
                        val children = storageDir.listFiles()
                        for (child in children!!) {
                            if (child.toString().contains(recap.photo)) {
                                val bmOptions = BitmapFactory.Options()
                                bmOptions.inJustDecodeBounds = true
                                BitmapFactory.decodeFile(child.toString(), bmOptions)
                                bmOptions.inJustDecodeBounds = false
                                val bitmaps = BitmapFactory.decodeFile(child.toString(), bmOptions)
                                binding.imgRecap.setImageBitmap(bitmaps)
                                break
                            }
                        }
                    }
                }
            }

            val arrayType = activity.resources.getStringArray(R.array.type)
            if (recap.type == arrayType[0]) {
                itemView.setBackgroundResource(R.color.colorGreenTransparent)
            } else if (recap.type == arrayType[1]) {
                itemView.setBackgroundResource(R.color.colorRedTransparent)
            }

            itemView.setOnClickListener {
                val builder = AlertDialog.Builder(activity)
                val root: View = LayoutInflater.from(activity).inflate(R.layout.dialog_detail, null)
                val llView = root.findViewById<LinearLayout>(R.id.LLDDParent)
                val tvDate = root.findViewById<TextView>(R.id.tvDDDate)
                val edtType = root.findViewById<EditText>(R.id.edtDDType)
                val edtNominal = root.findViewById<EditText>(R.id.edtDDNominal)
                val edtDesc = root.findViewById<EditText>(R.id.edtDDDesc)
                val edtReceipt = root.findViewById<EditText>(R.id.edtDDReceipt)
                val edtComment = root.findViewById<EditText>(R.id.edtDDComment)
                val llPhotos = root.findViewById<LinearLayout>(R.id.LLDDPhotos)
                val fabDelete = root.findViewById<FloatingActionButton>(R.id.fabDDDelete)
                val fabClose: FloatingActionButton = root.findViewById(R.id.fabDDClose)

                builder.setView(root)
                val dialog = builder.create()

                tvDate.text = recap.date
                edtType.setText(recap.type)
                edtNominal.setText(formatter.format(recap.nominal.toDouble()))
                edtDesc.setText(recap.description)
                edtReceipt.setText(recap.receipt)
                edtComment.setText(recap.comment)

                if (recap.photo != "") {
                    val storageDir = GetReimburseDirectory().getPicturesDirectory(activity)
                    if (storageDir.exists()) {
                        if (storageDir.isDirectory) {
                            val children = storageDir.listFiles()
                            for (child in children!!) {
                                if (child.toString().contains(recap.photo)) {
                                    val bmOptions = BitmapFactory.Options()
                                    bmOptions.inJustDecodeBounds = true
                                    BitmapFactory.decodeFile(child.toString(), bmOptions)
                                    bmOptions.inJustDecodeBounds = false

                                    val displayMetrics = DisplayMetrics()
                                    (activity).windowManager.defaultDisplay.getMetrics(
                                        displayMetrics
                                    )
                                    val width = displayMetrics.widthPixels
                                    val layoutParams =
                                        LinearLayout.LayoutParams(width / 2, width / 2)

                                    val newPhoto = ImageView(activity)
                                    newPhoto.layoutParams = layoutParams
                                    newPhoto.tag = child.toString()
                                    newPhoto.setPadding(2, 2, 2, 2)
                                    newPhoto.setOnClickListener {
                                        dialogImageFullscreen(newPhoto)
                                    }
                                    val bitmaps =
                                        BitmapFactory.decodeFile(child.toString(), bmOptions)
                                    newPhoto.setImageBitmap(bitmaps)
                                    llPhotos.addView(newPhoto)
                                }
                            }
                        }
                    }
                }

                fabDelete.setOnClickListener {
                    val builderDelete = AlertDialog.Builder(activity)
                    builderDelete
                        .setTitle(activity.resources.getString(R.string.title_delete_record))
                        .setMessage(activity.resources.getString(R.string.msg_delete_record))
                    builderDelete.setPositiveButton(activity.resources.getString(R.string.tag_delete)) { _, _ ->
                        thread {
                            val database: AppDatabase by lazy { AppDatabase.getAppDatabase(activity) }
                            database.recapDao().deleteRecap(recap.id_project, recap.no)
                            val storageDir = GetReimburseDirectory().getPicturesDirectory(activity)
                            if (storageDir.exists()) {
                                if (storageDir.isDirectory) {
                                    val children = storageDir.listFiles()
                                    for (child in children!!) {
                                        if (child.toString().contains(recap.photo)) {
                                            child.delete()
                                        }
                                    }
                                }
                            }

                            activity.runOnUiThread {
                                dialog.dismiss()
                                val intent = Intent(activity, RecapitulationActivity::class.java)
                                activity.finish()
                                activity.startActivity(intent)
                            }
                        }
                    }
                    builderDelete.setNegativeButton(activity.resources.getString(R.string.tag_cancel)) { dialog, _ -> dialog.dismiss() }
                    val dialogDelete = builderDelete.create()
                    dialogDelete.show()
                }
                fabClose.setOnClickListener {
                    dialog.dismiss()
                }

                if (recap.type == arrayType[0]) {
                    llView.setBackgroundResource(R.color.colorGreenTransparent)
                } else if (recap.type == arrayType[1]) {
                    llView.setBackgroundResource(R.color.colorRedTransparent)
                }

                dialog.show()
            }
        }
    }

    private fun dialogImageFullscreen(photo: ImageView) {
        val builder = AlertDialog.Builder(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_image_fullscreen, null)
        val popPhoto: TouchImageView = view.findViewById(R.id.imgDIFullscreen)
        val fabDelete: FloatingActionButton = view.findViewById(R.id.fabDIFDelete)
        val fabClose: FloatingActionButton = view.findViewById(R.id.fabDIFClose)
        builder.setView(view)
        val dialog = builder.create()
        fabDelete.visibility = View.GONE
        fabClose.setOnClickListener {
            dialog.dismiss()
        }
        try {
            popPhoto.setImageBitmap((photo.drawable as BitmapDrawable).bitmap)
        } catch (e: Throwable) {
            Toast.makeText(
                activity,
                activity.resources.getString(R.string.msg_failed_to_show_image),
                Toast.LENGTH_SHORT
            ).show()
        }
        dialog.show()
    }

}