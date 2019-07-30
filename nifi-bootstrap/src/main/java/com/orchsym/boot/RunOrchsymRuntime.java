/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orchsym.boot;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.bootstrap.RunNiFi;
import org.apache.nifi.bootstrap.ShutdownHook;
import org.apache.nifi.bootstrap.notification.NotificationType;
import org.apache.nifi.bootstrap.util.OSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orchsym.branding.BrandingExtension;
import com.orchsym.branding.BrandingService;
import com.orchsym.util.OrchsymProperties;

/**
 * 
 * @author GU Guoqiang
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class RunOrchsymRuntime extends RunNiFi {
    public static String RUNTIME_NAME = BrandingExtension.get().getProductName();

    private final FilenameFilter jarFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String filename) {
            return filename.toLowerCase().endsWith(".jar");
        }
    };

    protected final static File configFile = getDefaultBootstrapConfFile();

    public RunOrchsymRuntime() throws IOException {
        super(configFile);

        this.cmdLogger = LoggerFactory.getLogger("com.orchsym.bootstrap.Command");
        this.defaultLogger = LoggerFactory.getLogger(RunOrchsymRuntime.class);
        this.loggingExecutor = Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable runnable) {
                final Thread t = Executors.defaultThreadFactory().newThread(runnable);
                t.setDaemon(true);
                t.setName(RUNTIME_NAME + " logging handler");
                return t;
            }
        });
        this.serviceManager = loadServices();
    }

    public void setAutoRestartNiFi(final boolean restart) {
        super.setAutoRestartNiFi(restart);
    }

    public File getStatusFile() throws IOException {
        return super.getStatusFile();
    }

    public int getNiFiCommandControlPort() {
        return super.getNiFiCommandControlPort();
    }

    /**
     * Only replace the "Apache NiFi" to RUNTIME_NAME
     */
    protected void setNiFiCommandControlPort(final int port, final String secretKey) throws IOException {
        this.ccPort = port;
        this.secretKey = secretKey;

        if (shutdownHook != null) {
            shutdownHook.setSecretKey(secretKey);
        }

        final File statusFile = getStatusFile(defaultLogger);

        final Properties nifiProps = new Properties();
        if (nifiPid != -1) {
            nifiProps.setProperty(PID_KEY, String.valueOf(nifiPid));
        }
        nifiProps.setProperty("port", String.valueOf(ccPort));
        nifiProps.setProperty("secret.key", secretKey);

        try {
            savePidProperties(nifiProps, defaultLogger);
        } catch (final IOException ioe) {
            defaultLogger.warn(RUNTIME_NAME + " has started but failed to persist platform Port information to {} due to {}", new Object[] { statusFile.getAbsolutePath(), ioe });
        }

        defaultLogger.info(RUNTIME_NAME + " now running and listening for Bootstrap requests on port {}", port);
    }

    /**
     * Only replace the "Apache NiFi" to RUNTIME_NAME
     */
    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println();
        System.out.println("java " + RunOrchsymRuntime.class.getName() + " [<-verbose>] <command> [options]");
        System.out.println();
        System.out.println("Valid commands include:");
        System.out.println("");
        System.out.println("Start : Start a new instance of " + RUNTIME_NAME);
        System.out.println("Stop : Stop a running instance of " + RUNTIME_NAME);
        System.out.println("Restart : Stop " + RUNTIME_NAME + ", if it is running, and then start a new instance");
        System.out.println("Status : Determine if there is a running instance of " + RUNTIME_NAME);
        System.out.println("Dump : Write a Thread Dump to the file specified by [options], or to the log if no file is given");
        System.out.println("Run : Start a new instance of " + RUNTIME_NAME + " and monitor the Process, restarting if the instance dies");
        System.out.println();
    }

    /**
     * Only replace the "Apache NiFi" to RUNTIME_NAME
     */
    @Override
    public int status() throws IOException {
        final Logger logger = cmdLogger;
        final Status status = getStatus(logger);
        if (status.isRespondingToPing()) {
            logger.info(RUNTIME_NAME + " is currently running, listening to Bootstrap on port {}, PID={}", new Object[] { status.getPort(), status.getPid() == null ? "unknown" : status.getPid() });
            return 0;
        }

        if (status.isProcessRunning()) {
            logger.info(RUNTIME_NAME + " is running at PID {} but is not responding to ping requests", status.getPid());
            return 4;
        }

        if (status.getPort() == null) {
            logger.info(RUNTIME_NAME + " is not running");
            return 3;
        }

        if (status.getPid() == null) {
            logger.info(RUNTIME_NAME + " is not responding to Ping requests. The process may have died or may be hung");
        } else {
            logger.info(RUNTIME_NAME + " is not running");
        }
        return 3;
    }

    /**
     * Only replace the "Apache NiFi" to RUNTIME_NAME
     */
    @Override
    public void stop() throws IOException {
        final Logger logger = cmdLogger;
        final Integer port = getCurrentPort(logger);
        if (port == null) {
            logger.info(RUNTIME_NAME + " is not currently running");
            return;
        }

        // indicate that a stop command is in progress
        final File lockFile = getLockFile(logger);
        if (!lockFile.exists()) {
            lockFile.createNewFile();
        }

        final Properties nifiProps = loadProperties(logger);
        final String secretKey = nifiProps.getProperty("secret.key");
        final String pid = nifiProps.getProperty(PID_KEY);
        final File statusFile = getStatusFile(logger);
        final File pidFile = getPidFile(logger);

        try (final Socket socket = new Socket()) {
            logger.debug("Connecting to " + RUNTIME_NAME + " instance");
            socket.setSoTimeout(10000);
            socket.connect(new InetSocketAddress("localhost", port));
            logger.debug("Established connection to " + RUNTIME_NAME + " instance.");
            socket.setSoTimeout(10000);

            logger.debug("Sending SHUTDOWN Command to port {}", port);
            final OutputStream out = socket.getOutputStream();
            out.write((SHUTDOWN_CMD + " " + secretKey + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            socket.shutdownOutput();

            final InputStream in = socket.getInputStream();
            int lastChar;
            final StringBuilder sb = new StringBuilder();
            while ((lastChar = in.read()) > -1) {
                sb.append((char) lastChar);
            }
            final String response = sb.toString().trim();

            logger.debug("Received response to SHUTDOWN command: {}", response);

            if (SHUTDOWN_CMD.equals(response)) {
                logger.info(RUNTIME_NAME + " has accepted the Shutdown Command and is shutting down now");

                if (pid != null) {
                    final Properties bootstrapProperties = new Properties();
                    try (final FileInputStream fis = new FileInputStream(bootstrapConfigFile)) {
                        bootstrapProperties.load(fis);
                    }

                    String gracefulShutdown = bootstrapProperties.getProperty(GRACEFUL_SHUTDOWN_PROP, DEFAULT_GRACEFUL_SHUTDOWN_VALUE);
                    int gracefulShutdownSeconds;
                    try {
                        gracefulShutdownSeconds = Integer.parseInt(gracefulShutdown);
                    } catch (final NumberFormatException nfe) {
                        gracefulShutdownSeconds = Integer.parseInt(DEFAULT_GRACEFUL_SHUTDOWN_VALUE);
                    }

                    notifyStop();
                    final long startWait = System.nanoTime();
                    while (isProcessRunning(pid, logger)) {
                        logger.info("Waiting for " + RUNTIME_NAME + " to finish shutting down...");
                        final long waitNanos = System.nanoTime() - startWait;
                        final long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(waitNanos);
                        if (waitSeconds >= gracefulShutdownSeconds && gracefulShutdownSeconds > 0) {
                            if (isProcessRunning(pid, logger)) {
                                logger.warn(RUNTIME_NAME + " has not finished shutting down after {} seconds. Killing process.", gracefulShutdownSeconds);
                                try {
                                    killProcessTree(pid, logger);
                                } catch (final IOException ioe) {
                                    logger.error("Failed to kill Process with PID {}", pid);
                                }
                            }
                            break;
                        } else {
                            try {
                                Thread.sleep(2000L);
                            } catch (final InterruptedException ie) {
                            }
                        }
                    }

                    if (statusFile.exists() && !statusFile.delete()) {
                        logger.error("Failed to delete status file {}; this file should be cleaned up manually", statusFile);
                    }

                    if (pidFile.exists() && !pidFile.delete()) {
                        logger.error("Failed to delete pid file {}; this file should be cleaned up manually", pidFile);
                    }

                    logger.info(RUNTIME_NAME + " has finished shutting down.");
                }
            } else {
                logger.error("When sending SHUTDOWN command to " + RUNTIME_NAME + ", got unexpected response {}", response);
            }
        } catch (final IOException ioe) {
            if (pid == null) {
                logger.error("Failed to send shutdown command to port {} due to {}. No PID found for the " + RUNTIME_NAME + " process, so unable to kill process; "
                        + "the process should be killed manually.", new Object[] { port, ioe.toString() });
            } else {
                logger.error("Failed to send shutdown command to port {} due to {}. Will kill the " + RUNTIME_NAME + " Process with PID {}.", port, ioe.toString(), pid);
                notifyStop();
                killProcessTree(pid, logger);
                if (statusFile.exists() && !statusFile.delete()) {
                    logger.error("Failed to delete status file {}; this file should be cleaned up manually", statusFile);
                }
            }
        } finally {
            if (lockFile.exists() && !lockFile.delete()) {
                logger.error("Failed to delete lock file {}; this file should be cleaned up manually", lockFile);
            }
        }
    }

    protected void addProperty(List<String> cmd, String propKey, Object value) {
        if (value != null) {
            String str = value.toString();
            if (!str.trim().isEmpty())
                cmd.add("-D" + propKey + "=" + value);
        }
    }

    protected void addAddtionsCmds(List<String> cmd) {
        //
    }

    protected String getRuntimeApp() {
        return "com.orchsym.OrchsymRuntime";
    }

    /**
     * replace the "Apache NiFi" to RUNTIME_NAME, and RuntimeListener, also set one property for lic, add ext lib
     */
    @Override
    public void start() throws IOException, InterruptedException {
        final Integer port = getCurrentPort(cmdLogger);
        if (port != null) {
            cmdLogger.info(RUNTIME_NAME + " is already running, listening to Bootstrap on port " + port);
            return;
        }

        final File prevLockFile = getLockFile(cmdLogger);
        if (prevLockFile.exists() && !prevLockFile.delete()) {
            cmdLogger.warn("Failed to delete previous lock file {}; this file should be cleaned up manually", prevLockFile);
        }

        final ProcessBuilder builder = new ProcessBuilder();

        if (!bootstrapConfigFile.exists()) {
            throw new FileNotFoundException(bootstrapConfigFile.getAbsolutePath());
        }

        final Properties properties = new Properties();
        try (final FileInputStream fis = new FileInputStream(bootstrapConfigFile)) {
            properties.load(fis);
        }

        final Map<String, String> props = new HashMap<>();
        props.putAll((Map) properties);

        final String specifiedWorkingDir = props.get("working.dir");
        if (specifiedWorkingDir != null) {
            builder.directory(new File(specifiedWorkingDir));
        }

        final File bootstrapConfigAbsoluteFile = bootstrapConfigFile.getAbsoluteFile();
        final File workingDir = bootstrapConfigAbsoluteFile.getParentFile().getParentFile();

        if (specifiedWorkingDir == null) {
            builder.directory(workingDir);
        }

        final String nifiLogDir = replaceNull(System.getProperty(OrchsymProperties.NIFI_BOOTSTRAP_LOG_DIR), DEFAULT_LOG_DIR).trim();

        final String libFilename = replaceNull(props.get("lib.dir"), "./lib").trim();
        File libDir = getFile(libFilename, workingDir);

        final String confFilename = replaceNull(props.get("conf.dir"), OrchsymProperties.CONF_DIR).trim();
        File confDir = getFile(confFilename, workingDir);

        String nifiPropsFilename = props.get("props.file");
        if (nifiPropsFilename == null) {
            if (confDir.exists()) {
                nifiPropsFilename = new File(confDir, OrchsymProperties.FILE_NAME).getAbsolutePath();
            } else {
                nifiPropsFilename = DEFAULT_CONFIG_FILE;
            }
        }

        nifiPropsFilename = nifiPropsFilename.trim();

        final List<String> javaAdditionalArgs = new ArrayList<>();
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();

            if (key.startsWith("java.arg")) {
                javaAdditionalArgs.add(value);
            }
        }

        File[] libFiles = libDir.listFiles(jarFilter);

        final File extLibDir = new File(libDir, "ext"); // ext lib
        if (extLibDir.exists()) {
            final List<File> extLibFiles = retrieveExtLibs(extLibDir);
            if (extLibFiles.size() > 0) { // exist
                List<File> allLibFiles = new ArrayList<>(Arrays.asList(libFiles));
                allLibFiles.addAll(extLibFiles);
                libFiles = allLibFiles.toArray(new File[allLibFiles.size()]);
            }
        }

        if (libFiles == null || libFiles.length == 0) {
            throw new RuntimeException("Could not find lib directory at " + libDir.getAbsolutePath());
        }
        // native libs
        final String libraryPaths = NativeLibrariesLoader.getLibraryPaths(extLibDir);

        final File[] confFiles = confDir.listFiles();
        if (confFiles == null || confFiles.length == 0) {
            throw new RuntimeException("Could not find conf directory at " + confDir.getAbsolutePath());
        }

        final List<String> cpFiles = new ArrayList<>(confFiles.length + libFiles.length);
        cpFiles.add(confDir.getAbsolutePath());
        for (final File file : libFiles) {
            cpFiles.add(file.getAbsolutePath());
        }

        final StringBuilder classPathBuilder = new StringBuilder();
        for (int i = 0; i < cpFiles.size(); i++) {
            final String filename = cpFiles.get(i);
            classPathBuilder.append(filename);
            if (i < cpFiles.size() - 1) {
                classPathBuilder.append(File.pathSeparatorChar);
            }
        }

        String javaCmd = props.get("java");
        if (javaCmd == null) {
            javaCmd = DEFAULT_JAVA_CMD;
        }
        if (javaCmd.equals(DEFAULT_JAVA_CMD)) {
            String javaHome = System.getenv("JAVA_HOME");
            if (javaHome != null) {
                String fileExtension = isWindows() ? ".exe" : "";
                File javaFile = new File(javaHome + File.separatorChar + "bin" + File.separatorChar + "java" + fileExtension);
                if (javaFile.exists() && javaFile.canExecute()) {
                    javaCmd = javaFile.getAbsolutePath();
                }
            }
        }

        final RuntimeListener listener = new RuntimeListener();
        final int listenPort = listener.start(this);

        final List<String> cmd = new ArrayList<>();

        cmd.add(javaCmd);
        cmd.add("-classpath");
        cmd.add(classPathBuilder.toString());
        cmd.addAll(javaAdditionalArgs);
        addProperty(cmd, OrchsymProperties.PROPERTIES_FILE_PATH, nifiPropsFilename);
        addProperty(cmd, OrchsymProperties.BOOTSTRAP_LISTEN_PORT, listenPort);
        addProperty(cmd, OrchsymProperties.APP, BrandingService.DEFAULT_SHORT_RUNTIME_NAME);
        addProperty(cmd, OrchsymProperties.NIFI_BOOTSTRAP_LOG_DIR, nifiLogDir); // because logback still use it
        addProperty(cmd, OrchsymProperties.BOOTSTRAP_LOG_DIR, nifiLogDir);
        addProperty(cmd, NativeLibrariesLoader.KEY_LIB_PATH, libraryPaths);
        addAddtionsCmds(cmd);
        if (!System.getProperty("java.version").startsWith("1.")) {
            // running on Java 9+, java.xml.bind module must be made available
            cmd.add("--add-modules=java.xml.bind");
        }
        cmd.add(getRuntimeApp());
        if (isSensitiveKeyPresent(props)) {
            Path sensitiveKeyFile = createSensitiveKeyFile(confDir);
            writeSensitiveKeyFile(props, sensitiveKeyFile);
            cmd.add("-K " + sensitiveKeyFile.toFile().getAbsolutePath());
        }

        builder.command(cmd);

        final StringBuilder cmdBuilder = new StringBuilder();
        for (final String s : cmd) {
            cmdBuilder.append(s).append(" ");
        }

        cmdLogger.info("Starting " + RUNTIME_NAME + "...");
        cmdLogger.info("Working Directory: {}", workingDir.getAbsolutePath());
        cmdLogger.info("Command: {}", cmdBuilder.toString());

        String gracefulShutdown = props.get(GRACEFUL_SHUTDOWN_PROP);
        if (gracefulShutdown == null) {
            gracefulShutdown = DEFAULT_GRACEFUL_SHUTDOWN_VALUE;
        }

        final int gracefulShutdownSeconds;
        try {
            gracefulShutdownSeconds = Integer.parseInt(gracefulShutdown);
        } catch (final NumberFormatException nfe) {
            throw new NumberFormatException("The '" + GRACEFUL_SHUTDOWN_PROP + "' property in Bootstrap Config File " + bootstrapConfigAbsoluteFile.getAbsolutePath()
                    + " has an invalid value. Must be a non-negative integer");
        }

        if (gracefulShutdownSeconds < 0) {
            throw new NumberFormatException("The '" + GRACEFUL_SHUTDOWN_PROP + "' property in Bootstrap Config File " + bootstrapConfigAbsoluteFile.getAbsolutePath()
                    + " has an invalid value. Must be a non-negative integer");
        }

        Process process = builder.start();
        handleLogging(process);
        Long pid = OSUtils.getProcessId(process, cmdLogger);
        if (pid == null) {
            cmdLogger.warn("Launched " + RUNTIME_NAME + " but could not determined the Process ID");
        } else {
            nifiPid = pid;
            final Properties pidProperties = new Properties();
            pidProperties.setProperty(PID_KEY, String.valueOf(nifiPid));
            savePidProperties(pidProperties, cmdLogger);
            cmdLogger.info("Launched " + RUNTIME_NAME + " with Process ID " + pid);
        }

        shutdownHook = new OrchsymShutdownHook(process, this, secretKey, gracefulShutdownSeconds, loggingExecutor);
        final Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(shutdownHook);

        final String hostname = getHostname();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        String now = sdf.format(System.currentTimeMillis());
        String user = System.getProperty("user.name");
        if (user == null || user.trim().isEmpty()) {
            user = "Unknown User";
        }
        serviceManager.notify(NotificationType.NIFI_STARTED, RUNTIME_NAME + " Started on Host " + hostname,
                "Hello,\n\n" + RUNTIME_NAME + " has been started on host " + hostname + " at " + now + " by user " + user);

        while (true) {
            final boolean alive = isAlive(process);

            if (alive) {
                try {
                    Thread.sleep(1000L);
                } catch (final InterruptedException ie) {
                }
            } else {
                try {
                    runtime.removeShutdownHook(shutdownHook);
                } catch (final IllegalStateException ise) {
                    // happens when already shutting down
                }

                now = sdf.format(System.currentTimeMillis());
                if (autoRestartNiFi) {
                    final File statusFile = getStatusFile(defaultLogger);
                    if (!statusFile.exists()) {
                        defaultLogger.info("Status File no longer exists. Will not restart " + RUNTIME_NAME);
                        return;
                    }

                    final File lockFile = getLockFile(defaultLogger);
                    if (lockFile.exists()) {
                        defaultLogger.info("A shutdown was initiated. Will not restart " + RUNTIME_NAME);
                        return;
                    }

                    final boolean previouslyStarted = getNifiStarted();
                    if (!previouslyStarted) {
                        defaultLogger.info(RUNTIME_NAME + " never started. Will not restart " + RUNTIME_NAME);
                        return;
                    } else {
                        setNiFiStarted(false);
                    }

                    if (isSensitiveKeyPresent(props)) {
                        Path sensitiveKeyFile = createSensitiveKeyFile(confDir);
                        writeSensitiveKeyFile(props, sensitiveKeyFile);
                    }

                    defaultLogger.warn(RUNTIME_NAME + " appears to have died. Restarting...");
                    process = builder.start();
                    handleLogging(process);

                    pid = OSUtils.getProcessId(process, defaultLogger);
                    if (pid == null) {
                        cmdLogger.warn("Launched " + RUNTIME_NAME + " but could not obtain the Process ID");
                    } else {
                        nifiPid = pid;
                        final Properties pidProperties = new Properties();
                        pidProperties.setProperty(PID_KEY, String.valueOf(nifiPid));
                        savePidProperties(pidProperties, defaultLogger);
                        cmdLogger.info("Launched " + RUNTIME_NAME + " with Process ID " + pid);
                    }

                    shutdownHook = new ShutdownHook(process, this, secretKey, gracefulShutdownSeconds, loggingExecutor);
                    runtime.addShutdownHook(shutdownHook);

                    final boolean started = waitForStart();

                    if (started) {
                        defaultLogger.info("Successfully started " + RUNTIME_NAME + "{}", (pid == null ? "" : " with PID " + pid));
                        // We are expected to restart nifi, so send a notification that it died. If we are not restarting nifi,
                        // then this means that we are intentionally stopping the service.
                        serviceManager.notify(NotificationType.NIFI_DIED, RUNTIME_NAME + " Died on Host " + hostname,
                                "Hello,\n\nIt appears that " + RUNTIME_NAME + " has died on host " + hostname + " at " + now + "; automatically restarting " + RUNTIME_NAME);
                    } else {
                        defaultLogger.error(RUNTIME_NAME + " does not appear to have started");
                        // We are expected to restart nifi, so send a notification that it died. If we are not restarting nifi,
                        // then this means that we are intentionally stopping the service.
                        serviceManager.notify(NotificationType.NIFI_DIED, RUNTIME_NAME + " Died on Host " + hostname, "Hello,\n\nIt appears that " + RUNTIME_NAME + " has died on host " + hostname
                                + " at " + now + ". Attempted to restart " + RUNTIME_NAME + " but the services does not appear to have restarted!");
                    }
                } else {
                    return;
                }
            }
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    private List<File> retrieveExtLibs(File parentFile) {
        List<File> jarFiles = new ArrayList<>();
        final File[] listFiles = parentFile.listFiles(jarFilter);
        if (listFiles != null && listFiles.length > 0) {
            jarFiles.addAll(Arrays.asList(listFiles));
        }
        //
        final File[] subFolders = parentFile.listFiles(new FileFilter() {

            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        });
        if (subFolders != null && subFolders.length > 0) {
            for (File folder : subFolders) {
                jarFiles.addAll(retrieveExtLibs(folder));
            }
        }
        return jarFiles;
    }

    protected static void printErr(String message) {
        System.err.println();
        System.err.println(message);
        System.err.println();
    }

    public void run(String[] args) throws IOException, InterruptedException {
        /*
         * 
         */
        if (args.length < 1 || args.length > 3)

        {
            printUsage();
            return;
        }

        File dumpFile = null;
        boolean verbose = false;
        if (args[0].equals("-verbose")) {
            verbose = true;
            args = shift(args);
        }

        final String cmd = args[0];
        if (cmd.equals("dump")) {
            if (args.length > 1) {
                dumpFile = new File(args[1]);
            } else {
                dumpFile = null;
            }
        }

        switch (cmd.toLowerCase()) {
        case "start":
        case "run":
        case "stop":
        case "status":
        case "dump":
        case "restart":
        case "env":
            break;
        default:
            printUsage();
            return;
        }

        Integer exitStatus = null;
        switch (cmd.toLowerCase()) {
        case "start":
            start();
            break;
        case "run":
            start();
            break;
        case "stop":
            stop();
            break;
        case "status":
            exitStatus = status();
            break;
        case "restart":
            stop();
            start();
            break;
        case "dump":
            dump(dumpFile);
            break;
        case "env":
            env();
            break;
        }
        if (exitStatus != null) {
            System.exit(exitStatus);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final RunOrchsymRuntime runtime = new RunOrchsymRuntime();
        runtime.run(args);

    }
}
