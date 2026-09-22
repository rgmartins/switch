package com.guzula.pswitch.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LengthFieldFramerEncoderTest {

  @Test
  void addsTheBinaryPayloadLength() {
    EmbeddedChannel channel = new EmbeddedChannel(new LengthFieldFramerEncoder());

    assertTrue(channel.writeOutbound(Unpooled.copiedBuffer("OLA SWITCH", StandardCharsets.UTF_8)));

    ByteBuf frame = channel.readOutbound();
    assertEquals(10, frame.readUnsignedShort());
    assertEquals("OLA SWITCH", frame.toString(StandardCharsets.UTF_8));
    frame.release();
    channel.finishAndReleaseAll();
  }
}
