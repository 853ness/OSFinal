package filesystem;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;

class FileSystemTest {

    private FileSystem fileSystem;

    @Test
    void testUnsuccessfulBlockAllocation() throws IOException {
        // Arrange: Create a FileSystem and simulate a full disk
        FileSystem fs = new FileSystem();

        // Fill up the disk to simulate no free blocks
        byte[] freeBlockList = new byte[Disk.BYTES_IN_FREE_SPACE_LIST];
        for (int i = 0; i < freeBlockList.length; i++) {
            freeBlockList[i] = (byte) 0xFF; // Set all bits to 1 (all blocks used)
        }
        fs.diskDevice.writeFreeBlockList(freeBlockList); // Mark all blocks as allocated

        String fileName = "testFile.txt";
        int fileSize = Disk.BLOCK_SIZE; // File size that requires 1 block

        // Act & Assert
        IOException exception = assertThrows(IOException.class, () -> {
            int fd = fs.create(fileName); // Create a file, which should attempt to allocate space
            fs.write(fd, "A".repeat(fileSize)); // Attempt to write to the file
        });

        // Verify the exception message
        assertEquals("FileSystem::allocateBlocksForFile: Number of blocks is unavailable!", exception.getMessage());
    }
