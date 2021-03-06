package me.zeroeightsix.kami.mixin.client;

import me.zeroeightsix.kami.KamiMod;
import me.zeroeightsix.kami.event.events.EntityEvent;
import me.zeroeightsix.kami.event.events.MoveEntityFluidEvent;
import me.zeroeightsix.kami.feature.module.movement.SafeWalk;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Created by 086 on 16/11/2017.
 */
@Mixin(Entity.class)
public abstract class MixinEntity  {

    @Shadow public World world;

    @Redirect(method = "pushAwayFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V", ordinal = 0))
    public void addVelocity(Entity entity, double x, double y, double z) {
        EntityEvent.EntityCollision event = new EntityEvent.EntityCollision(entity, x, y, z);
        KamiMod.EVENT_BUS.post(event);
        if (event.isCancelled()) return;
        entity.addVelocity(event.getX(), event.getY(), event.getZ());
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z"))
    public boolean isSneaking(Entity entity) {
        return SafeWalk.shouldSafewalk() || entity.isSneaking();
    }

    @Redirect(method = "updateMovementInFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;getVelocity(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d getVelocity(FluidState fluidState, BlockView world, BlockPos pos) {
        Vec3d vec = fluidState.getVelocity(world, pos);
        MoveEntityFluidEvent event = new MoveEntityFluidEvent(((Entity) (Object) this), vec);
        KamiMod.EVENT_BUS.post(event);
        return event.isCancelled() ? Vec3d.ZERO : event.getMovement();
    }
}
