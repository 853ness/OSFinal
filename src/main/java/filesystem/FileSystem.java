package filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;


public class FileSystem {
    private Disk diskDevice;

    private int iNodeNumber;
    private int fileDescriptor;
    private INode iNodeForFile;

    public FileSystem() throws IOException {
        diskDevice = new Disk();
        diskDevice.format();
    }

    /***
     * Create a file with the name <code>fileName</code>
     *
     * @param fileName - name of the file to create
     * @throws IOException
     */
    public int create(String fileName) throws IOException {
        INode tmpINode = null;

        boolean isCreated = false;

        for (int i = 0; i < Disk.NUM_INODES && !isCreated; i++) {
            tmpINode = diskDevice.readInode(i);
            String name = tmpINode.getFileName();
            if (name.trim().equals(fileName)){
                throw new IOException("FileSystem::create: "+fileName+
                        " already exists");
            } else if (tmpINode.getFileName() == null) {
                this.iNodeForFile = new INode();
                this.iNodeForFile.setFileName(fileName);
                this.iNodeNumber = i;
                this.fileDescriptor = i;
                isCreated = true;
            }
        }
        if (!isCreated) {
            throw new IOException("FileSystem::create: Unable to create file");
        }

        return fileDescriptor;
    }

    /**
     * Removes the file
     *
     * @param fileName
     * @throws IOException
     */
    public void delete(String fileName) throws IOException {
        INode tmpINode = null;
        boolean isFound = false;
        int inodeNumForDeletion = -1;

        /**
         * Find the non-null named inode that matches,
         * If you find it, set its file name to null
         * to indicate it is unused
         */
        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);

            String fName = tmpINode.getFileName();

            if (fName != null && fName.trim().compareTo(fileName.trim()) == 0) {
                isFound = true;
                inodeNumForDeletion = i;
                break;
            }
        }

        /***
         * If file found, go ahead and deallocate its
         * blocks and null out the filename.
         */
        if (isFound) {
            deallocateBlocksForFile(inodeNumForDeletion);
            tmpINode.setFileName(null);
            diskDevice.writeInode(tmpINode, inodeNumForDeletion);
            this.iNodeForFile = null;
            this.fileDescriptor = -1;
            this.iNodeNumber = -1;
        }
    }


    /***
     * Makes the file available for reading/writing
     *
     * @return
     * @throws IOException
     */
    public int open(String fileName) throws IOException {
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
        INode tmpINode = null;
        boolean isFound = false;
        int iNodeContainingName = -1;

        for (int i = 0; i < Disk.NUM_INODES && !isFound; i++) {
            tmpINode = diskDevice.readInode(i);
            String fName = tmpINode.getFileName();
            if (fName != null) {
                if (fName.trim().compareTo(fileName.trim()) == 0) {
                    isFound = true;
                    iNodeContainingName = i;
                    this.iNodeForFile = tmpINode;
                }
            }
        }

        if (isFound) {
            this.fileDescriptor = iNodeContainingName;
            this.iNodeNumber = fileDescriptor;
        }

        return this.fileDescriptor;
    }


    /***
     * Closes the file
     *
     * @throws IOException If disk is not accessible for writing
     */
    public void close(int fileDescriptor) throws IOException {
        if (fileDescriptor != this.iNodeNumber){
            throw new IOException("FileSystem::close: file descriptor, "+
                    fileDescriptor + " does not match file descriptor " +
                    "of open file");
        }
        diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);
        this.iNodeForFile = null;
        this.fileDescriptor = -1;
        this.iNodeNumber = -1;
    }


    /**
     * Add your Javadoc documentation for this method
     */
    public String read(int fileDescriptor) throws IOException {
        // TODO: Replace this line with your code
        return null;
    }


    /**
     * Add your Javadoc documentation for this method
     */
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
public void write(int fileDescriptor, String data) throws IOException {
        if (fileDescriptor != this.iNodeNumber || this.iNodeForFile == null) {
            throw new IOException("FileSystem::write: Invalid file descriptor or inode is null.");
        }

        byte[] dataBytes = data.getBytes();
        int dataSize = dataBytes.length;
        int requiredBlocks = (int) Math.ceil((double) dataSize / Disk.BLOCK_SIZE);

        // Deallocate existing blocks
        deallocateBlocksForFile(this.iNodeNumber);

        // Allocate new blocks
        byte[] freeBlockList = diskDevice.readFreeBlockList();
        int[] allocatedBlocks = new int[requiredBlocks];
        int count = 0;

        for (int i = 0; i < Disk.NUM_BLOCKS && count < requiredBlocks; i++) {
            if ((freeBlockList[i / 8] & (1 << (i % 8))) == 0) { // Block is free
                allocatedBlocks[count++] = i;
                freeBlockList[i / 8] |= (1 << (i % 8)); // Mark block as used
            }
        }

        if (count < requiredBlocks) {
            throw new IOException("FileSystem::allocateBlocksForFile: Number of blocks is unavailable!");
        }

        // Write data to allocated blocks
        for (int i = 0; i < allocatedBlocks.length; i++) {
            byte[] blockData = new byte[Disk.BLOCK_SIZE];
            int start = i * Disk.BLOCK_SIZE;
            int length = Math.min(dataSize - start, Disk.BLOCK_SIZE);
            System.arraycopy(dataBytes, start, blockData, 0, length);
            diskDevice.writeDataBlock(blockData, allocatedBlocks[i]);
        }

        // Update inode
        for (int i = 0; i < allocatedBlocks.length; i++) {
            this.iNodeForFile.setBlockPointer(i, allocatedBlocks[i]);
        }
        this.iNodeForFile.setSize(dataSize);

        // Write updates to disk
        diskDevice.writeFreeBlockList(freeBlockList);
        diskDevice.writeInode(this.iNodeForFile, this.iNodeNumber);
    }


    /**
     * Add your Javadoc documentation for this method
     */
    private int[] allocateBlocksForFile(int iNodeNumber, int numBytes)
            throws IOException {

        // TODO: replace this line with your code

        return null;
    }

    /**
     * Add your Javadoc documentation for this method
     */
    private void deallocateBlocksForFile(int iNodeNumber) {
        // TODO: replace this line with your code
    }

    // You may add any private method after this comment

}
