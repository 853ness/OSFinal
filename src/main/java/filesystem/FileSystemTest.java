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
    @Test
    void testDeallocateBlockSuccessfully() {
        // Arrange: Create a FreeBlockList and simulate an allocated block
        FreeBlockList freeBlockList = new FreeBlockList();
        byte[] freeList = new byte[Disk.BYTES_IN_FREE_SPACE_LIST];
        freeBlockList.setFreeBlockList(freeList);

        int blockToAllocate = 5;
        freeList[blockToAllocate / 8] |= (1 << (blockToAllocate % 8)); // Mark block as allocated
        assertTrue((freeList[blockToAllocate / 8] & (1 << (blockToAllocate % 8))) != 0,
                "Block should be allocated before deallocation");

        // Act: Deallocate the block
        freeBlockList.deallocateBlock(blockToAllocate);

        // Assert: Verify the block is deallocated
        assertFalse((freeList[blockToAllocate / 8] & (1 << (blockToAllocate % 8))) != 0,
                "Block should be deallocated");
    }

}


