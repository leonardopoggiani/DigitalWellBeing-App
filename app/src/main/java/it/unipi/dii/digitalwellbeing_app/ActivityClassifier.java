package it.unipi.dii.digitalwellbeing_app;

import android.hardware.SensorEvent;
import android.util.Log;
import android.widget.TextView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import android.content.Context;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import it.unipi.dii.digitalwellbeing_app.ml.PickupClassifier;


public class ActivityClassifier {

    private static final String TAG = "PickupClassifier";
    long timestamp;
    boolean already_recognized = false;
    private Context ctx;
    TreeMap<Long,Float[]> toBeClassified = new TreeMap<>();

    public ActivityClassifier(Context context){
        this.ctx = context;
    }

    void classifySamples(Float[] toClassify) {
        // classify the samples
        TensorBuffer inputFeature0 = null;
        float[] data = new float[18];

        try {
            PickupClassifier model = PickupClassifier.newInstance(ctx);
            for (Map.Entry<Long, Float[]> entry : toBeClassified.entrySet()) {
                Log.d(TAG, "rowString length: " + (entry.getValue() != null ? entry.getValue().length : 0));

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

                // Releases model resources if no longer used.
                /*

                TextView tv = findViewById(R.id.activity);
                TextView tv2 = findViewById(R.id.counter);

                 */
                if (data[0] <= 0.5) {
                    // tv.setText("Picking up phone!");
                    // CharSequence counter = tv2.getText();
                    // int count = Integer.parseInt(counter.toString());
                    // count += 1;
                    // tv2.setText(String.valueOf(count));
                    already_recognized = true;
                } else {
                    // tv.setText("Other activities");
                }

                Log.d(TAG, "predictActivities: output array: " + Arrays.toString(outputFeature0.getFloatArray()));
                break;
            }
            toBeClassified.clear();
            // Releases model resources if no longer used.
            model.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    /*Boolean classifySamples(TreeMap<Long, Float[]> toBeClassified) {
        // classify the samples
        Boolean pickup = false;
        TensorBuffer inputFeature0 = null;
        float[] data = new float[12];

        Log.d(TAG, String.valueOf(toBeClassified.size()));
        try {
            PickupClassifier model = PickupClassifier.newInstance(ctx.getApplicationContext());
            for (Map.Entry<Long, Float[]> entry : toBeClassified.entrySet()) {
                Log.d(TAG, "rowString length: " + (entry.getValue() != null ? entry.getValue().length : 0));

                int[] shape = new int[]{1, 12};
                TensorBuffer tensorBuffer = TensorBuffer.createFixedSize(shape, DataType.FLOAT32);

                for (int i = 0; i < entry.getValue().length; i++) {
                    data[i] = entry.getValue()[i];
                }

                tensorBuffer.loadArray(data);

                inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 12, 1}, DataType.FLOAT32);
                ByteBuffer byteBuffer = tensorBuffer.getBuffer();
                inputFeature0.loadBuffer(byteBuffer);

                // Runs model inference and gets result.
                PickupClassifier.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                data = outputFeature0.getFloatArray();


                // tv.setText(outputFeature0.getDataType().toString());
                if (data[0] <= 0.5) {

                    already_recognized = true;
                } else {
                    pickup = true;
                    // tv.setText("Other activities");
                }

                Log.d(TAG, "predictActivities: output array: " + Arrays.toString(outputFeature0.getFloatArray()));
                break;
            }

            // Releases model resources if no longer used.
            model.close();


        } catch (IOException e) {
            // TODO Handle the exception
        }
        Log.d(TAG, "PICKUP");
        Log.d(TAG, String.valueOf(pickup));
        return pickup;
    }*/
}
