package it.unipi.dii.digitalwellbeing_app;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.TreeMap;

import it.unipi.dii.digitalwellbeing_app.ml.PickupClassifier;


public class ActivityClassifier {

    private static final String TAG = "PickupClassifier";
    private Context ctx;
    static boolean pocket = false;

    public ActivityClassifier(Context context){
        this.ctx = context;
    }

    boolean classifySamples(Float[] toClassify, TreeMap<Long, Float[]> toBeClassified) {
        // classify the samples
        boolean pickup = false;
        TensorBuffer inputFeature0 = null;
        float[] data = new float[18];

        try {
            PickupClassifier model = PickupClassifier.newInstance(ctx.getApplicationContext());

            int[] shape = new int[]{1, 18};
            TensorBuffer tensorBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32);

            for (int i = 0; i < toClassify.length; i++) {
                data[i] = toClassify[i];
            }

            tensorBuffer.loadArray(data);

            inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 18, 1}, DataType.FLOAT32);
            ByteBuffer byteBuffer = tensorBuffer.getBuffer();
            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            PickupClassifier.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            data = outputFeature0.getFloatArray();

            if(!pocket) {
                pocket = (data[0] > 0.85) && (SensorHandler.already_recognized);
            }

            if(pocket && !SensorHandler.already_recognized) {
                pickup = true;
                pocket = false;
            }

            Log.d(TAG, "predictActivities: output array: " + Arrays.toString(outputFeature0.getFloatArray()));

            // Releases model resources if no longer used.
            model.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return pickup;
    }
}
