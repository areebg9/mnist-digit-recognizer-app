package com.example.firebase

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.custom.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    private lateinit var button: Button

    private lateinit var inputBitmap: Bitmap

    private val input = Array(1) {Array(28) {Array(28) { FloatArray(1)} } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    public fun infer(view: View): Void?{
        inputBitmap = canvasView.getBitmap()

        inputBitmap = Bitmap.createScaledBitmap(inputBitmap, 28, 28, true)

        for (x in 0..27){
            for (y in 0..27){
                val pixel = inputBitmap.getPixel(x, y)
                input[0][y][x][0] = (0xff - (pixel and 0xff)).toFloat()
            }
        }

        val localModel = FirebaseCustomLocalModel.Builder()
            .setAssetFilePath("mnistOfficial.tflite")
            .build()

        val options = FirebaseModelInterpreterOptions.Builder(localModel).build()
        val interpreter = FirebaseModelInterpreter.getInstance(options)!!

        val inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
            .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 28, 28, 1))
            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 10))
            .build()

        var inputs = FirebaseModelInputs.Builder()
            .add(input)
            .build()

        interpreter.run(inputs, inputOutputOptions)
            .addOnSuccessListener {result ->
                Log.i(TAG, "Prediction has been made")
                var output = result.getOutput<Array<FloatArray>>(0)
                var probabilities = output[0]
                var maxIdx = probabilities.indices.maxBy { probabilities[it] } ?: -1
                textView.text = (maxIdx).toString()
            }
            .addOnFailureListener({e ->
                Log.e(TAG, "exception", e)
            })

        return null
    }

    public fun clear(view: View): Void?{
        canvasView.clear()
        return null
    }
}