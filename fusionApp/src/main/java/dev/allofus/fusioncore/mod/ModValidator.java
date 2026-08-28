package dev.allofus.fusioncore.mod;
import java.io.*; import java.util.*; import java.util.zip.*;
public class ModValidator {
 public static final long MAX_FILE_SIZE=25L*1024L*1024L; private static final long MAX_UNCOMPRESSED=100L*1024L*1024L; private static final int MAX_ENTRIES=2000;
 public boolean isValidFileName(String n){if(n==null)return false;n=new File(n).getName().toLowerCase();return n.endsWith(".dll")||n.endsWith(".cs")||n.endsWith(".zip");}
 public boolean validateStandalone(File f){if(f==null||!f.isFile()||f.length()<=0||f.length()>MAX_FILE_SIZE)return false;if(f.getName().toLowerCase().endsWith(".dll")){try(FileInputStream in=new FileInputStream(f)){return in.read()=='M'&&in.read()=='Z';}catch(Exception e){return false;}}return f.getName().toLowerCase().endsWith(".cs");}
 public boolean validateAndInspectArchive(File f){if(f==null||!f.isFile()||f.length()<=0||f.length()>MAX_FILE_SIZE)return false;try(ZipFile z=new ZipFile(f)){Enumeration<? extends ZipEntry> en=z.entries();int count=0;long total=0;boolean useful=false;Set<String> names=new HashSet<>();while(en.hasMoreElements()){ZipEntry e=en.nextElement();if(++count>MAX_ENTRIES)return false;String n=e.getName().replace('\\','/');if(n.startsWith("/")||n.contains("../")||n.equals(".."))return false;if(!names.add(n))return false;if(e.isDirectory())continue;long s=e.getSize();if(s>MAX_UNCOMPRESSED)return false;if(s>0)total+=s;if(total>MAX_UNCOMPRESSED)return false;String l=n.toLowerCase();if(l.endsWith(".dll")||l.endsWith(".cs")||l.endsWith(".csproj"))useful=true;}return useful;}catch(Exception e){return false;}}
 public boolean validateArchive(File f){return validateAndInspectArchive(f);}
}
