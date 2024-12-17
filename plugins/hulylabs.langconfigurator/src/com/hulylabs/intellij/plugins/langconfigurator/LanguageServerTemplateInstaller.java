// Copyright © 2024 HulyLabs. Use of this source code is governed by the Apache 2.0 license.
package com.hulylabs.intellij.plugins.langconfigurator;

import com.hulylabs.intellij.plugins.langconfigurator.templates.HulyLanguageServerTemplate;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.download.DownloadableFileDescription;
import com.intellij.util.download.DownloadableFileService;
import com.intellij.util.download.FileDownloader;
import com.intellij.util.io.Decompressor;
import com.redhat.devtools.lsp4ij.LanguageServersRegistry;
import com.redhat.devtools.lsp4ij.server.definition.launching.UserDefinedLanguageServerDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tukaani.xz.XZInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class LanguageServerTemplateInstaller {
  private static final Logger LOG = Logger.getInstance(LanguageServerTemplateInstaller.class);

  public static void install(@NotNull Project project,
                             @NotNull HulyLanguageServerTemplate template,
                             @NotNull Runnable onSuccess,
                             @NotNull Runnable onFailure) {
    installBinary(project, template, () -> {
      addTemplate(project, template);
      ApplicationManager.getApplication().invokeLater(onSuccess);
    }, onFailure);
  }

  public static void installBinary(@NotNull Project project,
                                   @NotNull HulyLanguageServerTemplate template,
                                   @NotNull Runnable onSuccess,
                                   @NotNull Runnable onFailure) {
    if (template.installCommand != null) {
      String command = template.installCommand;
      try {
        String[] commandParts = command.split(" ");
        String exePath = PathEnvironmentVariableUtil.findExecutableInWindowsPath(commandParts[0]);
        GeneralCommandLine commandLine =
          new GeneralCommandLine().withExePath(exePath).withParameters(Arrays.stream(commandParts).skip(1).toList())
            .withWorkDirectory(project.getBasePath()).withCharset(StandardCharsets.UTF_8).withRedirectErrorStream(true);
        LOG.info("Install command: " + commandLine.getCommandLineString());
        ProcessHandler processHandler = new OSProcessHandler(commandLine);
        processHandler.addProcessListener(new ProcessAdapter() {
          @Override
          public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            LOG.info(event.getText());
            super.onTextAvailable(event, outputType);
          }

          @Override
          public void processTerminated(@NotNull ProcessEvent event) {
            if (event.getExitCode() != 0) {
              LOG.warn("Install command failed with exit code: " + event.getExitCode());
              ApplicationManager.getApplication().invokeLater(onFailure);
            }
            else {
              ApplicationManager.getApplication().invokeLater(onSuccess);
            }
          }
        });
        processHandler.startNotify();
      }
      catch (ExecutionException e) {
        LOG.warn("Can't execute install server process", e);
        ApplicationManager.getApplication().invokeLater(onFailure);
      }
    }
    else if (template.getBinaryUrl() != null) {
      File directory = new File(PathManager.getConfigPath(), "lsp4ij/" + template.id);
      if (!directory.exists()) {
        directory.mkdirs();
      }
      DownloadableFileService service = DownloadableFileService.getInstance();
      String downloadName;
      try {
        downloadName = new URL(template.getBinaryUrl()).getFile();
      }
      catch (MalformedURLException e) {
        LOG.warn("Can't parse binary url", e);
        ApplicationManager.getApplication().invokeLater(onFailure);
        return;
      }
      DownloadableFileDescription description = service.createFileDescription(template.getBinaryUrl(), downloadName);
      FileDownloader downloader = service.createDownloader(Collections.singletonList(description), downloadName);
      Task.Backgroundable task = new Task.Backgroundable(project, "Downloading") {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          try {
            List<Pair<File, DownloadableFileDescription>> pairs = downloader.download(directory);
            Pair<File, DownloadableFileDescription> first = ContainerUtil.getFirstItem(pairs);
            File file = first != null ? first.first : null;
            if (file != null) {
              decompressBinary(file, directory, template.binaryExecutable);
              ApplicationManager.getApplication().invokeLater(onSuccess);
            }
          }
          catch (IOException e) {
            LOG.warn("Can't download language-server binary", e);
            ApplicationManager.getApplication().invokeLater(onFailure);
          }
        }
      };
      BackgroundableProcessIndicator processIndicator = new BackgroundableProcessIndicator(task);
      processIndicator.setIndeterminate(false);
      ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, processIndicator);
    }
  }

  static void addTemplate(@NotNull Project project, @NotNull HulyLanguageServerTemplate template) {
    String serverId = UUID.randomUUID().toString();

    UserDefinedLanguageServerDefinition definition =
      new UserDefinedLanguageServerDefinition(
        serverId,
        template.name,
        "",
        template.getCommandLine(),
        Collections.emptyMap(),
        false,
        template.settingsJson,
        null,
        template.initializationOptionsJson,
        template.clientSettingsJson
      );
    LanguageServersRegistry.getInstance().addServerDefinition(project, definition, template.getServerMappingSettings());
  }

  static void decompressBinary(File archFile, File directory, @Nullable String binaryExecutable) throws IOException {
    String fileName = archFile.getName().toLowerCase(Locale.ROOT);
    if (fileName.endsWith(".zip")) {
      new Decompressor.Zip(archFile.toPath()).extract(directory.toPath());
    }
    else if (fileName.endsWith(".tar")) {
      new Decompressor.Tar(archFile.toPath()).extract(directory.toPath());
    }
    else if (fileName.endsWith(".gz")) {
      Path outFiled = decompressGzip(archFile.toPath());
      if (outFiled.getFileName().toString().endsWith(".tar")) {
        new Decompressor.Tar(outFiled).extract(directory.toPath());
        FileUtil.delete(outFiled);
      }
      else {
        Path resultFile = directory.toPath().resolve(binaryExecutable != null ? binaryExecutable : outFiled.getFileName().toString());
        Files.move(outFiled, resultFile);
        FileUtil.setExecutable(resultFile.toFile());
      }
    }
    else if (fileName.endsWith(".xz")) {
      Path outFiled = decompressXz(archFile.toPath());
      if (outFiled.getFileName().toString().endsWith(".tar")) {
        new Decompressor.Tar(outFiled).extract(directory.toPath());
        FileUtil.delete(outFiled);
      }
      else {
        Path resultFile = directory.toPath().resolve(binaryExecutable != null ? binaryExecutable : outFiled.getFileName().toString());
        Files.move(outFiled, resultFile);
        FileUtil.setExecutable(resultFile.toFile());
      }
    }
    else {
      throw new IOException("Unsupported package type: " + fileName);
    }
    FileUtil.delete(archFile);
  }

  static Path decompressGzip(Path filePath) throws IOException {
    try (GZIPInputStream gzipInputStream = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(filePath)))) {
      String fileName = filePath.getFileName().toString();
      Path outFileName = filePath.getParent().resolve(fileName.substring(0, fileName.lastIndexOf('.')));
      Files.copy(gzipInputStream, outFileName);
      return outFileName;
    }
  }

  static Path decompressXz(Path filePath) throws IOException {
    try (XZInputStream gzipInputStream = new XZInputStream(new BufferedInputStream(Files.newInputStream(filePath)))) {
      String fileName = filePath.getFileName().toString();
      Path outFileName = filePath.getParent().resolve(fileName.substring(0, fileName.lastIndexOf('.')));
      Files.copy(gzipInputStream, outFileName);
      return outFileName;
    }
  }
}
