package com.osrsd.cache.def;

import com.osrsd.cache.util.ByteBufferExt;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

@Data
@Slf4j
public final class SpriteDefinition implements Definition {

    private int id;
    private int width;
    private int height;
    private BufferedImage[] frames;

    public SpriteDefinition(int id) {
        this.id = id;
    }

    @Override
    public void decode(ByteBuffer buffer) {
        buffer.position(buffer.limit() - 2);
        int spriteCount = buffer.getShort() & 0xffff;
        frames = new BufferedImage[spriteCount];
        buffer.position(buffer.limit() - spriteCount * 8 - 7);
        width = buffer.getShort() & 0xffff;
        height = buffer.getShort() & 0xffff;
        int[] palette = new int[(buffer.get() & 0xff) + 1];
        int[] offsetsX = IntStream.range(0, spriteCount).map(i -> buffer.getShort() & 0xffff).toArray();
        int[] offsetsY = IntStream.range(0, spriteCount).map(i -> buffer.getShort() & 0xffff).toArray();
        int[] subWidths = IntStream.range(0, spriteCount).map(i -> buffer.getShort() & 0xffff).toArray();
        int[] subHeights = IntStream.range(0, spriteCount).map(i -> buffer.getShort() & 0xffff).toArray();
        buffer.position(buffer.limit() - spriteCount * 8 - 7 - (palette.length - 1) * 3);
        palette[0] = 0;
        IntStream.range(1, palette.length).forEach(index -> {
            palette[index] = ByteBufferExt.getMedium(buffer);
            if (palette[index] == 0) {
                palette[index] = 1;
            }
        });
        buffer.position(0);
        for (int id = 0; id < spriteCount; id++) {
            int subWidth = subWidths[id], subHeight = subHeights[id];
            int offsetX = offsetsX[id], offsetY = offsetsY[id];
            BufferedImage image = frames[id] = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[][] indices = new int[subWidth][subHeight];
            int flags = buffer.get() & 0xff;
            if (image != null) {
                if ((flags & 0x01) != 0) {
                    for (int x = 0; x < subWidth; x++) {
                        for (int y = 0; y < subHeight; y++) {
                            indices[x][y] = buffer.get() & 0xff;
                        }
                    }
                } else {
                    for (int y = 0; y < subHeight; y++) {
                        for (int x = 0; x < subWidth; x++) {
                            indices[x][y] = buffer.get() & 0xff;
                        }
                    }
                }
                if ((flags & 0x02) != 0) {
                    if ((flags & 0x01) != 0) {
                        for (int x = 0; x < subWidth; x++) {
                            for (int y = 0; y < subHeight; y++) {
                                int alpha = buffer.get() & 0xff;
                                image.setRGB(x + offsetX, y + offsetY, alpha << 24 | palette[indices[x][y]]);
                            }
                        }
                    } else {
                        for (int y = 0; y < subHeight; y++) {
                            for (int x = 0; x < subWidth; x++) {
                                int alpha = buffer.get() & 0xff;
                                image.setRGB(x + offsetX, y + offsetY, alpha << 24 | palette[indices[x][y]]);
                            }
                        }
                    }
                } else {
                    for (int x = 0; x < subWidth; x++) {
                        for (int y = 0; y < subHeight; y++) {
                            int index = indices[x][y];
                            if (index == 0) {
                                image.setRGB(x + offsetX, y + offsetY, 0);
                            } else {
                                image.setRGB(x + offsetX, y + offsetY, 0xff000000 | palette[index]);
                            }
                        }
                    }
                }
            }
        }
    }

}