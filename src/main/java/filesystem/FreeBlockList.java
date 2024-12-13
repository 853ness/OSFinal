package filesystem;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.IOException;

public class FreeBlockList {
    private byte[] freeList;

    public FreeBlockList() {
        freeList = new byte[Disk.BYTES_IN_FREE_SPACE_LIST];
    }

    public byte[] getFreeBlockList() {
        return freeList;
    }

    /**
     * Replaces the current free block list with the block list given by
     * <code>list</code>
     *
     * @param list - New free block list
     * @throws IllegalArgumentException If the length of <code>list</code> is not equal to
     *                                  the free space list length in bytes
     */
    public void setFreeBlockList(byte[] list) throws IllegalArgumentException {
        if (list.length != Disk.BYTES_IN_FREE_SPACE_LIST) {
            throw new IllegalArgumentException("FreeBlockList:: setFreeBlockList: " +
                    "setting free block list of size " + list.length +
                    " it should be of length " + Disk.BYTES_IN_FREE_SPACE_LIST);
        }

        this.freeList = list;
    }

    /**
     * Allocate the block given by <code>whichBlock</code>
     *
     * @param whichBlock - block to allocate
     */

    public void allocateBlock(int whichBlock) {
        int blockNum = (int) (whichBlock / 8);
        int offset = whichBlock % 8;

        /**
         * Extract the correct byte in which the block falls
         *
         * Blocks are arranged from 0..max_block
         *
         * Each block has a bit that tells whether or not the block
         * is free (0) or taken(1).  The list of bits are grouped
         * 8-bits at a time.  So we divide by 8 to compute the
         * byte in which a block number falls.  We do a modulo 8
         * to find the offset within the byte.
         *
         * It is assumed that block numbers begin with 0
         */
        freeList[blockNum] = (byte) (freeList[blockNum] | (1 << offset));
    }

    /**
     * Deallocate the block given by <code>whichBlock</code>
     *
     * @param whichBlock - block to deallocate
     */
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

    
}
