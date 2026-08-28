package dev.allofus.fusioncore.ui;
import android.os.Bundle; import android.view.View; import android.view.WindowManager; import androidx.annotation.Nullable; import androidx.appcompat.app.AppCompatActivity; import androidx.core.view.WindowCompat; import androidx.core.view.WindowInsetsCompat; import androidx.core.view.WindowInsetsControllerCompat;
public abstract class BaseFullscreenActivity extends AppCompatActivity {
 @Override protected void onCreate(@Nullable Bundle b){super.onCreate(b);applyFullscreen();}
 @Override protected void onResume(){super.onResume();applyFullscreen();}
 private void applyFullscreen(){boolean fs=getSharedPreferences("fusioncore_settings",MODE_PRIVATE).getBoolean("pref_fullscreen",true); WindowCompat.setDecorFitsSystemWindows(getWindow(),!fs); WindowInsetsControllerCompat c=new WindowInsetsControllerCompat(getWindow(),getWindow().getDecorView()); if(fs){c.hide(WindowInsetsCompat.Type.systemBars());c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);}else{c.show(WindowInsetsCompat.Type.systemBars());getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);}}
}
