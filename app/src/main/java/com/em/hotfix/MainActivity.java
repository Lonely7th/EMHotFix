package com.em.hotfix;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private BugUtils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File patchDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/patch/");
        if(!patchDir.exists()){
            patchDir.mkdirs();
        }
        findViewById(R.id.tv_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                funBug();
                Log.d("MainActivity","BugFix");
            }
        });
    }

    private void funBug(){
        android.widget.Toast.makeText(MainActivity.this,utils.getValue(),android.widget.Toast.LENGTH_SHORT).show();
    }
}
