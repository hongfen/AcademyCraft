/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.academy.vanilla.meltdowner.entity.EntityMdShield;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;
import java.util.function.Predicate;

import static cn.lambdalib.util.generic.MathUtils.lerpf;
import static cn.lambdalib.util.generic.RandUtils.ranged;

/**
 * @author WeAthFolD
 */
public class LightShield extends Skill {

    public static final LightShield instance = new LightShield();
    
    static final int ACTION_INTERVAL = 18;
    static Predicate<Entity> basicSelector = EntitySelectors.everything();

    private LightShield() {
        super("light_shield", 2);
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new LSAction());
    }
    
    @SubscribeEvent
    public void onPlayerAttacked(LivingHurtEvent event) {
        if(event.entityLiving instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.entityLiving;
            LSAction action = ActionManager.findAction(player, LSAction.class);
            if(action != null) {
                event.ammount = action.handleAttacked(event.source, event.ammount);
                if(event.ammount == 0)
                    event.setCanceled(true);
            }
        }
    }
    
    public static class LSAction extends SkillSyncAction {

        int ticks;
        int lastAbsorb = -1; // The tick last the shield absorbed damage.

        float exp;
        
        public LSAction() {
            super(instance);
        }
        
        @Override
        public void onStart() {
            super.onStart();

            exp = ctx().getSkillExp();

            float overload = lerpf(198, 132, exp);
            ctx().consume(overload, 0);

            if(isRemote)
                startEffects();
        }
        
        @Override
        public void onTick() {
            ++ticks;
            
            if(isRemote)
                updateEffects();

            float cp = lerpf(12, 7, exp);
            if(!ctx().consume(0, cp) && !isRemote)
                ActionManager.endAction(this);
            ctx().addSkillExp(1e-6f);
            
            if(!isRemote) {
                // Find the entities that are 'colliding' with the shield.
                List<Entity> candidates = WorldUtils.getEntities(player, 3,
                        basicSelector.and(this::isEntityReachable).and(EntitySelectors.exclude(player)));
                for(Entity e : candidates) {
                    if(e.hurtResistantTime <= 0 && ctx().consume(getAbsorbOverload(), getAbsorbConsumption())) {
                        MDDamageHelper.attack(ctx(), e, getTouchDamage());
                        ctx().addSkillExp(.001f);
                    }
                }
            }
        }
        
        @Override
        public void onEnd() {
            ctx().setCooldown((int) lerpf(80, 60, exp));
        }
        
        @Override
        public void onFinalize() {
            if(isRemote)
                endEffects();
        }
        
        public float handleAttacked(DamageSource src, float damage) {
            if(damage == 0 || lastAbsorb != -1 && ticks - lastAbsorb <= ACTION_INTERVAL)
                return damage;
            
            Entity entity = src.getSourceOfDamage();
            boolean perform = false;
            if(entity != null) {
                if(isEntityReachable(entity))
                    perform = true;
            } else {
                perform = true;
            }
            
            if(perform) {
                lastAbsorb = ticks;
                if(ctx().consume(getAbsorbConsumption(), getAbsorbOverload())) {
                    float amt = getAbsorbDamage();
                    damage -= Math.min(damage, amt);
                }
            }
            
            ctx().addSkillExp(.001f);
            return damage;
        }
        
        //CLIENT
        @SideOnly(Side.CLIENT)
        EntityMdShield shield;
        
        @SideOnly(Side.CLIENT)
        FollowEntitySound loopSound;
        
        @SideOnly(Side.CLIENT)
        public void startEffects() {
            world.spawnEntityInWorld(shield = new EntityMdShield(player));
            ACSounds.playClient(player, "md.shield_startup", 0.8f);
            ACSounds.playClient(loopSound = new FollowEntitySound(player, "md.shield_loop").setLoop());
        }
        
        @SideOnly(Side.CLIENT)
        public void updateEffects() {
            if(RandUtils.nextFloat() < 0.3f) {
                Motion3D mo = new Motion3D(player, true).move(1);
                final double s = 0.5;
                mo.px += ranged(-s, s);
                mo.py += ranged(-s, s);
                mo.pz += ranged(-s, s);
                
                Particle p = MdParticleFactory.INSTANCE.next(world, 
                    VecUtils.vec(mo.px, mo.py, mo.pz),
                    VecUtils.vec(ranged(-.02, .02), ranged(-.01, .05), ranged(-.02, .02)));
                
                world.spawnEntityInWorld(p);
            }
        }
        
        @SideOnly(Side.CLIENT)
        public void endEffects() {
            shield.setDead();
            loopSound.stop();
        }

        float getAbsorbDamage() {
            return lerpf(15, 50, exp);
        }

        float getTouchDamage() {
            return lerpf(3, 8, exp);
        }

        float getAbsorbOverload() {
            return lerpf(15, 10, exp);
        }

        float getAbsorbConsumption() {
            return lerpf(50, 30, exp);
        }

        boolean isEntityReachable(Entity e) {
            double dx = e.posX - player.posX,
                    //dy = e.posY - player.posY,
                    dz = e.posZ - player.posZ;
            double yaw = -MathUtils.toDegrees(Math.atan2(dx, dz));
            return Math.abs(yaw - player.rotationYaw) % 360 < 60;
        }
        
    }

}
