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


    
}
