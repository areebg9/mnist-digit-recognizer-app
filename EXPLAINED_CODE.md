# Overview

This document will help explain all the code within this repository. This repository contains all files within *.../app/src/main* inside of an Android project - the gradle files and files in higher directories have been omitted.

# Workflow

This project uses Firebase's MLKit as an interface to use with Tensorflow Lite to make a prediction on a user-drawn MNIST digit.  The MNIST model is a machine learning model that takes as input a handwritten-image of a digit, and outputs which digit the model believes it is.

The layout of the app is as follows - there is a canvas for the user to draw a digit, and two buttons beneath, labeled *Classify* and *Clear*. The *Clear* button clears the canvas, and the *Classify* button uses the Tensorflow Lite model to make a prediction on what digit the user has drawn - the output with highest probability is displayed beneath the canvas. 

The steps of production are:
1. Create the model
2. Register our App with Firebase
3. Load the Model and respective settings
4. Create a Custom View for the user's Canvas
5. Take user's drawing as input to model, display output

## Create the Model

To use a model with Tensorflow Lite and Firebase's MLKit, we need to change our model into a ```.tflite``` format so it can be interpreted by the *interpreter* (an *interpreter* is the interface with which a model is controlled in the app).

A link to a Colab Notebook which contains the training and conversion process can be found [here](https://colab.research.google.com/drive/1hHEfP4nm0vuZL0ae3qRY4wRE5729cpAt).

## Register Our App with Firebase

Next, we need to create a Firebase project and link it with our app. The app shown here was created in Android Studio using Kotlin as the primary language. Once the app has been created, simply link the app to the Firebase Project, as shown [here](https://firebase.google.com/docs/android/setup).

## Load the Model and Respective Settings

Now we can get into coding in ```MainActivity.kt```. All the code here can be found at [the official tutorial](https://firebase.google.com/docs/ml-kit/android/use-custom-models), but it will be explained here as well.

First, upload the model (in ```.tflite``` format) to the *Assets* folder in Android Studio (you may need to click *Main -> New -> Folder -> Assets Folder* to create it first)

Once the internet permissions and "no compress tflite" options are added (as documented in the tutorial), we can get on with loading the model. With MLKit, there are 2 options when it comes to loading a model - one can either upload the model to Firebase, or upload the model directly to the project directory. In this application, the model is directly uploaded as to avoid more obfuscated code.

The code presented in this section will reside in ```infer()```, the ```onClick``` method for the *Classify* button.

The process of using a model and its respective settings is
1. Create a model (```FirebaseCustomLocalModel```)
2. Define the options of the model (```FirebaseModelInterpreterOptions```)
3. Define the interpreter (```FirebaseModelInterpreter```)
4. Define the inputs and outputs (```FirebaseModelInputOutputOptions```)
5. TO BE USED LATER: Define the input (```FirebaseModelInputs```)
6. TO BE USED LATER: Run (```FirebaseModelInterpreter.run```)

Steps 5 and 6 will be used at the final step of production.

#### Create a Model
To create a model, run the following code.
```kotlin
val localModel = FirebaseCustomLocalModel.Builder()
    .setAssetFilePath("mnistOfficial.tflite")
    .build()
```
This created a model, loading it from the specified file path.

#### Define the Options of the Model
The options of a model are determined for us - all we have to do is pass in our model.
```kotlin
val options = FirebaseModelInterpreterOptions.Builder(localModel).build()
```

#### Define the Interpreter
The *interpreter*, as mentioned before, is the interface with which we interact with our model. All we have to do are pass in the options.
```kotlin
val interpreter = FirebaseModelInterpreter.getInstance(options)!!
```
The ```!!``` operator is a way (in Kotlin) to make sure than a variable is not null (with the Firebase interface, it's required that ```interpreter``` be non-null).

#### Define the Inputs and Outputs
Now we need to define the ```InputOutputOptions``` - all we need to do is define the shape of our input and output. As shown in the Colab Notebook, the input dimensionality of an image is ```28x28x1``` (or at least, that's how it is for this particular model. If ```x_train``` and ```x_test``` were reshaped differently, to, let's say, ```28x28```, those would be the input dimensions). The output is ```1x10```, representing the probability of the digit for each class.
```kotlin
val inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
    .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 28, 28, 1))
    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 10))
    .build()
```

## Create the Canvas (Custom View)

Now we will create the canvas. Almost all the code can be found in [this tutorial](https://codelabs.developers.google.com/codelabs/advanced-android-kotlin-training-canvas) about canvases, and [this tutorial](https://codelabs.developers.google.com/codelabs/advanced-andoid-kotlin-training-custom-views) about custom views (as part of the Advanced Android Development in Kotlin Course).

We'll start by creating the class. 
```kotlin
class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {}
```
This notation allows us to embed this view within our ```activity_main.xml``` while also giving us the ability to call an object to its class. 

For the sake of brevity, the initializations at the class-level will be skipped (they can still be viewed in [here](main/java/com/example/firebase/CanvasView.kt)) - keep in mind, however, that ```private lateinit var variable: Type``` initializes an empty variable ```variable``` of type ```Type```, to be initialized at a later time.

We'll start by defining our colors. In ```colors.xml``` (found in */res/values*), we have defined a background and foreground color for our canvas. Now let's load them into our code.
```kotlin
private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)
private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)
```
Let's also define a constant called ```touchTolerance``` - this constant holds the value at which a touch (with the intent of drawing) will be distinguished from scrolling.
```kotlin
private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop
```
Let's define our paint as well. 
```kotlin
private val paint = Paint().apply{
    color = drawColor
    isAntiAlias = true
    isDither = true
    style = Paint.Style.STROKE
    strokeJoin = Paint.Join.ROUND
    strokeCap = Paint.Cap.ROUND
    strokeWidth = STROKE_WIDTH
}
```
We have initialized it with the color, anti-aliasing, dithering (a technique to create the illusion of a certain color), the stroke, and a pre-defined stroke width (set at 60f). 

Now let's get into creating our canvas. To create the canvas, we will overlay it with a bitmap - this also allows us to retrieve the pixel values from the bitmap for training in our model.

We'll start by overriding the ```onSizeChanged``` function. This function is called when the size of the view changes - in our case, at the start of the app. The advantage of creating the bitmap here is that the width and height of the view are passed in as parameters.
```kotlin
override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
    super.onSizeChanged(width, height, oldWidth, oldHeight)
    
    if (::extraBitmap.isInitialized) extraBitmap.recycle()
    
    extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    extraCanvas = Canvas(extraBitmap)
    extraCanvas.drawColor(backgroundColor)
}
```
First, we override the function and call ```super.onSizeChanged()``` (the default behavior). Then, we go on to create our bitmap given the width and height (```Bitmap.Config.ARGB_8888``` is the [recommended option](https://developer.android.com/reference/android/graphics/Bitmap.Config#ARGB_8888) for this parameter, as each pixel is stored on 4 bytes). We create the canvas with this bitmap, and also initialize its color to be the background color we defined in the class. The line ```if (::extraBitmap.isInitialized) extraBitmap.recycle()``` recycles any extra bitmaps left over - the reason for this is that incase we decide to change, it does not take up unwanted memory.

Next, we override the ```onDraw``` function.

```kotlin
override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawBitmap(extraBitmap, 0f, 0f, null)
}
```
This simply calls ```super.onDraw()``` (the default behavior), and also draws the bitmap onto the canvas.

Now we can handle user interaction. We start by overriding ```onTouchEvent```.

```kotlin
override fun onTouchEvent(event: MotionEvent): Boolean {
    newX = event.x
    newY = event.y

    when (event.action) {
        MotionEvent.ACTION_DOWN -> touchStart()
        MotionEvent.ACTION_MOVE -> touchMove()
        MotionEvent.ACTION_UP -> touchUp()
    }

    return true
}
```
This code first retrieves the ```x``` and ```y``` coordinates of the user's touch. Then, depending on whether the user put down their finger/cursor (```ACTION_DOWN```), moved their finger/cursor (```ACTION_MOVE```), or taken up their finger/cursor (```ACTION_UP```), a different function (which we will proceed to define) will be called.

The ```touchStart()``` function should start drawing, the ```touchUp()``` function should stop drawing, and the ```touchMove()``` function should continue drawing and change it to respond with the user's touch. Let's first define ```touchUp()``` and ```touchStart()```, as those are the easier 2 of the functions.
