package com.rootbeerutils.client.bbe.sodium.render;

public class SodiumWorldRenderer {

    public static SodiumWorldRenderer instanceNullable() {
        return null;
    }

    public void scheduleRebuildForBlockArea(int minX, int minY, int minZ,
                                            int maxX, int maxY, int maxZ,
                                            boolean important) {
    }

    public void scheduleTerrainUpdate() {
    }
}
