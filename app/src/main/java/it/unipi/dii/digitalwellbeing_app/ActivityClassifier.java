package it.unipi.dii.digitalwellbeing_app;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
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

    Boolean classifySamples(Float[] toClassify, TreeMap<Long, Float[]> toBeClassified) {
        // classify the samples
        Boolean pickup = false;
        TensorBuffer inputFeature0 = null;
        float[] data = new float[18];

        try {
            PickupClassifier model = PickupClassifier.newInstance(ctx.getApplicationContext());
            for (Map.Entry<Long, Float[]> entry : toBeClassified.entrySet()) {
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

                // TextView tv = findViewById(R.id.activity);
                // TextView tv2 = findViewById(R.id.counter);

                // tv.setText(outputFeature0.getDataType().toString());
                if (data[0] > 0.5) {

                    // tv.setText("Picking up phone!");
                    // CharSequence counter = tv2.getText();
                    // int count = Integer.parseInt(counter.toString());

                    if(!already_recognized) {
                        pickup = true;
                        // count += 1;
                    }

                    // tv2.setText(String.valueOf(count));
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
            e.printStackTrace();
        }
        return pickup;
    }
}
