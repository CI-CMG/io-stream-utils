# io-stream-utils

The io-stream-utils project provides various utilities for working with InputStreams and OutputStreams

Additional project information, javadocs, and test coverage is located at https://ci-cmg.github.io/project-documentation/io-stream-utils/

## Adding To Your Project

Add the following dependency to your Maven pom.xml

```xml
    <dependency>
      <groupId>io.github.ci-cmg</groupId>
      <artifactId>io-stream-utils</artifactId>
      <version>1.0.0</version>
    </dependency>
```

## Usage

### Pipe

The Pipe allows an OutputStream to be converted to an InputStream.  The pipe() function takes two arguments: a Consumer<OutputStream> (pipe supplier) and a Consumer<OutputStream> (pipe consumer).

The pipe supplier is responsible for writing to the supplied OutputStream.  The pipe consumer does whatever it needs to do with the supplied InputStream. Neither the
pipe supplier nor the pipe consumer need to close the provided stream.  The Pipe will handle that.


```java
Pipe.pipe(
    (outputStream) -> someMethodThatWritesToOutput(outputStream),
    (inputStream) -> someMethodThatReadsInput(inputStream)
);
```
