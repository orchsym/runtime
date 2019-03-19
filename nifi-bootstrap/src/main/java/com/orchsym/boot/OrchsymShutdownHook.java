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
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.bootstrap.RunNiFi;
import org.apache.nifi.bootstrap.ShutdownHook;

public class OrchsymShutdownHook extends ShutdownHook {
    private RunOrchsymRuntime runtime;

    public OrchsymShutdownHook(Process nifiProcess, RunOrchsymRuntime runner, String secretKey, int gracefulShutdownSeconds, ExecutorService executor) {
        super(nifiProcess, runner, secretKey, gracefulShutdownSeconds, executor);
        this.runtime = runner;
    }

    @Override
    public void run() {
        executor.shutdown();
        runtime.setAutoRestartNiFi(false);
        final int ccPort = runtime.getNiFiCommandControlPort();
        if (ccPort > 0) {
            System.out.println("Initiating Shutdown of " + RunOrchsymRuntime.RUNTIME_NAME + "...");

            try {
                final Socket socket = new Socket("localhost", ccPort);
                final OutputStream out = socket.getOutputStream();
                out.write(("SHUTDOWN " + secretKey + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();

                socket.close();
            } catch (final IOException ioe) {
                System.out.println("Failed to Shutdown " + RunOrchsymRuntime.RUNTIME_NAME + " due to " + ioe);
            }
        }

        runtime.notifyStop();
        System.out.println("Waiting for " + RunOrchsymRuntime.RUNTIME_NAME + " to finish shutting down...");
        final long startWait = System.nanoTime();
        while (RunNiFi.isAlive(nifiProcess)) {
            final long waitNanos = System.nanoTime() - startWait;
            final long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(waitNanos);
            if (waitSeconds >= gracefulShutdownSeconds && gracefulShutdownSeconds > 0) {
                if (RunNiFi.isAlive(nifiProcess)) {
                    System.out.println(RunOrchsymRuntime.RUNTIME_NAME + " has not finished shutting down after " + gracefulShutdownSeconds + " seconds. Killing process.");
                    nifiProcess.destroy();
                }
                break;
            } else {
                try {
                    Thread.sleep(1000L);
                } catch (final InterruptedException ie) {
                }
            }
        }

        try {
            final File statusFile = runtime.getStatusFile();
            if (!statusFile.delete()) {
                System.err.println("Failed to delete status file " + statusFile.getAbsolutePath() + "; this file should be cleaned up manually");
            }
        } catch (IOException ex) {
            System.err.println("Failed to retrieve status file " + ex);
        }
    }
}
