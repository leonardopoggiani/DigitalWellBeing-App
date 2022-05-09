package it.unipi.dii.digitalwellbeing_app;

import android.content.Context;

import org.tensorflow.lite.DataType;


import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.io. * ;
import java.util.Scanner;

import it.unipi.dii.digitalwellbeing_app.ml.PickupClassifier;

public class Classifier {

     private PickupClassifier model;
     private Context context;

    public Classifier(Context ctx) throws IOException {

       this.context = ctx;
       this.model = PickupClassifier.newInstance(ctx);


    }

    public void classify(ByteBuffer byteBuffer){

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 4}, DataType.FLOAT32);
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            PickupClassifier.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Releases model resources if no longer used.
            model.close();


    }


}
/*
try {
        Model model = Model.newInstance(context);

        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 4}, DataType.FLOAT32);
        inputFeature0.loadBuffer(byteBuffer);

        // Runs model inference and gets result.
        Model.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        // Releases model resources if no longer used.
        model.close();
        } catch (IOException e) {
        // TODO Handle the exception
        }
*/