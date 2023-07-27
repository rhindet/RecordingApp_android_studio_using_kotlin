package com.arrap.recordingapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity :AppCompatActivity(),OnItemClickListener{

    private lateinit var records : ArrayList<AudioRecord>
    private lateinit var  mAdapter:Adapter
    private lateinit var  db:AppDatabase
    private lateinit var searchInput : TextInputEditText
    private lateinit var bottomSheet : LinearLayout
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private var allChecked = false

    private lateinit var btnRename : ImageButton
    private lateinit var btnDelete : ImageButton

    private lateinit var tvRename : TextView
    private lateinit var tvDelete  : TextView

    private lateinit var toolbar: MaterialToolbar

    private lateinit var editBar :View
    private lateinit var  btnClose:ImageButton
    private lateinit var  btnSelectAll:ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener{
            onBackPressed()
        }

        btnRename = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        tvRename = findViewById(R.id.tvEdit)
        tvDelete = findViewById(R.id.tvDelete)

        editBar = findViewById(R.id.editBar)
        btnClose = findViewById(R.id.btnClose)
        btnSelectAll = findViewById(R.id.btnSelectAll)

        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        records = ArrayList()

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()



        mAdapter = Adapter(records , this)

        recyclerview.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(context)
        }
        fetchALL()

        searchInput = findViewById(R.id.search_input)
        searchInput.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
              var query = p0.toString()
               searchDatabase(query)
            }

            override fun afterTextChanged(p0: Editable?) {}

        })

        btnClose.setOnClickListener {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            editBar.visibility = View.GONE

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            records.map{it.isChecked = false}
            mAdapter.setEditMode(false)
        }

        btnSelectAll.setOnClickListener {
            allChecked = !allChecked
            records.map { it.isChecked = allChecked }
            mAdapter.notifyDataSetChanged()

            if(allChecked){
                enableDelete()
                disableRename()
            }else{
                disableDelete()
                disableRename()
            }
        }

        btnDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Elimar grabacion?")
            val nbRecords = records.count{it.isChecked}
            builder.setMessage("Estas seguro que quieres borrar $nbRecords grabacion(es) ?")
            builder.setPositiveButton("Eliminar"){_,_->
                val toDelete = records.filter{it.isChecked}.toTypedArray()
                GlobalScope.launch {
                    db.audioRecordDoc().delete(toDelete)
                    runOnUiThread{
                        records.removeAll(toDelete)
                        mAdapter.notifyDataSetChanged()
                        leaveEditMode()
                    }
                }
            }
            builder.setNegativeButton("Cancelar"){_,_->
                //it does nothing
            }
            val dialog = builder.create()
            dialog.show()
        }

        btnRename.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val dialogView = this.layoutInflater.inflate(R.layout.rename_layout,null)
            builder.setView(dialogView)
            val dialog = builder.create()

            val record = records.filter { it.isChecked }.get(0)

            val textInput = dialogView.findViewById<TextInputEditText>(R.id.filenameInput)
            textInput.setText(record.filename)

            dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener{
                val input = textInput.text.toString()
                if(input.isEmpty()){
                    Toast.makeText(this,"A name is required",Toast.LENGTH_LONG).show()
                }else{
                    record.filename = input
                    GlobalScope.launch {
                        db.audioRecordDoc().update(record)
                        runOnUiThread{
                            mAdapter.notifyItemChanged(records.indexOf(record))
                            dialog.dismiss()
                            leaveEditMode()
                        }
                    }
                }
            }

            dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener{
                    dialog.dismiss()
            }

            dialog.show()
        }

    }

    private fun leaveEditMode(){
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editBar.visibility = View.GONE

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        records.map{it.isChecked = false}
        mAdapter.setEditMode(false)
    }


    private fun disableRename(){
        btnRename.isClickable = false
        btnRename.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme)
        tvEdit.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme))
    }

    private fun disableDelete(){
        btnDelete.isClickable = false
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.grayDarkDisabled,theme))
    }

    private fun enableRename(){
        btnRename.isClickable = true
        btnRename.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.azul,theme)
        tvEdit.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.azul,theme))
    }

    private fun enableDelete(){
        btnDelete.isClickable = true
        btnDelete.backgroundTintList = ResourcesCompat.getColorStateList(resources,R.color.red,theme)
        tvDelete.setTextColor(ResourcesCompat.getColorStateList(resources,R.color.red,theme))
    }

    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            records.clear()
            var queryResult : List<AudioRecord> = db.audioRecordDoc().searchDatabase("%$query%")
            records.addAll(queryResult)

            runOnUiThread {
                mAdapter.notifyDataSetChanged()
            }

        }
    }

    private fun fetchALL(){
            GlobalScope.launch {
                records.clear()
                var queryResult : List<AudioRecord> = db.audioRecordDoc().getAll()
                val reversedRecords: List<AudioRecord> = queryResult.reversed()
                records.addAll(reversedRecords)

                mAdapter.notifyDataSetChanged()
            }
        }

    override fun onItemClickListener(position: Int) {
      var audioRecord = records[position]

        if(mAdapter.isEditMode()){
            records[position].isChecked = !records[position].isChecked
            mAdapter.notifyItemChanged(position)

            var nbSelected = records.count{it.isChecked}
            when(nbSelected){
                0 -> {
                    disableRename()
                    disableDelete()
                }
                1 -> {
                    enableDelete()
                    enableRename()
                }
                else->{
                    disableRename()
                    enableDelete()
                }
            }
        }else{
            var intent = Intent(this,AudioPlayerActivity::class.java)

            intent.putExtra("filepath",audioRecord.filePath)
            intent.putExtra("filename",audioRecord.filename)
            startActivity(intent)
        }



    }

    override fun onItemLongClickListener(position: Int) {
        mAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        mAdapter.notifyItemChanged(position)

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        if(mAdapter.isEditMode() && editBar.visibility == View.GONE){
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)

            editBar.visibility = View.VISIBLE

            enableDelete()
            enableRename()
        }
    }


}