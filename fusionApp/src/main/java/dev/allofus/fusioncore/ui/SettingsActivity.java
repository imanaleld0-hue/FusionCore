package dev.allofus.fusioncore.ui;
import android.os.Bundle; import android.widget.Toast; import androidx.annotation.Nullable; import androidx.preference.*; import dev.allofus.fusioncore.R;
public class SettingsActivity extends BaseFullscreenActivity {
 @Override protected void onCreate(@Nullable Bundle b){super.onCreate(b);setContentView(R.layout.activity_settings);if(b==null)getSupportFragmentManager().beginTransaction().replace(R.id.settings_container,new SettingsFragment()).commit();}
 public static class SettingsFragment extends PreferenceFragmentCompat {
  private final androidx.activity.result.ActivityResultLauncher<String> export=registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain"),u->{if(u==null)return;try{LogsViewerActivity.exportLogs(requireContext(),u);Toast.makeText(requireContext(),"Logs exported",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(requireContext(),"Export failed: "+e.getMessage(),Toast.LENGTH_LONG).show();}});
  @Override public void onCreatePreferences(Bundle b,String root){setPreferencesFromResource(R.xml.preferences_settings,root);EditTextPreference d=findPreference("pref_plugin_dir");if(d!=null)d.setSummaryProvider(p->{String x=((EditTextPreference)p).getText();return x==null||x.trim().isEmpty()?"Default plugin directory":x;});Preference e=findPreference("pref_export_logs");if(e!=null)e.setOnPreferenceClickListener(x->{export.launch("fusioncore-"+System.currentTimeMillis()+".log");return true;});Preference c=findPreference("pref_clear_cache");if(c!=null)c.setOnPreferenceClickListener(x->{del(requireContext().getCacheDir());Toast.makeText(requireContext(),"Cache cleared",Toast.LENGTH_SHORT).show();return true;});}
  private static void del(java.io.File f){if(f==null||!f.exists())return;if(f.isDirectory()){java.io.File[] a=f.listFiles();if(a!=null)for(java.io.File x:a)del(x);}f.delete();}
 }
}
