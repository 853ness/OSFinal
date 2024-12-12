package filesystem;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {

    @Test
    void readMultiBlockFile() {
        try {
            // Arrange: Create a FileSystem
            FileSystem fs = new FileSystem();
            String fileName = "largeFile.txt";
            StringBuilder fileContent = new StringBuilder();

            // Generate content that spans 3 blocks
            for (int i = 0; i < Disk.BLOCK_SIZE * 3; i++) {
                fileContent.append("A");
            }

            // Step 1: Create a new file
            int fd = fs.create(fileName);

            // Step 2: Manually simulate writing data into the file's inode and disk blocks
            INode inode = fs.diskDevice.readInode(fd); // Get inode
            inode.setSize(fileContent.length()); // Set the file size
            inode.setBlockPointer(0, 0); // Block 0
            inode.setBlockPointer(1, 1); // Block 1
            inode.setBlockPointer(2, 2); // Block 2
            fs.diskDevice.writeInode(inode, fd); // Write inode back to disk

            // Write data directly to the disk blocks
            byte[] block0 = fileContent.substring(0, Disk.BLOCK_SIZE).getBytes();
            byte[] block1 = fileContent.substring(Disk.BLOCK_SIZE, Disk.BLOCK_SIZE * 2).getBytes();
            byte[] block2 = fileContent.substring(Disk.BLOCK_SIZE * 2).getBytes();
            fs.diskDevice.writeDataBlock(block0, 0); // Write to block 0
            fs.diskDevice.writeDataBlock(block1, 1); // Write to block 1
            fs.diskDevice.writeDataBlock(block2, 2); // Write to block 2

            // Step 3: Read the data back
            String readContent = fs.read(fd);

            // Assert: Verify the content matches what was written
            assertEquals(fileContent.toString(), readContent, "File content should match for large files");

        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        }
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

