package filesystem;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {

    @Test
    private int safeCreateFile(FileSystem fs, String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        return fs.create(fileName);

    }
    @Test
    void readSingleBlockFile() throws IOException {
        try {
            // Arrange: Create a FileSystem and set up the test
            FileSystem fs = new FileSystem();
            String fileName = "testFile.txt";
            String fileContent = "Hello, FileSystem! This is a test.";

            // Safely create the file
            int fd = Integer.parseInt(String.valueOf(safeCreateFile(fs, fileName)));

            // Step 2: Write data to the file
            fs.write(fd, fileContent);

            // Act: Read the data back
            String readContent = fs.read(fd);

            // Assert: Verify the content matches what was written
            assertEquals(fileContent, readContent, "File content should match what was written");

        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        }
    }


    @Test
    void readMultiBlockFile() throws IOException {
        FileSystem fs = new FileSystem();
        String fileName = "largeFile.txt";
        StringBuilder fileContent = new StringBuilder();

        // Generate content spanning multiple blocks
        for (int i = 0; i < Disk.BLOCK_SIZE * 3; i++) {
            fileContent.append("A");
        }

        // Create and write to the file
        int fd = fs.create(fileName);
        fs.write(fd, fileContent.toString());

        // Read the file
        String readContent = fs.read(fd);

        // Assert that the read content matches the written content
        assertEquals(fileContent.toString(), readContent, "Content should match for large files");
    }
    @Test
    void readEmptyFile() throws IOException {
        FileSystem fs = new FileSystem();
        String fileName = "emptyFile.txt";

        // Create an empty file
        int fd = fs.create(fileName);

        // Read the file
        String readContent = fs.read(fd);

        // Assert that the content is empty
        assertEquals("", readContent, "Empty file should return an empty string");
    }
    @Test
    void readInvalidFileDescriptor() throws IOException {
        FileSystem fs = new FileSystem();

        // Assert that reading an invalid file descriptor throws an exception
        assertThrows(IOException.class, () -> fs.read(-1), "Invalid file descriptor should throw IOException");
    }

}