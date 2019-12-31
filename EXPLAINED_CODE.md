# Overview

This document will help explain all the code within this repository. This repository contains all files within *.../app/src/main* inside of an Android project - the gradle files and files in higher directories have been omitted.

# Workflow

This project uses Firebase's MLKit as an interface to use with Tensorflow Lite to make a prediction on a user-drawn MNIST digit. 

The layout of the app is as follows - there is a canvas for the user to draw a digit, and two buttons beneath, labeled *Classify* and *Clear*. The *Clear* button clears the canvas, and the *Classify* button uses the Tensorflow Lite model to make a prediction on what digit the user has drawn - the output with highest probability is displayed beneath the canvas. 

The steps of production are:
1. Create the model
2. Register our App with Firebase
3. Load the Model and respective settings
4. Create a Custom View for the user's Canvas
5. Take user's drawing as input to model, display output

## Create the Model

To use a model with Tensorflow Lite and Firebase's MLKit, we need to change our model into a ```.tflite``` format so it can be interpreted by the *interpreter* (an *interpreter* is the interface with which a model is controlled in the app).

A link to a Colab Notebook which contains the training and conversion process can be found [here]().

## Register Our App with Firebase

Next, we need to create a Firebase project and link it with our app. The app shown here was created in Android Studio using Kotlin as the primary language. Once the app has been created, simply link the app to the Firebase Project, as shown [here](https://firebase.google.com/docs/android/setup).

## Load the Model and Respective Settings

Now we can get into coding in ```MainActivity.kt```. All the code here can be found at [the official tutorial](https://firebase.google.com/docs/ml-kit/android/use-custom-models), but it will be explained here as well.

Once the internet permissions and "no compress tflite" options are added (as documented in the tutorial), we can get on with loading the model. With MLKit, there are 2 options when it comes to loading a model - one can either upload the model to Firebase, or upload the model directly to the project directory. In this application, the model is directly uploaded as to avoid more obfuscated code.

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
Now we need to 
```kotlin
val inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
    .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 28, 28, 1))
    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 10))
    .build()
```
