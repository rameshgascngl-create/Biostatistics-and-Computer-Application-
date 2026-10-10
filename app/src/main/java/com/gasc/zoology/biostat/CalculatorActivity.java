package com.gasc.zoology.biostat;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;

public class CalculatorActivity extends AppCompatActivity {
    private EditText inputData;
    private TextView resultView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        inputData = findViewById(R.id.inputData);
        resultView = findViewById(R.id.resultView);
        Button calcBtn = findViewById(R.id.calcBtn);

        calcBtn.setOnClickListener(v -> calculate());
    }

    private void calculate() {
        String raw = inputData.getText().toString().trim();
        if (raw.isEmpty()) {
            Toast.makeText(this, "Enter numbers separated by commas", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String[] parts = raw.split(",");
            double[] data = new double[parts.length];
            double sum = 0;
            for (int i = 0; i < parts.length; i++) {
                data[i] = Double.parseDouble(parts[i].trim());
                sum += data[i];
            }
            double mean = sum / data.length;
            double var = 0;
            for (double d : data) var += (d - mean) * (d - mean);
            double sd = Math.sqrt(var / (data.length - 1));

            String result = String.format(
                "n = %d\nMean = %.3f\nSample SD = %.3f\nSum = %.3f",
                data.length, mean, sd, sum);
            resultView.setText(result);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid numbers. Use commas only.", Toast.LENGTH_LONG).show();
        }
    }
}
