package com.arrap.recordingapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.MediaStore.Audio.Media
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*

//Esta constante define un código de solicitud utilizado para identificar la solicitud de permisos
const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(),Timer.OnTimerTickListener {

    private lateinit var amplitudes: ArrayList<Float>

    //Es un arreglo que contiene los permisos que la aplicación requerirá.
    private var permission = arrayOf(android.Manifest.permission.RECORD_AUDIO)
    // Es una variable booleana que se utiliza para rastrear si el permiso RECORD_AUDIO ha sido otorgado por el usuario.
    private var permissionGaranted = false

    private lateinit var  recorder : MediaRecorder

    private var dirPath = ""
    private  var fileName = ""
    private var isRecording = false
    private var isPaused = false
    private var duration = ""

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var timer:Timer

    private lateinit var db : AppDatabase

    private lateinit var vibrator : Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //ActivityCompat.checkSelfPermission : Permite verificar si se ha otorgado un permiso específico a la aplicación.
        //PackageManager. Indica que el permiso específico ha sido otorgado por el usuario.
        permissionGaranted = ActivityCompat.checkSelfPermission(this,permission[0]) == PackageManager.PERMISSION_GRANTED

        //Si no hay permisos  -> pedirselos
        if(!permissionGaranted){
            //se utiliza para solicitar permisos al usuario en Android
            ActivityCompat.requestPermissions(this,permission, REQUEST_CODE)
        }

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        //Oculatar bottomsheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED


        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        btnRecord.setOnClickListener{
            when{
                isPaused -> resumeRecording()
                isRecording -> pauseRecording()
                else -> startRecording()
                }
            //hacer que vibre
            vibrator.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
        }

        btnList.setOnClickListener {
            startActivity(Intent(this,GalleryActivity::class.java))
        }

        btnDone.setOnClickListener {
            stopRecorder()
            Toast.makeText(this,"Record saved",Toast.LENGTH_SHORT).show()

            //Mostrar bottomsheet
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBG.visibility = View.VISIBLE
            filenameInput.setText(fileName)

        }

        btnCancel.setOnClickListener{
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        btnOk.setOnClickListener{
            dismiss()
            save()
        }

        bottomSheetBG.setOnClickListener{
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        btnDelete.setOnClickListener {
            stopRecorder()
            //Borrar audio
            File("$dirPath$fileName.mp3").delete()
            Toast.makeText(this,"Record deleted",Toast.LENGTH_SHORT).show()
        }

        btnDelete.isClickable = false

    }

    //Guardar grabacion
    private fun save() {
        val newFileName = filenameInput.text.toString()
        if(newFileName != fileName){
            var newFile = File("$dirPath$newFileName.mp3")
            //crea un nuevo archivo poniendo el contenido el contenido del archivo ruta en newFile y
            // elimina el archivo de la ruta al final solo queda 1 archivo con el nombre correcto
            File("$dirPath$fileName.mp3").renameTo(newFile)
        }

        var filePath = "$dirPath$newFileName.mp3"
        var timestamp = Date().time
        var ampsPath = "$dirPath$newFileName"

        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        }catch (e:IOException){ }

        var record = AudioRecord(newFileName,filePath,timestamp,duration,ampsPath)

        //Esto se ejecutara en un hilo secundario(de fondo)
        GlobalScope.launch {
            db.audioRecordDoc().insert(record)

        }
        val intent = Intent(this, GalleryActivity::class.java)
        startActivity(intent)


    }

    //Ocultar menu de guardar grabacion
    private fun dismiss(){
        bottomSheetBG.visibility = View.GONE
        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        },100)
        hideKeyBoard(filenameInput)
        //----NO FUNCIONA POR ALGUNA RAZON
        //bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

    }

    //oculatar teclado
    private fun hideKeyBoard(view:View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken,0)
    }

    //se utiliza para manejar la respuesta del usuario a la solicitud de permisos.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //grantResults: es un array de enteros que contiene los resultados de la solicitud de permisos.
        //PackageManager.PERMISSION_GRANTED si el usuario concedió el permiso o PackageManager.PERMISSION_DENIED si lo denegó.
        //permissionGaranted true si se obtubo permisos
        if(requestCode == REQUEST_CODE){
            permissionGaranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
        }
    }

    //GRABAR AUDIO
    private fun startRecording(){
        //Si no hay permisos -> pedirselos
        if(!permissionGaranted){
            ActivityCompat.requestPermissions(this,permission, REQUEST_CODE)
            return
        }

        //start recording
        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}/"
        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        fileName = "audio_record_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try {
                prepare()
            }catch (e:IOException){}

            start()
        }
        btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false
        timer.start()

        btnDelete.isClickable = true
        btnDelete.setImageResource(R.drawable.ic_delete)

        btnList.visibility = View.GONE
        btnDone.visibility = View.VISIBLE


    }

    //PAUSAR GRABACION
    private fun pauseRecording() {
        recorder.pause()
        isPaused = true
        btnRecord.setImageResource(R.drawable.ic_record)
        timer.pause()
    }

    //RESUMIR GRABACION
    private fun resumeRecording() {
        recorder.resume()
        isPaused = false
        btnRecord.setImageResource(R.drawable.ic_pause)
        timer.start()
    }

    //PARAR GRABACION
    private fun stopRecorder(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording = false

        btnList.visibility = View.VISIBLE
        btnDone.visibility = View.GONE

        btnDelete.isClickable = false
        btnDelete.setImageResource(R.drawable.ic_delete_disabled)

        btnRecord.setImageResource(R.drawable.ic_record)

        tvTimer.text = "00:00:00"
        amplitudes = waveformView.clear()


    }

    // CAMBIAR TEXTO DE DURACION Y ACTIVAR WAVE DE AUDIO
    override fun onTimerTick(duration: String) {
        tvTimer.text = duration
        this.duration = duration.dropLast(3)
        waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }

}