package com.guzula.pswitch.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LengthFieldFramerDecoderTest {

  @Test
  void waitsForTheCompletePayload() {
    EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldFramerDecoder());

    assertFalse(channel.writeInbound(Unpooled.wrappedBuffer(new byte[] {0x00, 0x05, 'O', 'L'})));
    assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(new byte[] {'A', '!', '!'})));

    ByteBuf payload = channel.readInbound();
    assertEquals("OLA!!", payload.toString(StandardCharsets.UTF_8));
    payload.release();
    assertNull(channel.readInbound());
    channel.finishAndReleaseAll();
  }

  @Test
  void separatesMultipleFramesReceivedTogether() {
    EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldFramerDecoder());

    assertTrue(
        channel.writeInbound(
            Unpooled.wrappedBuffer(new byte[] {0x00, 0x02, 'O', 'K', 0x00, 0x03, 'P', 'O', 'S'})));

    ByteBuf first = channel.readInbound();
    ByteBuf second = channel.readInbound();
    assertEquals("OK", first.toString(StandardCharsets.UTF_8));
    assertEquals("POS", second.toString(StandardCharsets.UTF_8));
    first.release();
    second.release();
    assertNull(channel.readInbound());
    channel.finishAndReleaseAll();
  }
}
