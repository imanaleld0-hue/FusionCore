package dev.allofus.fusioncore.log;
import android.content.Context; import android.util.Log; import java.io.*; import java.nio.charset.StandardCharsets; import java.text.SimpleDateFormat; import java.util.*;
public final class FusionLog {
 private static final Object LOCK=new Object(); private FusionLog(){}
 public static File getLogDir(Context c){ File d=c.getExternalFilesDir("logs"); if(d==null)d=new File(c.getFilesDir(),"logs"); if(!d.exists())d.mkdirs(); return d; }
 public static File getLogFile(Context c){return new File(getLogDir(c),"fusioncore.log");}
 public static void write(Context c,String level,String tag,String msg){String line=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).format(new Date())+" "+level+"/"+tag+": "+msg+"\n"; synchronized(LOCK){try(FileOutputStream o=new FileOutputStream(getLogFile(c),true)){o.write(line.getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){}} if("E".equals(level))Log.e(tag,msg);else if("W".equals(level))Log.w(tag,msg);else Log.i(tag,msg);}
 public static void i(Context c,String t,String m){write(c,"I",t,m);} public static void w(Context c,String t,String m){write(c,"W",t,m);} public static void e(Context c,String t,String m){write(c,"E",t,m);}
}
