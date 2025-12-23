package sys.exe.al.util.player;

import net.minecraft.util.math.Vec3d;

public class PositionData {

    public float pitch;
    public float yaw;
    public Vec3d pos;

    public PositionData (Vec3d a, float p, float y) {
        pos = a;
        pitch = p;
        yaw = y;
    }

}
