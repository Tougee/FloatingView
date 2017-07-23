package com.touge.floatingview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.touge.floatingview.floating.BubbleService;

public class MainActivity extends AppCompatActivity {

  private static final int REQUEST_CODE_HOVER_PERMISSION = 1000;
  private static final int REQUEST_CAMERA_PERMISSION = 1;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        BubbleService.showBubble(MainActivity.this);
      }
    });

    if (!OverlayPermission.hasRuntimePermissionToDrawOverlay(this)) {
      @SuppressWarnings("NewApi") Intent myIntent =
          OverlayPermission.createIntentToRequestOverlayPermission(this);
      startActivityForResult(myIntent, REQUEST_CODE_HOVER_PERMISSION);
    }
    if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED)) {
      ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
          REQUEST_CAMERA_PERMISSION);
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (REQUEST_CODE_HOVER_PERMISSION == requestCode) {
    } else if (REQUEST_CAMERA_PERMISSION == requestCode) {
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
