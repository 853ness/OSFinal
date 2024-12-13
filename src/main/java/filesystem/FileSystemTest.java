package filesystem;

import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {

@Test
    public void testWrite() throws IOException {
        // Step 1: Initialize the FileSystem
        FileSystem fileSystem = new FileSystem();

        // Step 2: Create a file
        String fileName = "testFile";
        int fileDescriptor = fileSystem.create(fileName);

        // Step 3: Write data to the file
        String data = "Hello, this is test data!";
        fileSystem.write(fileDescriptor, data);

        // Step 4: Read back the data and verify (optional if read not implemented)
        INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
        int fileSize = inode.getSize();

        assertEquals(data.length(), fileSize, "File size should match the data length written");

        // Verify data stored in blocks
        byte[] readData = new byte[data.length()];
        int blockSize = Disk.BLOCK_SIZE;
        int requiredBlocks = (int) Math.ceil((double) data.length() / blockSize);

        for (int i = 0; i < requiredBlocks; i++) {
            int blockPointer = inode.getBlockPointer(i);
            byte[] blockData = fileSystem.diskDevice.readDataBlock(blockPointer);

            int blockStart = i * blockSize;
            int blockEnd = Math.min(data.length(), blockStart + blockSize);

            System.arraycopy(blockData, 0, readData, blockStart, blockEnd - blockStart);
        }

        assertEquals(data, new String(readData), "Data written and data read should match");
      
    }
  void testWriteSmallFile() throws IOException {
        // Step 1: Initialize the FileSystem
        FileSystem fileSystem = new FileSystem();

        // Step 2: Create a file
        String fileName = "testFile.txt";
        int fileDescriptor = fileSystem.create(fileName);

        // Step 3: Write data to the file
        String data = "Hello, FileSystem!";
        fileSystem.write(fileDescriptor, data);

        // Step 4: Verify that the file size matches the data length
        INode inode = fileSystem.diskDevice.readInode(fileDescriptor);
        assertEquals(data.length(), inode.getSize(), "File size should match the length of written data.");
    }

  
}
