
package dev.allofus.fusioncore.mod;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import org.json.JSONObject;
import dev.allofus.fusioncore.util.FusionCoreLogger;

public class ModProjectManager {
    private static final String TAG = "ModProjectManager";

    public static boolean createNewModProject(Context context, String modName, String version, String pluginName) {
        try {
            File rootDir = new File(context.getExternalFilesDir(null), "ModProjects");
            File modDir = new File(rootDir, modName);
            if (!modDir.exists()) modDir.mkdirs();

            File csproj = new File(modDir, modName + ".csproj");
            String csprojContent = "<Project Sdk=\"Microsoft.NET.Sdk\">\n" +
                    "  <PropertyGroup>\n" +
                    "    <TargetFramework>netstandard2.1</TargetFramework>\n" +
                    "    <AssemblyName>" + pluginName + "</AssemblyName>\n" +
                    "    <Version>" + version + "</Version>\n" +
                    "  </PropertyGroup>\n" +
                    "</Project>";

            try (FileWriter writer = new FileWriter(csproj)) {
                writer.write(csprojContent);
            }

            File configJson = new File(modDir, "bepInExConfig.json");
            JSONObject json = new JSONObject();
            json.put("name", modName);
            json.put("version", version);
            json.put("pluginType", "BepInEx");

            try (FileWriter writer = new FileWriter(configJson)) {
                writer.write(json.toString(4));
            }

            FusionCoreLogger.i(TAG, "Created new mod project: " + modName);
            return true;
        } catch (Exception e) {
            FusionCoreLogger.e(TAG, "Failed to create mod project", e);
            return false;
        }
    }
}
