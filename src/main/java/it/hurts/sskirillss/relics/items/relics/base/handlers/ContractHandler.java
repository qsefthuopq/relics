package it.hurts.sskirillss.relics.items.relics.base.handlers;

import it.hurts.sskirillss.relics.items.RelicContractItem;
import it.hurts.sskirillss.relics.items.relics.base.RelicItem;
import it.hurts.sskirillss.relics.network.NetworkHandler;
import it.hurts.sskirillss.relics.network.PacketPlayerMotion;
import it.hurts.sskirillss.relics.utils.NBTUtils;
import it.hurts.sskirillss.relics.utils.Reference;
import it.hurts.sskirillss.relics.utils.RelicUtils;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Reference.MODID)
public class ContractHandler {
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        ItemEntity drop = event.getItem();
        ItemStack stack = drop.getItem();
        PlayerEntity player = event.getPlayer();
        ServerWorld world = (ServerWorld) player.getCommandSenderWorld();

        if (!(stack.getItem() instanceof RelicItem) || world.getGameTime() - NBTUtils.getLong(stack, RelicContractItem.TAG_DATE, 0) >= (3600 * 20))
            return;

        String uuid = RelicUtils.Owner.getOwnerUUID(stack);

        if (player.isCreative() || uuid.equals("") || uuid.equals(player.getStringUUID()))
            return;

        drop.setPickUpDelay(40);

        Vector3d motion = player.position().subtract(drop.position()).normalize();

        NetworkHandler.sendToClient(new PacketPlayerMotion(motion.x(), motion.y(), motion.z()), (ServerPlayerEntity) player);
        drop.setDeltaMovement(motion.multiply(-1.25F, -1.25F, -1.25F));

        world.sendParticles(ParticleTypes.EXPLOSION, drop.getX(), drop.getY() + 0.5F, drop.getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, drop.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1F, 1F);

        event.setCanceled(true);
    }

    public static void handleOwner(PlayerEntity player, World world, ItemStack stack) {
        PlayerEntity owner = RelicUtils.Owner.getOwner(stack, world);

        if (owner == null || world.isClientSide())
            return;

        int contract = NBTUtils.getInt(stack, RelicContractItem.TAG_DATE, 0);

        if (contract == 0)
            NBTUtils.setInt(stack, RelicContractItem.TAG_DATE, (int) world.getGameTime());
        else if (world.getGameTime() - contract >= (3600 * 20)) {
            NBTUtils.setInt(stack, RelicContractItem.TAG_DATE, -1);

            RelicUtils.Owner.setOwnerUUID(stack, "");

            return;
        }

        if (!owner.getStringUUID().equals(player.getStringUUID()) && !player.isCreative() && !player.isSpectator()) {
            player.drop(stack.copy(), false, true);
            stack.shrink(1);

            player.setDeltaMovement(player.getViewVector(0F).multiply(-1F, -1F, -1F).normalize());

            world.addParticle(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1, player.getZ(), 0, 0, 0);
            world.playSound(player, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundCategory.PLAYERS, 1F, 1F);
        }
    }
}