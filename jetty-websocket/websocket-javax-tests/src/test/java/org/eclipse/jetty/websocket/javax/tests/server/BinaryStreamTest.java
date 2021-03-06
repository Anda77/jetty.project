//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.websocket.javax.tests.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.core.CloseStatus;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.OpCode;
import org.eclipse.jetty.websocket.javax.tests.DataUtils;
import org.eclipse.jetty.websocket.javax.tests.Fuzzer;
import org.eclipse.jetty.websocket.javax.tests.LocalServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BinaryStreamTest
{
    private static final String PATH = "/echo";

    private static LocalServer server;

    @BeforeAll
    public static void startServer() throws Exception
    {
        server = new LocalServer();
        server.start();
        ServerEndpointConfig config = ServerEndpointConfig.Builder.create(ServerBinaryStreamer.class, PATH).build();
        server.getServerContainer().addEndpoint(config);
    }

    @AfterAll
    public static void stopServer() throws Exception
    {
        server.stop();
    }

    @Test
    public void testEchoWithMediumMessage() throws Exception
    {
        testEcho(1024);
    }

    @Test
    public void testLargestMessage() throws Exception
    {
        testEcho(server.getServerContainer().getDefaultMaxBinaryMessageBufferSize());
    }

    private byte[] newData(int size)
    {
        byte[] pattern = "01234567890abcdefghijlklmopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++)
        {
            data[i] = pattern[i % pattern.length];
        }
        return data;
    }

    private void testEcho(int size) throws Exception
    {
        byte[] data = newData(size);

        List<Frame> send = new ArrayList<>();
        send.add(new Frame(OpCode.BINARY).setPayload(data));
        send.add(CloseStatus.toFrame(CloseStatus.NORMAL));

        ByteBuffer expectedMessage = DataUtils.copyOf(data);

        try (Fuzzer session = server.newNetworkFuzzer("/echo"))
        {
            session.sendBulk(send);
            BlockingQueue<Frame> receivedFrames = session.getOutputFrames();
            session.expectMessage(receivedFrames, OpCode.BINARY, expectedMessage);
        }
    }

    @Test
    public void testMoreThanLargestMessageOneByteAtATime() throws Exception
    {
        int size = server.getServerContainer().getDefaultMaxBinaryMessageBufferSize() + 16;
        byte[] data = newData(size);

        List<Frame> send = new ArrayList<>();
        send.add(new Frame(OpCode.BINARY).setPayload(data));
        send.add(CloseStatus.toFrame(CloseStatus.NORMAL));

        ByteBuffer expectedMessage = DataUtils.copyOf(data);

        try (Fuzzer session = server.newNetworkFuzzer("/echo"))
        {
            session.sendSegmented(send, 1);
            BlockingQueue<Frame> receivedFrames = session.getOutputFrames();
            session.expectMessage(receivedFrames, OpCode.BINARY, expectedMessage);
        }
    }

    @ServerEndpoint(PATH)
    public static class ServerBinaryStreamer
    {
        private static final Logger LOG = Log.getLogger(ServerBinaryStreamer.class);

        @OnMessage
        public void echo(Session session, InputStream input) throws IOException
        {
            byte[] buffer = new byte[128];
            try (OutputStream output = session.getBasicRemote().getSendStream())
            {
                int readCount = 0;
                int read;
                while ((read = input.read(buffer)) >= 0)
                {
                    output.write(buffer, 0, read);
                    readCount += read;
                }
                LOG.debug("Read {} bytes", readCount);
            }
        }
    }
}
