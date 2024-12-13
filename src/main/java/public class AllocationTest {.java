
public class AllocationTest {

    public static void main(String[] args) {
        // Test Case 1: Sufficient space available
        testAllocation(10, 5); // Expected: No message about insufficient space

        // Test Case 2: Insufficient space available
        testAllocation(3, 5); // Expected: Prints the "Insufficient space available" message

        // Test Case 3: Edge case where allocated equals needed
        testAllocation(5, 5); // Expected: No message about insufficient space
    }

    private static void testAllocation(int blocksAllocated, int blocksNeeded) {
        System.out.println("\nTesting with blocksAllocated = " + blocksAllocated + " and blocksNeeded = " + blocksNeeded);

        if (blocksAllocated < blocksNeeded) {
            System.out.println("Space Available: " + blocksAllocated);
            System.out.println("Space Needed: " + blocksNeeded);
            System.out.println("Insufficient space available.");
        } else {
            System.out.println("Sufficient space available.");
        }
    }
}
