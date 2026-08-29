package dev.allofus.fusioncore.build;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import dev.allofus.fusioncore.mod.ModProjectManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public class ModBuilder {

    private static final String DOTNET_VERSION = "10.0.400";

    private static final String DOTNET_URL =
            "https://builds.dotnet.microsoft.com/dotnet/Sdk/10.0.400/" +
            "dotnet-sdk-10.0.400-linux-arm64.tar.gz";

    private final Context context;
    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public interface ProgressListener {
        void onProgress(int percent, String message);
        void onComplete(File result);
        void onError(Exception error);
    }

    public ModBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public File build(ModProjectManager.ModProject project)
            throws Exception {

        validateProject(project);

        File dotnet = getDotnetExecutable();

        if (!dotnet.exists()) {
            throw new SdkRequiredException();
        }

        File tempDir = new File(
                context.getCacheDir(),
                "build_" + safeName(project.id)
        );

        if (tempDir.exists()) {
            deleteDir(tempDir);
        }

        if (!tempDir.mkdirs()) {
            throw new IOException(
                    "Cannot create build directory"
            );
        }

        File outputDir = new File(tempDir, "output");

        if (!outputDir.mkdirs()) {
            throw new IOException(
                    "Cannot create output directory"
            );
        }

        File srcDir = new File(
                tempDir,
                "src"
        );

        File projectDir = new File(project.path);

        if (!projectDir.exists()) {
            throw new IOException(
                    "Project does not exist: " +
                    project.path
            );
        }

        copyDir(projectDir, srcDir);

        File csproj = findCsproj(srcDir);

        if (csproj == null) {
            throw new IOException(
                    "No .csproj file found in project"
            );
        }

        File libsDir = new File(
                srcDir,
                "libs"
        );

        if (!libsDir.exists()) {
            libsDir.mkdirs();
        }

        copyAssetsLibs(libsDir);

        File buildLog = new File(
                tempDir,
                "build.log"
        );

        
        List<String> restoreCommand =
                new ArrayList<>();

        restoreCommand.add(dotnet.getAbsolutePath());
        restoreCommand.add("restore");
        restoreCommand.add(csproj.getAbsolutePath());
        restoreCommand.add("--nologo");

        int restoreCode = execute(
                restoreCommand,
                srcDir,
                buildLog
        );

        if (restoreCode != 0) {
            throw new IOException(
                    "dotnet restore failed.\n\n" +
                    readTail(buildLog, 16000)
            );
        }

        
        List<String> buildCommand =
                new ArrayList<>();

        buildCommand.add(dotnet.getAbsolutePath());
        buildCommand.add("build");
        buildCommand.add(csproj.getAbsolutePath());
        buildCommand.add("--configuration");
        buildCommand.add("Release");
        buildCommand.add("--nologo");

        buildCommand.add(
                "-p:OutputPath=" +
                outputDir.getAbsolutePath() +
                File.separator
        );

        buildCommand.add(
                "-p:DebugType=None"
        );

        buildCommand.add(
                "-p:DebugSymbols=false"
        );

        int buildCode = execute(
                buildCommand,
                srcDir,
                buildLog
        );

        if (buildCode != 0) {
            throw new IOException(
                    "dotnet build failed.\n\n" +
                    readTail(buildLog, 16000)
            );
        }

        File dll = findPluginDll(
                outputDir,
                project.plugin
        );

        if (dll == null) {
            throw new IOException(
                    "Build succeeded but plugin DLL " +
                    "was not found.\n\nOutput: " +
                    outputDir.getAbsolutePath()
            );
        }

        File resultDir = new File(
                tempDir,
                "result"
        );

        resultDir.mkdirs();

        File result = new File(
                resultDir,
                sanitizeDllName(project.plugin)
        );

        copyFile(
                dll,
                result
        );

        copyFile(
                buildLog,
                new File(
                        resultDir,
                        "build.log"
                )
        );

        return result;
    }

    
    public void installDotnet(
            ProgressListener listener
    ) {

        new Thread(() -> {

            File archive = new File(
                    context.getCacheDir(),
                    "dotnet-sdk-" +
                    DOTNET_VERSION +
                    "-linux-arm64.tar.gz"
            );

            try {

                downloadDotnet(
                        archive,
                        listener
                );

                notifyProgress(
                        listener,
                        92,
                        "Unzipping .NET SDK..."
                );

                File installDir =
                        getDotnetDirectory();

                if (installDir.exists()) {
                    deleteDir(installDir);
                }

                if (!installDir.mkdirs()) {
                    throw new IOException(
                            "Cannot create .NET directory"
                    );
                }

                extractTarGz(
                        archive,
                        installDir
                );

                notifyProgress(
                        listener,
                        98,
                        "Checking .NET SDK..."
                );

                File dotnet =
                        getDotnetExecutable();

                if (!dotnet.exists()) {
                    throw new IOException(
                            "dotnet executable not found"
                    );
                }

                if (!dotnet.setExecutable(true, false)) {
                Process chmod = new ProcessBuilde(
                    "chmod",
                     "755",
                    dotnet.getAbsolutePath()
                ).start();
                    int exitCode = chmod.waitFor();
                    
                    if (exitCode != 0 || !dotnet.canExecute()) {
                        throw new IOException(
                            "Cannot make dotnet executable: "
                        + dotnet.getAbsolutePath()
                        + ", chmod exit="
                        + exitCode
        );
    }
                }
                verifyDotnet(dotnet);

                if (archive.exists()) {
                    archive.delete();
                }

                notifyProgress(
                        listener,
                        100,
                        ".NET SDK installed"
                );

                if (listener != null) {
                    mainHandler.post(
                            () -> listener.onComplete(dotnet)
                    );
                }

            } catch (Exception e) {

                if (listener != null) {
                    mainHandler.post(
                            () -> listener.onError(e)
                    );
                }
            }

        }).start();
    }

    private void downloadDotnet(
            File destination,
            ProgressListener listener
    ) throws Exception {

        notifyProgress(
                listener,
                0,
                "Connecting to Server..."
        );

        URL url = new URL(DOTNET_URL);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(
                15000
        );

        connection.setReadTimeout(
                30000
        );

        connection.setInstanceFollowRedirects(
                true
        );

        connection.setRequestProperty(
                "User-Agent",
                "FusionCore"
        );

        int response =
                connection.getResponseCode();

        if (response != HttpURLConnection.HTTP_OK) {
            throw new IOException(
                    "Download failed. HTTP " +
                    response
            );
        }

        long total =
                connection.getContentLengthLong();

        long downloaded = 0;

        File parent =
                destination.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        try (
                InputStream raw =
                        new BufferedInputStream(
                                connection.getInputStream()
                        );

                FileOutputStream output =
                        new FileOutputStream(
                                destination
                        )
        ) {

            byte[] buffer =
                    new byte[64 * 1024];

            int count;

            int lastPercent = -1;

            while ((count = raw.read(buffer)) != -1) {

                output.write(
                        buffer,
                        0,
                        count
                );

                downloaded += count;

                if (total > 0) {

                    int percent =
                            (int) (
                                    downloaded *
                                    90L /
                                    total
                            );

                    if (percent != lastPercent) {

                        lastPercent = percent;

                        notifyProgress(
                                listener,
                                percent,
                                "Download .NET SDK..."
                            );
                    }
                }
            }
        } finally {
            connection.disconnect();
        }
    }

    private void extractTarGz(
            File archive,
            File destination
    ) throws Exception {

        try (
                FileInputStream fis =
                        new FileInputStream(archive);

                GZIPInputStream gzip =
                        new GZIPInputStream(
                                new BufferedInputStream(
                                        fis
                                )
                        );

                TarArchiveInputStream tar =
                        new TarArchiveInputStream(
                                gzip
                        )
        ) {

            TarArchiveEntry entry;

            while ((entry = tar.getNextTarEntry())
                    != null) {

                
                String name =
                        entry.getName();

                File target =
                        new File(
                                destination,
                                name
                        );

                String destinationPath =
                        destination
                                .getCanonicalPath();

                String targetPath =
                        target
                                .getCanonicalPath();

                if (!targetPath.equals(
                        destinationPath
                ) && !targetPath.startsWith(
                        destinationPath +
                        File.separator
                )) {
                    throw new IOException(
                            "Unsafe archive entry: " +
                            name
                    );
                }

                if (entry.isDirectory()) {

                    target.mkdirs();

                } else {

                    File parent =
                            target.getParentFile();

                    if (parent != null) {
                        parent.mkdirs();
                    }

                    try (
                            OutputStream out =
                                    new FileOutputStream(
                                            target
                                    )
                    ) {

                        byte[] buffer =
                                new byte[64 * 1024];

                        int count;

                        while ((count =
                                tar.read(buffer)) != -1) {

                            out.write(
                                    buffer,
                                    0,
                                    count
                            );
                        }
                    }

                    if (name.equals("dotnet")
                            || name.endsWith("/dotnet")) {

                        target.setExecutable(
                                true,
                                false
                        );
                    }
                }
            }
        }
    }

    private void verifyDotnet(
            File dotnet
    ) throws Exception {

        List<String> command =
                new ArrayList<>();

        command.add(
                dotnet.getAbsolutePath()
        );

        command.add("--info");

        File log = new File(
                context.getCacheDir(),
                "dotnet_info.log"
        );

        int code =
                execute(
                        command,
                        getDotnetDirectory(),
                        log
                );

        if (code != 0) {
            throw new IOException(
                    "Installed .NET SDK failed verification.\n" +
                    readTail(log, 12000)
            );
        }
    }

    public boolean isDotnetInstalled() {

        File dotnet =
                getDotnetExecutable();

        return dotnet.exists()
                && dotnet.isFile()
                && dotnet.canExecute();
    }

    private File getDotnetDirectory() {
        // Android may mount ordinary app data with noexec. code_cache is the
        // writable location intended for runtime-generated executable code.
        return new File(
                context.getCodeCacheDir(),
                "fusion-dotnet/" + DOTNET_VERSION
        );
    }

    private File getDotnetExecutable() {

        return new File(
                getDotnetDirectory(),
                "dotnet"
        );
    }

    private int execute(
            List<String> command,
            File workingDirectory,
            File logFile
    ) throws Exception {

        ProcessBuilder builder =
                new ProcessBuilder(command);

        builder.directory(
                workingDirectory
        );

        builder.redirectErrorStream(
                true
        );

        /*
         * Environment для embedded .NET.
         */
        File dotnetDir =
                getDotnetDirectory();

        builder.environment().put(
                "DOTNET_ROOT",
                dotnetDir.getAbsolutePath()
        );

        builder.environment().put(
                "DOTNET_ROOT_ARM64",
                dotnetDir.getAbsolutePath()
        );

        builder.environment().put(
                "DOTNET_CLI_TELEMETRY_OPTOUT",
                "1"
        );

        builder.environment().put(
                "DOTNET_NOLOGO",
                "1"
        );

        builder.environment().put(
                "DOTNET_CLI_HOME",
                new File(
                        context.getFilesDir(),
                        "dotnet-home"
                ).getAbsolutePath()
        );

        File cliHome =
                new File(
                        context.getFilesDir(),
                        "dotnet-home"
                );

        cliHome.mkdirs();

        try (
                FileOutputStream logOut =
                        new FileOutputStream(
                                logFile
                        )
        ) {

            Process process =
                    builder.start();

            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            process.getInputStream()
                                    )
                            )
            ) {

                String line;

                while ((line =
                        reader.readLine()) != null) {

                    String text =
                            line + "\n";

                    logOut.write(
                            text.getBytes("UTF-8")
                    );

                    logOut.flush();
                }
            }

            return process.waitFor();
        }
    }

    private File findCsproj(
            File directory
    ) {

        File[] files =
                directory.listFiles();

        if (files == null) {
            return null;
        }

        for (File file : files) {

            if (file.isFile()
                    && file.getName()
                    .toLowerCase()
                    .endsWith(".csproj")) {

                return file;
            }
        }

        for (File file : files) {

            if (file.isDirectory()) {

                File result =
                        findCsproj(file);

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private File findPluginDll(
            File directory,
            String pluginName
    ) {

        if (!directory.exists()) {
            return null;
        }

        String expected =
                sanitizeDllName(pluginName)
                        .toLowerCase();

        File exact = null;
        if (exact != null) {
            return exact;
        }

        File[] files =
                directory.listFiles();

        if (files == null) {
            return null;
        }

        for (File file : files) {

            if (file.isFile()
                    && file.getName()
                    .toLowerCase()
                    .endsWith(".dll")) {

                return file;
            }
        }
        

        
        for (File file : files) {

            if (file.isDirectory()) {

                File result =
                        findPluginDll(
                                file,
                            pluginName
                        );

                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private void copyAssetsLibs(
            File dest
    ) throws Exception {

        File libs =
                new File(
                        context.getFilesDir(),
                        "libs"
                );

        if (!libs.exists()) {
            return;
        }

        File[] files =
                libs.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isFile()
                    && file.getName()
                    .toLowerCase()
                    .endsWith(".dll")) {

                copyFile(
                        file,
                        new File(
                                dest,
                                file.getName()
                        )
                );
            }
        }
    }

    private void copyFile(
            File source,
            File destination
    ) throws Exception {

        File parent =
                destination.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        try (
                FileInputStream in =
                        new FileInputStream(source);

                FileOutputStream out =
                        new FileOutputStream(
                                destination
                        )
        ) {

            byte[] buffer =
                    new byte[64 * 1024];

            int count;

            while ((count =
                    in.read(buffer)) != -1) {

                out.write(
                        buffer,
                        0,
                        count
                );
            }
        }
    }

    private void copyDir(
            File source,
            File destination
    ) throws Exception {

        if (!destination.exists()) {
            destination.mkdirs();
        }

        File[] files =
                source.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            File target =
                    new File(
                            destination,
                            file.getName()
                    );

            if (file.isDirectory()) {

                copyDir(
                        file,
                        target
                );

            } else {

                copyFile(
                        file,
                        target
                );
            }
        }
    }

    private void deleteDir(
            File dir
    ) {

        File[] files =
                dir.listFiles();

        if (files != null) {

            for (File file : files) {

                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }
        }

        dir.delete();
    }

    private void zipDirectory(
            File sourceDir,
            File zipFile
    ) throws Exception {

        File parent =
                zipFile.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        try (
                FileOutputStream fos =
                        new FileOutputStream(zipFile);

                ZipOutputStream zos =
                        new ZipOutputStream(fos)
        ) {

            zipRecursive(
                    sourceDir,
                    "",
                    zos
            );
        }
    }

    private void zipRecursive(
            File directory,
            String prefix,
            ZipOutputStream zos
    ) throws Exception {

        File[] files =
                directory.listFiles();

        if (files == null) {
            return;
        }

        for (File file : files) {

            if (file.isDirectory()) {

                zipRecursive(
                        file,
                        prefix +
                        file.getName() +
                        "/",
                        zos
                );

            } else {

                ZipEntry entry =
                        new ZipEntry(
                                prefix +
                                file.getName()
                        );

                zos.putNextEntry(entry);

                try (
                        FileInputStream in =
                                new FileInputStream(file)
                ) {

                    byte[] buffer =
                            new byte[64 * 1024];

                    int count;

                    while ((count =
                            in.read(buffer)) != -1) {

                        zos.write(
                                buffer,
                                0,
                                count
                        );
                    }
                }

                zos.closeEntry();
            }
        }
    }

    private void validateProject(
            ModProjectManager.ModProject project
    ) throws IOException {

        if (project == null) {
            throw new IOException(
                    "Project is null"
            );
        }

        if (project.path == null
                || project.path.trim().isEmpty()) {

            throw new IOException(
                    "Project path is empty"
            );
        }

        if (project.plugin == null
                || project.plugin.trim().isEmpty()) {

            throw new IOException(
                    "Plugin name is empty"
            );
        }
    }

    private String sanitizeDllName(
            String name
    ) {

        String result =
                sanitizeFileName(name);

        if (!result
                .toLowerCase()
                .endsWith(".dll")) {

            result += ".dll";
        }

        return result;
    }

    private String sanitizeFileName(
            String name
    ) {

        if (name == null
                || name.trim().isEmpty()) {

            return "plugin";
        }

        return name.replaceAll(
                "[\\\\/:*?\"<>|]",
                "_"
        ).trim();
    }

    private String safeName(
            Object value
    ) {

        return sanitizeFileName(
                String.valueOf(value)
        );
    }

    private void notifyProgress(
            ProgressListener listener,
            int percent,
            String message
    ) {

        if (listener == null) {
            return;
        }

        mainHandler.post(
                () -> listener.onProgress(
                        percent,
                        message
                )
        );
    }

    private String readTail(
            File file,
            int maxChars
    ) {

        if (!file.exists()) {
            return "Build log not found.";
        }

        try {

            StringBuilder result =
                    new StringBuilder();

            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new FileReader(file)
                            )
            ) {

                String line;

                while ((line =
                        reader.readLine()) != null) {

                    result.append(line)
                            .append('\n');

                    if (result.length()
                            > maxChars) {

                        result.delete(
                                0,
                                result.length()
                                        - maxChars
                        );
                    }
                }
            }

            return result.toString();

        } catch (Exception e) {

            return "Cannot read build log: " +
                    e.getMessage();
        }
    }

    public File exportToZip(
            ModProjectManager.ModProject project
    ) throws Exception {

        File buildResult =
                build(project);

        File zipFile =
                new File(
                        context.getFilesDir(),
                        "exports/" +
                        sanitizeFileName(project.name) +
                        ".zip"
                );

        File exportDir =
                buildResult.getParentFile();

        zipDirectory(
                exportDir,
                zipFile
        );

        return zipFile;
    }

    public static class SdkRequiredException
            extends IOException {

        public SdkRequiredException() {
            super(
                    "FusionCore requires .NET SDK " +
                    DOTNET_VERSION
            );
        }
    }
    }
