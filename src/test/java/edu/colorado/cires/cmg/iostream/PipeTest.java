package edu.colorado.cires.cmg.iostream;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class PipeTest {

  @Test
  public void testPipe() throws Exception {

    Consumer<OutputStream> outConsumer = out -> {
      try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/qcFiles/badExample.xyz"))) {
        IOUtils.copy(in, out);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Pipe.pipe(outConsumer, in -> {
      try {
        IOUtils.copy(in, outputStream);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

    assertEquals(FileUtils.readFileToString(Paths.get("src/test/resources/qcFiles/badExample.xyz").toFile(), StandardCharsets.UTF_8),
        new String(outputStream.toByteArray(), StandardCharsets.UTF_8));

  }

  @Test
  public void testPipeError() throws Exception {

    Exception ex = assertThrows(Exception.class, () -> {
      Consumer<OutputStream> outConsumer = out -> {
        try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/qcFiles/nothere.xyz"))) {
          IOUtils.copy(in, out);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      };

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Pipe.pipe(outConsumer, in -> {
        try {
          IOUtils.copy(in, outputStream);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    });
  }

  @Test
  public void testNestedPipeError() throws Exception {

    Exception ex = assertThrows(RuntimeException.class, () -> {
      Consumer<OutputStream> outConsumer = out -> {
        try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/qcFiles/nothere.xyz"))) {
          IOUtils.copy(in, out);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      };

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Pipe.pipe(outConsumer, in -> {
        AtomicLong count = new AtomicLong();
        Consumer<OutputStream> countConsumer = out -> {
          try {
            count.set(IOUtils.copyLarge(in, out));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };

        Pipe.pipe(countConsumer, countedIn -> {
          try {
            IOUtils.copy(countedIn, outputStream);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      });

    });

  }

  @Test
  public void testNestedPipeError2() throws Exception {

    Exception ex = assertThrows(RuntimeException.class, () -> {
      Consumer<OutputStream> outConsumer = out -> {
        try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/qcFiles/badExample.xyz"))) {
          IOUtils.copy(in, out);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      };

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Pipe.pipe(outConsumer, in -> {
        Consumer<OutputStream> countConsumer = out -> {
          try {
            byte[] bytes = new byte[10];
            IOUtils.read(in, bytes);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          throw new RuntimeException("test error");
        };
        Pipe.pipe(countConsumer, countedIn -> {
          try {
            IOUtils.copy(countedIn, outputStream);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

      });

    });

  }

}