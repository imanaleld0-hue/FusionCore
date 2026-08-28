package dev.allofus.fusioncore.mod;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModProjectManager {
    private static final String PREFS = "mod_projects";
    private static final String KEY_PROJECTS = "projects";
    private final SharedPreferences prefs;
    private final Context context;

    public ModProjectManager(Context context) 
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void createProject(String name, String version, String pluginName) {
        try {
            JSONArray arr = getProjectsJson();
            JSONObject obj = new JSONObject();
            obj.put("id", UUID.randomUUID().toString());
            obj.put("name", name);
            obj.put("version", version);
            obj.put("plugin", pluginName);
        }
        
            File dir = new File(context.getFilesDir(), "projects/" + name);
            dir.mkdirs();
            obj.put("path", dir.getAbsolutePath());

            File cfg = new File(dir, "BepInEx.cfg");
            writeFile(cfg, "[General]\nEnable = true\n");

            File csproj = new File(dir, name + ".csproj");
            String csprojContent = "<Project Sdk=\"Microsoft.NET.Sdk\">\n" +
                    "  <PropertyGroup>\n" +
                    "    <TargetFramework>netstandard2.1</TargetFramework>\n" +
                    "    <LangVersion>latest</LangVersion>\n" +
                    "  </PropertyGroup>\n" +
                    "  <ItemGroup>\n" +
                    "    <Reference Include=\"BepInEx\" />\n" +
                    "    <Reference Include=\"UnityEngine\" />\n" +
                    "  </ItemGroup>\n" +
                    "</Project>";
            writeFile(csproj, csprojContent);

            File cs = new File(dir, pluginName + ".cs");
            String csContent = "using BepInEx;\n\n" +
                    "[BepInPlugin(\"com.allofus." + pluginName + "\", \"" + name + "\", \"" + version + "\")]\n" +
                    "public class " + pluginName + " : BaseUnityPlugin {\n" +
                    "    void Awake() {\n" +
                    "        Logger.LogInfo(\"Loaded!\");\n" +
                    "    }\n" +
                    "}\n";
            writeFile(cs, csContent);

            arr.put(obj);
            save(arr);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void addProject(String name, String path) {
        try {
            JSONArray arr = getProjectsJson();
            JSONObject obj = new JSONObject();
            obj.put("id", UUID.randomUUID().toString());
            obj.put("name", name);
            obj.put("version", "unknown");
            obj.put("plugin", name);
            obj.put("path", path);
            arr.put(obj);
            save(arr);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public List<ModProject> getProjects() {
        List<ModProject> list = new ArrayList<>();
        try {
            JSONArray arr = getProjectsJson();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                ModProject p = new ModProject();
                p.id = o.getString("id");
                p.name = o.optString("name");
                p.version = o.optString("version");
                p.plugin = o.optString("plugin");
                p.path = o.optString("path");
                list.add(p);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public ModProject getProject(String id) {
        for (ModProject p : getProjects()) if (p.id.equals(id)) return p;
        return null;
    }

    private JSONArray getProjectsJson() {
        String s = prefs.getString(KEY_PROJECTS, "[]");
        try { return new JSONArray(s); } catch (Exception e) { return new JSONArray(); }
    }

    private void save(JSONArray arr) {
        prefs.edit().putString(KEY_PROJECTS, arr.toString()).apply();
    }

    private void writeFile(File f, String content) throws Exception {
        FileWriter w = new FileWriter(f); w.write(content); w.close();
    }

    public static class ModProject {
        public String id, name, version, plugin, path;
    }
}
