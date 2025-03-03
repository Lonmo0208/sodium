package me.jellysquid.mods.sodium.client.render.chunk.data;

import org.lwjgl.system.MemoryUtil;

// This code is a terrible hack to get around the fact that we are so incredibly memory bound, and that we
// have no control over memory layout. The chunk rendering code spends an astronomical amount of time chasing
// object pointers that are scattered across the heap. Worse yet, because render state is initialized over a long
// period of time as the level loads, those objects are never even remotely close to one another in heap, so
// you also have to pay the penalty of a DTLB miss on every other access.
//
// Unfortunately, Hotspot *still* produces abysmal machine code for the chunk rendering code paths, since any usage of
// unsafe memory intrinsics seems to cause it to become paranoid about memory aliasing. Well, that, and it just produces
// terrible machine code in pretty much every critical code path we seem to have...
//
// Please never try to write performance critical code in Java. This is what it will do to you. And you will still be
// three times slower than the most naive solution in literally any other language that LLVM can compile.

// struct SectionRenderData { // 64 bytes
//   base_element: u32
//   mask: u32,
//   ranges: [VertexRange; 7]
// }
//
// struct VertexRange { // 8 bytes
//   offset: u32,
//   count: u32
// }

public class SectionRenderDataUnsafe {
    public static final int BASE_ELEMENT_MSB = 1 << 31;

    /**
     * When the "base element" field is not specified (indicated by setting the MSB to 0), the indices for the geometry set
     * should be sourced from a monotonic sequence (see {@link me.jellysquid.mods.sodium.client.render.chunk.SharedQuadIndexBuffer}).
     *
     * Otherwise, indices should be sourced from the index buffer for the render region using the specified offset.
     */
    private static final long OFFSET_BASE_ELEMENT = 0;

    private static final long OFFSET_SLICE_MASK = 4;
    private static final long OFFSET_SLICE_RANGES = 8;

    private static final long ALIGNMENT = 64;
    private static final long STRIDE = 64; // cache-line friendly! :)

    public static long allocateHeap(int count) {
        final var bytes = STRIDE * count;

        final var ptr = MemoryUtil.nmemAlignedAlloc(ALIGNMENT, bytes);
        MemoryUtil.memSet(ptr, 0, bytes);

        return ptr;
    }

    public static void freeHeap(long pointer) {
        MemoryUtil.nmemAlignedFree(pointer);
    }

    public static void clear(long pointer) {
        MemoryUtil.memSet(pointer, 0x0, STRIDE);
    }

    public static long heapPointer(long ptr, int index) {
        return ptr + (index * STRIDE);
    }

    public static void setSliceMask(long ptr, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_SLICE_MASK, value);
    }

    public static int getSliceMask(long ptr) {
        return MemoryUtil.memGetInt(ptr + OFFSET_SLICE_MASK);
    }

    public static void setBaseElement(long ptr, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_BASE_ELEMENT, value);
    }

    public static int getBaseElement(long ptr) {
        return MemoryUtil.memGetInt(ptr + OFFSET_BASE_ELEMENT);
    }

    public static void setVertexOffset(long ptr, int facing, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_SLICE_RANGES + (facing * 8L) + 0L, value);
    }

    public static int getVertexOffset(long ptr, int facing) {
        return MemoryUtil.memGetInt(ptr + OFFSET_SLICE_RANGES + (facing * 8L) + 0L);
    }

    public static void setElementCount(long ptr, int facing, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_SLICE_RANGES + (facing * 8L) + 4L, value);
    }

    public static int getElementCount(long ptr, int facing) {
        return MemoryUtil.memGetInt(ptr + OFFSET_SLICE_RANGES + (facing * 8L) + 4L);
    }
}