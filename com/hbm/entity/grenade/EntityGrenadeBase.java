package com.hbm.entity.grenade;

import org.apache.logging.log4j.Level;

import com.hbm.main.MainRegistry;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class EntityGrenadeBase extends EntityThrowable
{
    private static final String __OBFID = "CL_00001722";

    public EntityGrenadeBase(World p_i1773_1_)
    {
        super(p_i1773_1_);
    }

    public EntityGrenadeBase(World p_i1774_1_, EntityLivingBase p_i1774_2_)
    {
        super(p_i1774_1_, p_i1774_2_);
    }

    public EntityGrenadeBase(World p_i1775_1_, double p_i1775_2_, double p_i1775_4_, double p_i1775_6_)
    {
        super(p_i1775_1_, p_i1775_2_, p_i1775_4_, p_i1775_6_);
    }

    @Override
	protected void onImpact(MovingObjectPosition p_70184_1_)
    {
        if (p_70184_1_.entityHit != null)
        {
            byte b0 = 0;

            p_70184_1_.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), b0);
        }
        
        if(!worldObj.isRemote) {
    		if(MainRegistry.enableExtendedLogging) {
    			
    			String s = "null";
    			
    			if(getThrower() != null && getThrower() instanceof EntityPlayer)
    				s = ((EntityPlayer)getThrower()).getDisplayName();
    			
    			MainRegistry.logger.log(Level.INFO, "[GREN] Set off grenade at " + ((int)posX) + " / " + ((int)posY) + " / " + ((int)posZ) + " by " + s + "!");
    		}
        }
        
        this.explode();
    }
    
    public abstract void explode();
    
}
