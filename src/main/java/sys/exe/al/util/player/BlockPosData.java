package sys.exe.al.util.player;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockPosData {

    public BlockPos pos;
    public Direction dir;

    public BlockPosData (BlockPos p, Direction d) {
        pos = p;
        dir = d;
    }
}
