package com.chronogenesis.essence;

import org.jetbrains.annotations.Nullable;

/** A block that holds essence of a single aspect: a jug, an organism. */
public interface EssenceStorage {
    @Nullable
    EssenceContents contents();

    int capacity();

    boolean accepts(Aspect aspect);

    /** @return drams accepted */
    int insert(Aspect aspect, int drams, boolean simulate);

    /** @return drams taken, of {@link #contents()}'s aspect */
    int extract(int drams, boolean simulate);

    default int stored() {
        EssenceContents contents = contents();
        return contents == null ? 0 : contents.drams();
    }
}
