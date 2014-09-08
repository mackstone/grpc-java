package com.google.net.stubby;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * Tests for {@link Metadata}
 */
@RunWith(JUnit4.class)
public class MetadataTest {

  private static final Metadata.Marshaller<Fish> FISH_MARSHALLER =
      new Metadata.Marshaller<Fish>() {
    @Override
    public byte[] toBytes(Fish fish) {
      return fish.name.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toAscii(Fish value) {
      return value.name;
    }

        @Override
    public Fish parseBytes(byte[] serialized) {
      return new Fish(new String(serialized, StandardCharsets.UTF_8));
    }

    @Override
    public Fish parseAscii(String ascii) {
      return new Fish(ascii);
    }
  };

  private static final String LANCE = "lance";
  private static final byte[] LANCE_BYTES = LANCE.getBytes(StandardCharsets.US_ASCII);
  private static final Metadata.Key<Fish> KEY = new Metadata.Key<Fish>("test", FISH_MARSHALLER);

  @Test
  public void testWriteParsed() {
    Fish lance = new Fish(LANCE);
    Metadata.Headers metadata = new Metadata.Headers();
    metadata.put(KEY, lance);
    // Should be able to read same instance out
    assertSame(lance, metadata.get(KEY));
    Iterator<Fish> fishes = metadata.<Fish>getAll(KEY).iterator();
    assertTrue(fishes.hasNext());
    assertSame(fishes.next(), lance);
    assertFalse(fishes.hasNext());
    byte[][] serialized = metadata.serialize();
    assertEquals(2, serialized.length);
    assertEquals(new String(serialized[0], StandardCharsets.US_ASCII), "test");
    assertArrayEquals(LANCE_BYTES, serialized[1]);
    assertSame(lance, metadata.get(KEY));
    // Serialized instance should be cached too
    assertSame(serialized[0], metadata.serialize()[0]);
    assertSame(serialized[1], metadata.serialize()[1]);
  }

  @Test
  public void testWriteRaw() {
    Metadata.Headers raw = new Metadata.Headers(
        KEY.asciiName(), LANCE_BYTES);
    Fish lance = raw.get(KEY);
    assertEquals(lance, new Fish(LANCE));
    // Reading again should return the same parsed instance
    assertSame(lance, raw.get(KEY));
  }

  @Test
  public void testFailSerializeRaw() {
    Metadata.Headers raw = new Metadata.Headers(
        KEY.asciiName(), LANCE_BYTES);

    try {
      raw.serialize();
      fail("Can't serialize raw metadata");
    } catch (IllegalStateException ise) {
      // Success
    }
  }

  @Test
  public void testFailMergeRawIntoSerializable() {
    Metadata.Headers raw = new Metadata.Headers(
        KEY.asciiName(), LANCE_BYTES);
    Metadata.Headers serializable = new Metadata.Headers();
    try {
      serializable.merge(raw);
      fail("Can't serialize raw metadata");
    } catch (IllegalArgumentException iae) {
      // Success
    }
  }

  private static class Fish {
    private String name;

    private Fish(String name) {
      this.name = name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Fish fish = (Fish) o;
      if (name != null ? !name.equals(fish.name) : fish.name != null) return false;
      return true;
    }
  }
}