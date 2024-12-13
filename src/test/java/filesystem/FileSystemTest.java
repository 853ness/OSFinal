package filesystem;

import org.junit.Test;

public class FileSystemTest {

	@Test
	public void testFileSystem() throws Exception {

	}

	@Test
	public void testCreate() throws Exception {

	}

	@Test
	public void testDelete() throws Exception {

	}

	@Test
	public void testOpen() throws Exception {

	}

	@Test
	public void testClose() throws Exception {

	}

	@Test
	public void testRead() throws Exception {

	}

	@Test
	public void testWrite() throws Exception {
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

	}

}
