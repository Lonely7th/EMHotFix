package com.em.hotfix;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        utils = new Utils();
        String str = utils.getValue();
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
