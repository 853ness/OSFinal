package filesystem;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {
    @Test
    void fileSystemOperations() {
        try {
            // Arrange: Create a FileSystem and set up constants
            FileSystem fs = new FileSystem();
            String fileNameBase = "file";
            String testData = "This is some text ";
            int numLines = 10; // Reduced for testing purposes

            // Create multiple files with content of increasing length
            for (int i = 0; i < numLines; i++) {
                String fileName = fileNameBase + i + ".txt";
                int fd = fs.create(fileName);

                // Generate the message content
                StringBuilder theMessage = new StringBuilder();
                for (int j = 0; j < i + 1; j++) {
                    theMessage.append(testData).append(j).append(".  ");
                }

                // Write the content to the file
                fs.write(fd, theMessage.toString());
                fs.close(fd);
            }

            // Delete every second file
            for (int i = 0; i < numLines; i += 2) {
                String fileName = fileNameBase + i + ".txt";
                fs.delete(fileName);
            }

            // Read remaining files and verify content
            for (int i = 1; i < numLines; i += 2) {
                String fileName = fileNameBase + i + ".txt";
                int fd = fs.open(fileName);

                // Generate the expected content
                StringBuilder expectedMessage = new StringBuilder();
                for (int j = 0; j < i + 1; j++) {
                    expectedMessage.append(testData).append(j).append(".  ");
                }

                // Read the file and verify content
                String readContent = fs.read(fd);
                assertEquals(expectedMessage.toString(), readContent, "Content of file " + fileName + " should match");
                fs.close(fd);
            }

            // Verify deleted files cannot be opened
            for (int i = 0; i < numLines; i += 2) {
                String fileName = fileNameBase + i + ".txt";
                assertThrows(IOException.class, () -> fs.open(fileName), "Deleted file " + fileName + " should not exist");
            }

        } catch (IOException e) {
            fail("Test failed due to unexpected exception: " + e.getMessage());
        }
    }
}