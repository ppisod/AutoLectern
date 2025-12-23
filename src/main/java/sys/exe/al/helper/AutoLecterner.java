package sys.exe.al.helper;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import sys.exe.al.ALState;
import sys.exe.al.DummyInput;
import sys.exe.al.util.player.BlockPosData;
import sys.exe.al.util.player.PositionData;
import sys.exe.al.util.player.PlayerUtility;
import sys.exe.al.util.signals.SignalManager;
import sys.exe.al.util.tool.ToolsUtility;
import sys.exe.al.util.villager.EnchantUtility;

import java.io.File;
import java.util.Objects;

public class AutoLecterner {

    public VillagerEntity FocusedVillager;
    public final Config Config;
    private PositionData ExpectedPlayerPosition;
    private BlockPosData LecternPosition;
    public SignalManager Signals;

    private ALState State = ALState.STOPPED;

    public ALState getState() {
        return State;
    }

    public void setState(ALState state) {
        State = state;
    }

    public VillagerEntity getFocusedVillager() {
        return FocusedVillager;
    }

    public void setFocusedVillager(VillagerEntity focusedVillager) {
        FocusedVillager = focusedVillager;
    }

    public BlockPosData getLecternPosition() {
        return LecternPosition;
    }

    public PositionData getExpectedPlayerPosition() {
        return ExpectedPlayerPosition;
    }

    public int attempts = 0;
    private int originalSlot;
    private int DelayTicks = 0;
    public int lastGoalMet = 0;
    public int UUID = 0;

    public void incUUID () {++UUID;}

    public void SendMessage (MinecraftClient mc, String content, Formatting color) {
        mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ")
                .formatted(Formatting.YELLOW)
                .append(
                        Text.literal(content)
                                .formatted(color)
                )
        );
    }

    public AutoLecterner(File config) {
        Config = new Config(config);
        Signals = new SignalManager();
    }

    public void Tick (final MinecraftClient mc) {
        if (State == ALState.STOPPED) return;

        final var player = mc.player;
        if (!PlayerUtility.PlayerValid(player)) State = ALState.STOPPING;

        final ClientWorld world;
        final ClientPlayerInteractionManager interaction;
        if ((world = mc.world) == null || (interaction = mc.interactionManager) == null) {
            Stop();
            return;
        }
        while (true) {

            switch (State) {
                case ALState.STOPPING -> {
                    StopAutoLectern(mc, player, world); return;
                }
                case ALState.STARTING -> StartAutoLectern (mc, player, world);
                case ALState.BREAKING -> {
                    boolean toReturn = BreakLectern (player, world, interaction);
                    if (toReturn) return;
                }
                case ALState.WAITING_ITEM -> {
                    boolean toReturn = WaitForLecterns (player);
                    if (toReturn) return;
                }
                case ALState.PLACING -> {
                    boolean toReturn = PlacingLecterns (player, world, interaction);
                    if (toReturn) return;
                }
                case ALState.WAITING_PROF -> {
                    boolean toReturn = WaitingForProfession (player, world, interaction);
                    if (toReturn) return;
                }
                case ALState.INTERACT_VIL -> InteractWithVillager(player, interaction);
                case ALState.WAITING_TRADE -> {
                    boolean toReturn = WaitingForTrade (mc, player);
                    if (toReturn) return;
                }
            }

        }
    }


    private void StartAutoLectern (final MinecraftClient mc, ClientPlayerEntity player, ClientWorld world) {
        BlockHitResult result = PlayerUtility.GetBlockLookingAt(mc);
        if (result == null || world.getBlockState(result.getBlockPos()).getBlock() != Blocks.LECTERN) {
            SendMessage(mc, "Please look at a lectern before running this command.", Formatting.RED);
            State = ALState.STOPPING;
            return;
        }
        player.input = new DummyInput();
        ExpectedPlayerPosition = new PositionData(player.getEntityPos(), player.getYaw(), player.getPitch());
        LecternPosition = new BlockPosData(result.getBlockPos(), result.getSide());
        State = ALState.BREAKING;
    }

    private boolean BreakLectern (ClientPlayerEntity player, ClientWorld world, ClientPlayerInteractionManager interaction) {

        AntiDrift(player);

        BackToOriginalSlot(player);

        if (Config.PreserveTool) {
            if (!ToolsUtility.CheckForToolAvailability(player)) {
                State = ALState.STOPPING;
                return false;
            }
        } else if (player.getMainHandStack().isEmpty())
            ToolsUtility.EquipAxes(player);

        BreakLecternIncrementally(world, interaction, player);

        if (world.getBlockState(LecternPosition.pos).isAir()) {
            State = Config.ItemSync ? ALState.WAITING_ITEM : ALState.PLACING;
            return true; // Take a break. :3
        }
        return true;
    }

    private boolean WaitForLecterns (ClientPlayerEntity player) {
        AntiDrift(player);
        if (Signals.isSet(SignalManager.Signal.ITEM)) {
            State = ALState.PLACING;
            return false;
        }
        return true;
    }

    private boolean PlacingLecterns (ClientPlayerEntity player, ClientWorld world, ClientPlayerInteractionManager interaction) {
        // is the block already a lectern? If so, wait for a trade
        if (world.getBlockState(LecternPosition.pos).isOf(Blocks.LECTERN))  GetReadyToWaitForTrade();

        // can we even place the block there?
        final BlockHitResult hr = PlayerUtility.GetBlockInView(player, ExpectedPlayerPosition.pitch, ExpectedPlayerPosition.yaw);
        if (hr == null) {Stop(); return false;}

        final var LecternHand = EquipItem(player, Items.LECTERN);
        Place(player, interaction, hr, LecternHand);

        if (!world.getBlockState(LecternPosition.pos).isOf(Blocks.LECTERN)) return true;

        GetReadyToWaitForTrade();
        return false;
    }

    private boolean WaitingForProfession (ClientPlayerEntity player, ClientWorld world, ClientPlayerInteractionManager interaction) {
        if (!Signals.isSet(SignalManager.Signal.PROF)) {State = ALState.INTERACT_VIL; return false;}

        AntiDrift(player);

        final var lectern = world.getBlockState(LecternPosition.pos);
        if (lectern.isAir()) {State = ALState.PLACING; return false;}
        if (lectern.getBlock() != Blocks.LECTERN) {State = ALState.BREAKING; return false;}
        if (DelayTicks > 0) {
            if (Config.PreBreak) {
                PreBreak(player, interaction, world);
            }
            --DelayTicks;
            return true;
        }
        State = ALState.BREAKING;
        return false;
    }

    private void InteractWithVillager (ClientPlayerEntity player, ClientPlayerInteractionManager interaction) {
        if (FocusedVillager == null) {
            Stop();
            return;
        }
        DelayTicks = 5;
        Signals.clearAll();
        State = ALState.WAITING_TRADE;
        final var villagePos = FocusedVillager.getEntityPos();
        final var eyePos = player.getEyePos();
        final var box = player.getBoundingBox().expand(20.0, 20.0, 20.0);
        final var hitResult = ProjectileUtil.raycast(player, eyePos, villagePos, box, x -> x.equals(FocusedVillager), 20);
        ActionResult actionResult = interaction.interactEntityAtLocation(player, FocusedVillager, hitResult, Hand.MAIN_HAND);
        if (!actionResult.isAccepted())
            actionResult = interaction.interactEntity(player, FocusedVillager, Hand.MAIN_HAND);
        if(actionResult instanceof ActionResult.Success successActionResult &&
                successActionResult.swingSource() == ActionResult.SwingSource.CLIENT)
            player.swingHand(Hand.MAIN_HAND);
    }

    private boolean WaitingForTrade (final MinecraftClient mc, ClientPlayerEntity player) {
        if (!Signals.isSet(SignalManager.Signal.TRADE)) {
            if (Signals.isSet(SignalManager.Signal.TRADE_OK)) {State = ALState.BREAKING; return false;}
            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1));
            GLFW.glfwRequestWindowAttention(mc.getWindow().getHandle());
            final var goal = Config.Goals.get(lastGoalMet);
            assert mc.world != null;
            final MutableText message = Text.literal("[Auto Lectern] ")
                    .formatted(Formatting.YELLOW)
                    .append(
                            Text.literal("Goal met: ")
                                    .formatted(Formatting.WHITE)
                    ).append(Objects.requireNonNull(EnchantUtility.enchantFromIdentifier(mc.world, goal.enchant()))
                            .value()
                            .description()
                            .copy()
                            .formatted(Formatting.GRAY));
            if (!Config.AutoRemove) {
                message.append(Text.literal(" [REMOVE]")
                        .setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent.RunCommand(
                                        "/autolec remove " + lastGoalMet + " " + UUID
                                ))
                        ).formatted(Formatting.RED)
                );
            } else {
                final var lastElemIdx = Config.Goals.size() - 1;
                Config.Goals.set(lastGoalMet, Config.Goals.get(lastElemIdx));
                Config.Goals.remove(lastElemIdx);
                ++UUID;
                message.append(Text.literal(" [AUTO REMOVED]").formatted(Formatting.RED));
            }
            mc.inGameHud.getChatHud().addMessage(message);
            mc.inGameHud.getChatHud().addMessage(
                    Text.literal("[Auto Lectern] ")
                            .formatted(Formatting.YELLOW)
                            .append(
                                    Text.literal("Completed.")
                                            .formatted(Formatting.GREEN)
                            )
            );
            State = ALState.STOPPING;
            return false;
        }
        AntiDrift(player);
        if (DelayTicks > 0) {
            if (Config.PreBreak) {
                PreBreak(player, mc.interactionManager, mc.world);
            }
            --DelayTicks;
            return true;
        }
        State = ALState.INTERACT_VIL;
        return false;
    }

    private void StopAutoLectern (final MinecraftClient mc, ClientPlayerEntity player, ClientWorld world) {
        if (player != null) {
            if (LecternPosition != null) {
                player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, LecternPosition.pos, LecternPosition.dir));
                world.setBlockBreakingInfo(player.getId(), LecternPosition.pos, -1);
            }
            player.input = new KeyboardInput(mc.options);
        }
        ExpectedPlayerPosition = null;
        originalSlot = -1;
        Signals.clearAll();
        LecternPosition = null;
        FocusedVillager = null;
        SendMessage(mc, "Stopped.", Formatting.RED);
        State = ALState.STOPPED;
    }

    /* --- helpers down here --- */

    private void PreBreak (final ClientPlayerEntity plr, @Nullable final ClientPlayerInteractionManager interactionManager, final ClientWorld world) {
        BackToOriginalSlot(plr);
        if(Config.PreserveTool)
            if (!ToolsUtility.CheckForToolAvailability(plr)) {
                State = ALState.STOPPING;
            }
        else if(plr.getMainHandStack().isEmpty())
            ToolsUtility.EquipAxes(plr);
        world.spawnBlockBreakingParticle(LecternPosition.pos, LecternPosition.dir);
        if(interactionManager == null)
            return;
        interactionManager.updateBlockBreakingProgress(LecternPosition.pos, LecternPosition.dir);
        plr.swingHand(Hand.MAIN_HAND);
    }

    private Hand EquipItem (ClientPlayerEntity p, Item item) {
        if (p.getMainHandStack().isOf(item)) return Hand.MAIN_HAND;
        if (p.getOffHandStack().isOf(item)) return Hand.OFF_HAND;
        final var inv = p.getInventory();
        int index = 0;
        for (final var ItemStack : inv.getMainStacks()) {
            if (ItemStack.getItem() != item) {
                ++index;
                continue;
            }
            if (!PlayerInventory.isValidHotbarIndex(index)) break;
            SetSlot(inv, index);
            return Hand.MAIN_HAND;
        }
        return null;
    }

    private void Place (ClientPlayerEntity p, ClientPlayerInteractionManager interaction, BlockHitResult hitResult, Hand hand) {
        if (hand == null) return;
        final var actionResult = interaction.interactBlock(p, hand, hitResult);
        if (actionResult instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.CLIENT) {p.swingHand(hand);
        }
    }

    private void AntiDrift (ClientPlayerEntity p) {
        if (ExpectedPlayerPosition == null) return;
        if (p == null) return;
        p.move(MovementType.SELF, new Vec3d(ExpectedPlayerPosition.pos.getX() - p.getX(), -0.00001, ExpectedPlayerPosition.pos.getZ() - p.getZ()));
    }

    private void BackToOriginalSlot (ClientPlayerEntity p) {
        if (originalSlot != -1) {
            p.getInventory().setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
    }

    private void SetSlot (PlayerInventory inv, int index) {
        originalSlot = inv.getSelectedSlot();
        inv.setSelectedSlot(index);
    }

    private void BreakLecternIncrementally (ClientWorld w, ClientPlayerInteractionManager m, ClientPlayerEntity p) {
        w.spawnBlockBreakingParticle(LecternPosition.pos, LecternPosition.dir);
        m.updateBlockBreakingProgress(LecternPosition.pos, LecternPosition.dir);
        p.swingHand(Hand.MAIN_HAND);
    }

    private void GetReadyToWaitForTrade () {
        FocusedVillager = null;
        DelayTicks = 40; // should this value be configurable?
        Signals.clearAll();
        State = ALState.WAITING_PROF;
    }

    private void Stop () {
        State = ALState.STOPPING;
    }

}
