/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orchsym.boot;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.nifi.bootstrap.BootstrapCodec;
import org.apache.nifi.bootstrap.RunNiFi;
import org.apache.nifi.bootstrap.util.LimitingInputStream;

/**
 * 
 * @author GU Guoqiang
 *
 *         Same as @see NiFiListener, only replace the "NiFi" to RUNTIME_NAME
 */
public class RuntimeListener {

    private ServerSocket serverSocket;
    private volatile Listener listener;

    int start(final RunOrchsymRuntime runner) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("localhost", 0));

        final int localPort = serverSocket.getLocalPort();
        listener = new Listener(serverSocket, runner);
        final Thread listenThread = new Thread(listener);
        listenThread.setName("Listen to " + RunOrchsymRuntime.RUNTIME_NAME);
        listenThread.setDaemon(true);
        listenThread.start();
        return localPort;
    }

    public void stop() throws IOException {
        final Listener listener = this.listener;
        if (listener == null) {
            return;
        }

        listener.stop();
    }

    private class Listener implements Runnable {

        private final ServerSocket serverSocket;
        private final ExecutorService executor;
        private final RunNiFi runner;
        private volatile boolean stopped = false;

        public Listener(final ServerSocket serverSocket, final RunNiFi runner) {
            this.serverSocket = serverSocket;
            this.executor = Executors.newFixedThreadPool(2, new ThreadFactory() {
                @Override
                public Thread newThread(final Runnable runnable) {
                    final Thread t = Executors.defaultThreadFactory().newThread(runnable);
                    t.setDaemon(true);
                    t.setName(RunOrchsymRuntime.RUNTIME_NAME + " Bootstrap Command Listener");
                    return t;
                }
            });

            this.runner = runner;
        }

        public void stop() throws IOException {
            stopped = true;

            executor.shutdown();
            try {
                executor.awaitTermination(3, TimeUnit.SECONDS);
            } catch (final InterruptedException ie) {
            }

            serverSocket.close();
        }

        @Override
        public void run() {
            while (!serverSocket.isClosed()) {
                try {
                    if (stopped) {
                        return;
                    }

                    final Socket socket;
                    try {
                        socket = serverSocket.accept();
                    } catch (final IOException ioe) {
                        if (stopped) {
                            return;
                        }

                        throw ioe;
                    }

                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // we want to ensure that we don't try to read data from an InputStream directly
                                // by a BufferedReader because any user on the system could open a socket and send
                                // a multi-gigabyte file without any new lines in order to crash the Bootstrap,
                                // which in turn may cause the Shutdown Hook to shutdown NiFi.
                                // So we will limit the amount of data to read to 4 KB
                                final InputStream limitingIn = new LimitingInputStream(socket.getInputStream(), 4096);
                                final BootstrapCodec codec = new BootstrapCodec(runner, limitingIn, socket.getOutputStream());
                                codec.communicate();
                            } catch (final Throwable t) {
                                System.out.println("Failed to communicate with " + RunOrchsymRuntime.RUNTIME_NAME + " due to " + t);
                                t.printStackTrace();
                            } finally {
                                try {
                                    socket.close();
                                } catch (final IOException ioe) {
                                }
                            }
                        }
                    });
                } catch (final Throwable t) {
                    System.err.println("Failed to receive information from " + RunOrchsymRuntime.RUNTIME_NAME + " due to " + t);
                    t.printStackTrace();
                }
            }
        }
    }
}
