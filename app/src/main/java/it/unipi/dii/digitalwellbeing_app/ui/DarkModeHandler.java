package it.unipi.dii.digitalwellbeing_app.ui;

import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

import it.unipi.dii.digitalwellbeing_app.R;

public class DarkModeHandler implements View.OnClickListener{

    @Override
    public void onClick(View v) {
        SwitchMaterial switchtheme = v.findViewById(R.id.switchtheme);

        if (switchtheme.isChecked()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

    }

}
