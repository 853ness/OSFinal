package filesystem;

import java.io.IOException;
import java.util.Arrays;

public class FileSystem {

    static Disk diskDevice;

    private int iNodeNumber;
    private int fileDescriptor;
    private INode iNodeForFile;
    private byte[] freeList;

    public FileSystem() throws IOException {
        diskDevice = new Disk();
        diskDevice.format();
    }

    /**
     * *
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

            // The Fix: Add a null check before calling trim()
            if (name != null && name.trim().equals(fileName)) {
                throw new IOException("FileSystem::create: " + fileName + " already exists");
            } else if (tmpINode.getFileName() == null) { // No need for trim() here since we already checked for null

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
         * Find the non-null named inode that matches, If you find it, set its
         * file name to null to indicate it is unused
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

        /**
         * *
         * If file found, go ahead and deallocate its blocks and null out the
         * filename.
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

    /**
     * *
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

    /**
     * *
     * Closes the file
     *
     * @throws IOException If disk is not accessible for writing
     */
    public void close(int fileDescriptor) throws IOException {
        if (fileDescriptor != this.iNodeNumber) {

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
        //validating the file descriptor
        if (fileDescriptor != iNodeNumber || this.iNodeForFile == null)
            throw new IOException("FileSystem:read: Invalid or inode is null");

        INode inode = diskDevice.readInode(fileDescriptor);

        // Retrieve file size to determine the total number of bytes to read
        int fileSize = inode.getSize();
        if (fileSize <= 0) {
            return ""; // File is empty
        }

        // Calculate the number of blocks needed to read the file
        int numBlocks = (int) Math.ceil((double) fileSize / Disk.BLOCK_SIZE);

        // Read the data blocks associated with the inode
        StringBuilder fileContent = new StringBuilder();
        int remainingBytes = fileSize;
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            // Get the block pointer for the current block
            int blockPointer = inode.getBlockPointer(i);
            if (blockPointer == -1) break; // No more valid block pointers

            // Read the data block from the disk
            byte[] blockData = diskDevice.readDataBlock(blockPointer);

            // Determine how many bytes to read from this block
            int bytesToRead = Math.min(remainingBytes, Disk.BLOCK_SIZE);

            // Append the block data to the file content
            fileContent.append(new String(blockData, 0, bytesToRead));

            // Reduce the remaining bytes to read
            remainingBytes -= bytesToRead;
        }

        // Return the combined file content as a string
        return fileContent.toString();
    }

    /**
     * Add your Javadoc documentation for this method
     */
    public void write(int fileDescriptor, String data) throws IOException {

    }

    /**
     * Add your Javadoc documentation for this method
     */

    private int[] allocateBlocksForFile(int iNodeNumber, int numBytes)

            throws IOException {
        int blocksNeeded = (int) Math.ceil((double) numBytes / Disk.BLOCK_SIZE);
        int[] allocatedBlocks = new int[blocksNeeded];
        int blocksAllocated = 0;

        // Read from the freeblocklist to view vacancies
        byte[] freeBlockList = diskDevice.readFreeBlockList();
        FreeBlockList freeListManager = new FreeBlockList();
        freeListManager.setFreeBlockList(freeBlockList);

        //add counter of free blocks and unavailable blocks???
        // Allocate blocks
        for (int i = 0; i < Disk.NUM_BLOCKS && blocksAllocated < blocksNeeded; i++) {
            // Check if the block is free
            int blockNum = i / 8;
            int offset = i % 8;
            if ((freeBlockList[blockNum] & (1 << offset)) == 0) { // Block is free
                freeListManager.allocateBlock(i);
                allocatedBlocks[blocksAllocated++] = i;
            }
        }

        // checking the correct number of blocks has been allocated, printing a statement if blocks unavailable
        if (blocksAllocated < blocksNeeded) {
            System.out.println("Space Needed: " + blocksNeeded);
            System.out.println("Space Available: " + blocksAllocated);
            throw new IOException("FileSystem::allocateBlocksForFile: Number of blocks is unavailable!");

            //System.out.println("Insufficient space available.");
        }

        // Update the free block list on disk
        diskDevice.writeFreeBlockList(freeListManager.getFreeBlockList());

        // Update the block pointers in the INode with new allocated blocks
        INode fileINode = diskDevice.readInode(iNodeNumber);
        for (int j = 0; j < blocksAllocated; j++) {
            fileINode.setBlockPointer(j, allocatedBlocks[j]);
        }

        // writing to disk the updated filesize for inode
        fileINode.setSize(numBytes);
        diskDevice.writeInode(fileINode, iNodeNumber);

        return allocatedBlocks;

    }


    /**
     * Add your Javadoc documentation for this method
     */
  private void deallocateBlocksForFile(int iNodeNumber) throws IOException {
        // Validate the inode number
        if (iNodeNumber < 0 || iNodeNumber >= Disk.NUM_INODES) {
            throw new IOException("FileSystem::deallocateBlocksForFile: Invalid inode number: " + iNodeNumber);
        }

        // Read the inode from the disk
        INode inode = diskDevice.readInode(iNodeNumber);
        if (inode == null) {
            throw new IOException("FileSystem::deallocateBlocksForFile: Inode is null for inode number: " + iNodeNumber);
        }

        // Read the free block list from the disk
        byte[] freeBlockList = diskDevice.readFreeBlockList();

        // Iterate over the inode's block pointers
        for (int i = 0; i < INode.NUM_BLOCK_POINTERS; i++) {
            int blockNumber = inode.getBlockPointer(i);
            if (blockNumber == -1) {
                // No more blocks allocated to this file, exit loop
                break;
            }

            // Calculate the byte index and bit index for the block
            int byteIndex = blockNumber / 8;
            int bitIndex = blockNumber % 8;

            // Check if the block is already free
            if ((freeBlockList[byteIndex] & (1 << bitIndex)) == 0) {
                // Block is already free; log the event and continue
                System.out.println("Block " + blockNumber + " is already free.");
                continue;
            }

            // Deallocate the block by clearing its bit
            freeBlockList[byteIndex] &= ~(1 << bitIndex);

            // Clear the block pointer in the inode
            inode.setBlockPointer(i, -1);

            // Log the deallocation
            System.out.println("Block " + blockNumber + " deallocated successfully.");
        }

        // Write the updated free block list and inode back to the disk
        diskDevice.writeFreeBlockList(freeBlockList);
        diskDevice.writeInode(inode, iNodeNumber);
    }
    // You may add any private method after this comment

}
