package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.interfaces.ITankPacketAcceptor;
import com.hbm.items.ModItems;
import com.hbm.packet.AuxGaugePacket;
import com.hbm.packet.FluidTankPacket;
import com.hbm.packet.LargeReactorPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.saveddata.RadiationSavedData;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

public class TileEntityMachineReactorLarge extends TileEntity implements ITickable, IFluidHandler, ITankPacketAcceptor {

	public ItemStackHandler inventory;
	public ICapabilityProvider dropProvider;

	public int hullHeat;
	public final int maxHullHeat = 100000;
	public int coreHeat;
	public final int maxCoreHeat = 50000;
	public int rods;
	public final int rodsMax = 100;
	public int age = 0;
	public FluidTank[] tanks;
	public Fluid[] tankTypes;
	public ReactorFuelType type;
	public int fuel;
	public int maxFuel = 240 * fuelMult;
	public int waste;
	public int maxWaste = 240 * fuelMult;
	public int compression;

	public static int fuelMult = 1000;
	public static int cycleDuration = 24000;
	private static int fuelBase = 240 * fuelMult;
	//private static int waterBase = 128 * 1000;
	//private static int coolantBase = 64 * 1000;
	//private static int steamBase = 32 * 1000;

	//private static final int[] slots_top = new int[] { 0 };
	//private static final int[] slots_bottom = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 16 };
	//private static final int[] slots_side = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 16 };

	private String customName;
	
	public TileEntityMachineReactorLarge() {
		inventory = new ItemStackHandler(8){
			@Override
			protected void onContentsChanged(int slot) {
				markDirty();
				super.onContentsChanged(slot);
			}
		};
		dropProvider = new ICapabilityProvider(){

			@Override
			public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
				return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
			}

			@Override
			public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
				return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : null;
			}
			
		};
		tanks = new FluidTank[3];
		tankTypes = new Fluid[3];
		tanks[0] = new FluidTank(128000);
		tankTypes[0] = FluidRegistry.WATER;
		tanks[1] = new FluidTank(64000);
		tankTypes[1] = ModForgeFluids.coolant;
		tanks[2] = new FluidTank(32000);
		tankTypes[2] = ModForgeFluids.steam;
		type = ReactorFuelType.URANIUM;
		compression = 0;
	}
	
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container.reactorLarge";
	}

	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}

	public void setCustomName(String name) {
		this.customName = name;
	}
	
	public boolean isUseableByPlayer(EntityPlayer player) {
		if (world.getTileEntity(pos) != this) {
			return false;
		} else {
			return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64;
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		coreHeat = compound.getInteger("heat");
		hullHeat = compound.getInteger("hullHeat");
		rods = compound.getInteger("rods");
		fuel = compound.getInteger("fuel");
		waste = compound.getInteger("waste");
		compression = compound.getInteger("compression");
		tankTypes[0] = FluidRegistry.WATER;
		tankTypes[1] = ModForgeFluids.coolant;
		if(compression == 0){
			tankTypes[2] = ModForgeFluids.steam;
		} else if(compression == 1){
			tankTypes[2] = ModForgeFluids.hotsteam;
		} else if(compression == 2){
			tankTypes[2] = ModForgeFluids.superhotsteam;
		}
		type = ReactorFuelType.getEnum(compound.getInteger("type"));
		if(compound.hasKey("inventory"))
			inventory.deserializeNBT(compound.getCompoundTag("inventory"));
		if(compound.hasKey("tanks"))
			FFUtils.deserializeTankArray(compound.getTagList("tanks", 10), tanks);
		super.readFromNBT(compound);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setInteger("heat", coreHeat);
		compound.setInteger("hullHeat", hullHeat);
		compound.setInteger("rods", rods);
		compound.setInteger("fuel", fuel);
		compound.setInteger("waste", waste);
		compound.setInteger("compression", compression);
		compound.setInteger("type", type.getID());
		compound.setTag("inventory", inventory.serializeNBT());
		compound.setTag("tanks", FFUtils.serializeTankArray(tanks));
		return super.writeToNBT(compound);
	}
	
	public int getCoreHeatScaled(int i) {
		return (coreHeat * i) / maxCoreHeat;
	}

	public int getHullHeatScaled(int i) {
		return (hullHeat * i) / maxHullHeat;
	}

	public int getFuelScaled(int i) {
		return (fuel * i) / maxFuel;
	}

	public int getWasteScaled(int i) {
		return (waste * i) / maxWaste;
	}

	public int getSteamScaled(int i) {
		return (tanks[2].getFluidAmount() * i) / tanks[2].getCapacity();
	}

	public boolean hasCoreHeat() {
		return coreHeat > 0;
	}

	public boolean hasHullHeat() {
		return hullHeat > 0;
	}
	
	public void compress(int level){
		if(level == compression)
			return;
		if(level >= 0 && level < 3){
			if(compression == 0){
				if(level == 1){
					tankTypes[2] = ModForgeFluids.hotsteam;
					int newAmount = (int) (tanks[2].getFluidAmount()/10D);
					tanks[2].drain(tanks[2].getCapacity(), true);
					tanks[2].fill(new FluidStack(tankTypes[2], newAmount), true);
				}
				if(level == 2){
					tankTypes[2] = ModForgeFluids.superhotsteam;
					int newAmount = (int) (tanks[2].getFluidAmount()/100D);
					tanks[2].drain(tanks[2].getCapacity(), true);
					tanks[2].fill(new FluidStack(tankTypes[2], newAmount), true);
				}
			}
			if(compression == 1){
				if(level == 0){
					tankTypes[2] = ModForgeFluids.steam;
					int newAmount = (int) (tanks[2].getFluidAmount()*10);
					tanks[2].drain(tanks[2].getCapacity(), true);
					tanks[2].fill(new FluidStack(tankTypes[2], newAmount), true);
				}
				if(level == 2){
					tankTypes[2] = ModForgeFluids.superhotsteam;
					int newAmount = (int) (tanks[2].getFluidAmount()/10D);
					tanks[2].drain(tanks[2].getCapacity(), true);
					tanks[2].fill(new FluidStack(tankTypes[2], newAmount), true);
				}
			}
			if(compression == 2){
				if(level == 0){
					tankTypes[2] = ModForgeFluids.steam;
					int newAmount = (int) (tanks[2].getFluidAmount()*100);
					tanks[2].drain(tanks[2].getCapacity(), true);
					tanks[2].fill(new FluidStack(tankTypes[2], newAmount), true);
				}
				if(level == 1){
					tankTypes[2] = ModForgeFluids.hotsteam;
					int newAmount = (int) (tanks[2].getFluidAmount()*10D);
					tanks[2].drain(tanks[2].getCapacity(), true);
					tanks[2].fill(new FluidStack(tankTypes[2], newAmount), true);
				}
			}
			
			compression = level;
		}
	}
	
	public boolean checkBody() {
		MutableBlockPos mPos = new BlockPos.MutableBlockPos();
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		return world.getBlockState(mPos.setPos(x + 1, y, z + 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x - 1, y, z + 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x - 1, y, z - 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x + 1, y, z - 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x + 1, y, z)).getBlock() == ModBlocks.reactor_control &&
				world.getBlockState(mPos.setPos(x - 1, y, z)).getBlock() == ModBlocks.reactor_control &&
				world.getBlockState(mPos.setPos(x, y, z + 1)).getBlock() == ModBlocks.reactor_control &&
				world.getBlockState(mPos.setPos(x, y, z - 1)).getBlock() == ModBlocks.reactor_control;
	}
	
	public boolean checkSegment(int offset) {
		MutableBlockPos mPos = new BlockPos.MutableBlockPos();
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		return world.getBlockState(mPos.setPos(x + 1, y + offset, z + 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x - 1, y + offset, z + 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x - 1, y + offset, z - 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x + 1, y + offset, z - 1)).getBlock() == ModBlocks.reactor_element &&
				world.getBlockState(mPos.setPos(x + 1, y + offset, z)).getBlock() == ModBlocks.reactor_control &&
				world.getBlockState(mPos.setPos(x - 1, y + offset, z)).getBlock() == ModBlocks.reactor_control &&
				world.getBlockState(mPos.setPos(x, y + offset, z + 1)).getBlock() == ModBlocks.reactor_control &&
				world.getBlockState(mPos.setPos(x, y + offset, z - 1)).getBlock() == ModBlocks.reactor_control &&
				world.getBlockState(mPos.setPos(x, y + offset, z)).getBlock() == ModBlocks.reactor_conductor;
	}
	
	private float checkHull() {
		
		float max = getSize() * 12;
		float count = 0;
		MutableBlockPos mPos = new BlockPos.MutableBlockPos();
		int x = pos.getX();
		int z = pos.getZ();
		for(int y = pos.getY() - depth; y <= pos.getY() + height; y++) {
			
			if(blocksRad(mPos.setPos(x - 1, y, z + 2)))
				count++;
			if(blocksRad(mPos.setPos(x, y, z + 2)))
				count++;
			if(blocksRad(mPos.setPos(x + 1, y, z + 2)))
				count++;

			if(blocksRad(mPos.setPos(x - 1, y, z - 2)))
				count++;
			if(blocksRad(mPos.setPos(x, y, z - 2)))
				count++;
			if(blocksRad(mPos.setPos(x + 1, y, z - 2)))
				count++;

			if(blocksRad(mPos.setPos(x + 2, y, z - 1)))
				count++;
			if(blocksRad(mPos.setPos(x + 2, y, z)))
				count++;
			if(blocksRad(mPos.setPos(x + 2, y, z + 1)))
				count++;
			
			if(blocksRad(mPos.setPos(x - 2, y, z - 1)))
				count++;
			if(blocksRad(mPos.setPos(x - 2, y, z)))
				count++;
			if(blocksRad(mPos.setPos(x - 2, y, z + 1)))
				count++;
		}
		
		if(count == 0)
			return 1;

		//System.out.println(count + "/" + max);
		
		return 1 - (count / max);
	}
	
	private boolean blocksRad(BlockPos pos) {
		
		Block b = world.getBlockState(pos).getBlock();
		
		if(b == ModBlocks.block_lead || b == ModBlocks.block_desh || b == ModBlocks.brick_concrete)
			return true;
		
		if(b.getExplosionResistance(world, pos, null, null) >= 100)
			return true;
		
		return false;
	}
	
	int height;
	int depth;
	public int size;
	
	private void caluclateSize() {
		
		height = 0;
		depth = 0;
		
		for(int i = 0; i < 7; i++) {
			
			if(checkSegment(i + 1))
				height++;
			else
				break;
		}
		
		for(int i = 0; i < 7; i++) {
			
			if(checkSegment(-i - 1))
				depth++;
			else
				break;
		}
		
		size = height + depth + 1;
	}
	
	private int getSize() {
		return size;
	}
	
	private void generate() {
		
		int consumption = (maxFuel / cycleDuration) * rods / 100;
		
		if(consumption > fuel)
			consumption = fuel;
		
		if(consumption + waste > maxWaste)
			consumption = maxWaste - waste;
		
		fuel -= consumption;
		waste += consumption;
		
		int heat = (consumption / size) * type.heat / fuelMult;
		
		this.coreHeat += heat;
		
		if(consumption > 0 || heat > 0)
			markDirty();
	}
	
	@Override
	public void update() {
		if (!world.isRemote && checkBody()) {

			age++;
			if (age >= 20) {
				age = 0;
			}

			if (age == 9 || age == 19)
				fillFluidInit(tanks[2]);
			
			caluclateSize();
			PacketDispatcher.wrapper.sendToAllAround(new AuxGaugePacket(pos, size, 0), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 15));
			PacketDispatcher.wrapper.sendToAllAround(new FluidTankPacket(pos, new FluidTank[]{tanks[0], tanks[1], tanks[2]}), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 15));
		}
		
		//tanks[0] = FFUtils.changeTankSize(tanks[0], waterBase * getSize());
		//tanks[1] = FFUtils.changeTankSize(tanks[1], coolantBase * getSize());
		//tanks[2] = FFUtils.changeTankSize(tanks[2], steamBase * getSize());
		
		maxWaste = maxFuel = fuelBase * getSize();
			
		if(!world.isRemote) {
			
			if(waste > maxWaste)
				waste = maxWaste;
			
			if(fuel > maxFuel)
				fuel = maxFuel;
			
			if(this.inputValidForTank(0, 0))
				FFUtils.fillFromFluidContainer(inventory, tanks[0], 0, 1);

			if(this.inputValidForTank(1, 2))
				FFUtils.fillFromFluidContainer(inventory, tanks[1], 2, 3);

			
			//Change fuel type if empty
			if(fuel == 0) {
				
				if(!getFuelType(inventory.getStackInSlot(4).getItem()).toString().equals(ReactorFuelType.UNKNOWN.toString())) {
					
					this.type = getFuelType(inventory.getStackInSlot(4).getItem());
					this.waste = 0;
					
				}
			}
			
			//Load fuel
			if(getFuelContent(inventory.getStackInSlot(4).getItem(), type) > 0) {
				
				int cont = getFuelContent(inventory.getStackInSlot(4).getItem(), type) * fuelMult;
				
				if(fuel + cont <= maxFuel) {
					
					if(!inventory.getStackInSlot(4).getItem().hasContainerItem(inventory.getStackInSlot(4))) {
						inventory.getStackInSlot(4).shrink(1);
						fuel += cont;
						
					} else if(inventory.getStackInSlot(5).isEmpty()) {
						
						inventory.setStackInSlot(5, new ItemStack(inventory.getStackInSlot(4).getItem().getContainerItem()));
						inventory.getStackInSlot(4).shrink(1);
						fuel += cont;
						
					} else if(inventory.getStackInSlot(4).getItem().getContainerItem() == inventory.getStackInSlot(5).getItem() && inventory.getStackInSlot(5).getCount() < inventory.getStackInSlot(5).getMaxStackSize()) {
						
						inventory.getStackInSlot(4).shrink(1);
						inventory.getStackInSlot(5).grow(1);
						fuel += cont;
						
					}
					
					if(inventory.getStackInSlot(4).isEmpty())
						inventory.setStackInSlot(4, ItemStack.EMPTY);
				}
			}
			
			//Unload waste
			if(getWasteAbsorbed(inventory.getStackInSlot(6).getItem(), type) > 0) {
				
				int absorbed = getWasteAbsorbed(inventory.getStackInSlot(6).getItem(), type) * fuelMult;
				
				if(absorbed <= waste) {
					
					if(inventory.getStackInSlot(7).isEmpty()) {

						waste -= absorbed;
						inventory.setStackInSlot(7, new ItemStack(getWaste(inventory.getStackInSlot(6).getItem(), type)));
						inventory.getStackInSlot(6).shrink(1);
						
					} else if(inventory.getStackInSlot(7).getItem() == getWaste(inventory.getStackInSlot(6).getItem(), type) && inventory.getStackInSlot(7).getCount() < inventory.getStackInSlot(7).getMaxStackSize()) {

						waste -= absorbed;
						inventory.getStackInSlot(7).grow(1);
						inventory.getStackInSlot(6).shrink(1);
					}
					
					if(inventory.getStackInSlot(6).isEmpty())
						inventory.setStackInSlot(6, ItemStack.EMPTY);
				}
				
			}
			
			if(rods > 0)
				generate();

			if (this.coreHeat > 0 && this.tanks[1].getFluidAmount() > 0 && this.hullHeat < this.maxHullHeat) {
				this.hullHeat += this.coreHeat * 0.175;
				this.coreHeat -= this.coreHeat * 0.1;

				this.tanks[1].drain(10, true);
			}

			if (this.hullHeat > maxHullHeat) {
				this.hullHeat = maxHullHeat;
			}

			if (this.hullHeat > 0 && this.tanks[0].getFluidAmount() > 0) {
				generateSteam();
				this.hullHeat -= this.hullHeat * 0.085;
			}

			if (this.coreHeat > maxCoreHeat) {
				this.explode();
			}

			if (rods > 0 && coreHeat > 0 && age == 5) {

				float rad = (float)coreHeat / (float)maxCoreHeat * 50F;
				//System.out.println(rad);
				rad *= checkHull();
				//System.out.println(rad);
				
				RadiationSavedData.incrementRad(world, pos.getX(), pos.getZ(), rad, 50 * 4);
			}

			MutableBlockPos mPos = new BlockPos.MutableBlockPos();
			int x = pos.getX();
			int y = pos.getY();
			int z = pos.getZ();
			
			if(world.getBlockState(mPos.setPos(x, y, z - 2)).getBlock() == ModBlocks.reactor_ejector && world.getBlockState(mPos.setPos(x, y, z - 2)).getValue(BlockHorizontal.FACING) == EnumFacing.NORTH)
				tryEjectInto(mPos.setPos(x, y, z - 3));
			if(world.getBlockState(mPos.setPos(x, y, z + 2)).getBlock() == ModBlocks.reactor_ejector && world.getBlockState(mPos.setPos(x, y, z + 2)).getValue(BlockHorizontal.FACING) == EnumFacing.SOUTH)
				tryEjectInto(mPos.setPos(x, y, z + 3));
			if(world.getBlockState(mPos.setPos(x - 2, y, z)).getBlock() == ModBlocks.reactor_ejector && world.getBlockState(mPos.setPos(x - 2, y, z)).getValue(BlockHorizontal.FACING) == EnumFacing.WEST)
				tryEjectInto(mPos.setPos(x - 3, y, z));
			if(world.getBlockState(mPos.setPos(x + 2, y, z)).getBlock() == ModBlocks.reactor_ejector && world.getBlockState(mPos.setPos(x + 2, y, z)).getValue(BlockHorizontal.FACING) == EnumFacing.EAST)
				tryEjectInto(mPos.setPos(x + 3, y, z));

			if(world.getBlockState(mPos.setPos(x, y, z - 2)).getBlock() == ModBlocks.reactor_inserter && world.getBlockState(mPos.setPos(x, y, z - 2)).getValue(BlockHorizontal.FACING) == EnumFacing.NORTH)
				tryInsertFrom(mPos.setPos(x, y, z - 3));
			if(world.getBlockState(mPos.setPos(x, y, z + 2)).getBlock() == ModBlocks.reactor_inserter && world.getBlockState(mPos.setPos(x, y, z + 2)).getValue(BlockHorizontal.FACING) == EnumFacing.SOUTH)
				tryInsertFrom(mPos.setPos(x, y, z + 3));
			if(world.getBlockState(mPos.setPos(x - 2, y, z)).getBlock() == ModBlocks.reactor_inserter && world.getBlockState(mPos.setPos(x - 2, y, z)).getValue(BlockHorizontal.FACING) == EnumFacing.WEST)
				tryInsertFrom(mPos.setPos(x - 3, y, z));
			if(world.getBlockState(mPos.setPos(x + 2, y, z)).getBlock() == ModBlocks.reactor_inserter && world.getBlockState(mPos.setPos(x + 2, y, z)).getValue(BlockHorizontal.FACING) == EnumFacing.EAST)
				tryInsertFrom(mPos.setPos(x + 3, y, z));

			PacketDispatcher.wrapper.sendToAllAround(new LargeReactorPacket(pos, rods, coreHeat, hullHeat, fuel, waste, type.getID()), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 15));
		}
	}
	
	protected boolean inputValidForTank(int tank, int slot){
		if(!inventory.getStackInSlot(slot).isEmpty() && tanks[tank] != null){
			if(isValidFluidForTank(tank, FluidUtil.getFluidContained(inventory.getStackInSlot(slot)))){
				return true;
			}
		}
		return false;
	}
	
	private boolean isValidFluidForTank(int tank, FluidStack stack) {
		if(stack == null || tanks[tank] == null)
			return false;
		return stack.getFluid() == tankTypes[tank];
	}
	
	private void tryEjectInto(BlockPos pos) {
		
		int wSize = type.toString().equals(ReactorFuelType.SCHRABIDIUM.toString()) ? 60 * fuelMult : 6 * fuelMult;
		
		if(waste < wSize)
			return;
		
		TileEntity te = world.getTileEntity(pos);
		
		if(te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)){
			IItemHandler chest = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			
			Item waste = ModItems.waste_uranium_hot;
			
			switch(type) {
			case PLUTONIUM:
				waste = ModItems.waste_plutonium_hot;
				break;
			case MOX:
				waste = ModItems.waste_mox_hot;
				break;
			case SCHRABIDIUM:
				waste = ModItems.waste_schrabidium_hot;
				break;
			case THORIUM:
				waste = ModItems.waste_thorium_hot;
				break;
			default:
				waste = ModItems.waste_uranium_hot;
				break;
			}
			
			for(int i = 0; i < chest.getSlots(); i ++){
				if(chest.insertItem(i, new ItemStack(waste), false).isEmpty()){
					this.waste -= wSize;
				}
			}
		}
		
		/*if(te instanceof IInventory) {
			
			IInventory chest = (IInventory)te;
			
			Item waste = ModItems.waste_uranium_hot;
			
			switch(type) {
			case PLUTONIUM:
				waste = ModItems.waste_plutonium_hot;
				break;
			case MOX:
				waste = ModItems.waste_mox_hot;
				break;
			case SCHRABIDIUM:
				waste = ModItems.waste_schrabidium_hot;
				break;
			case THORIUM:
				waste = ModItems.waste_thorium_hot;
				break;
			default:
				waste = ModItems.waste_uranium_hot;
				break;
			}
			
			for(int i = 0; i < chest.getSizeInventory(); i++) {
				
				if(chest.isItemValidForSlot(i, new ItemStack(waste)) && chest.getStackInSlot(i) != null && chest.getStackInSlot(i).getItem() == waste && chest.getStackInSlot(i).stackSize < chest.getStackInSlot(i).getMaxStackSize()) {
					chest.setInventorySlotContents(i, new ItemStack(waste, chest.getStackInSlot(i).stackSize + 1));
					this.waste -= wSize;
					return;
				}
			}
			
			for(int i = 0; i < chest.getSizeInventory(); i++) {
				
				if(chest.isItemValidForSlot(i, new ItemStack(waste)) && chest.getStackInSlot(i) == null) {
					chest.setInventorySlotContents(i, new ItemStack(waste));
					this.waste -= wSize;
					return;
				}
			}
		}*/
	}
	
	private void tryInsertFrom(BlockPos pos) {
		
		TileEntity te = world.getTileEntity(pos);
		
		if(te != null && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)){
			IItemHandler check = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			if(check instanceof IItemHandlerModifiable){
				IItemHandlerModifiable chest = (IItemHandlerModifiable)check;
				if(fuel > 0){
					for(int i = 0; i < chest.getSlots(); i ++){
						if(!chest.getStackInSlot(i).isEmpty()){
							int cont = getFuelContent(chest.getStackInSlot(i).getItem(), type) * fuelMult;
							
							if(cont > 0 && fuel + cont <= maxFuel){
								Item container =  chest.getStackInSlot(i).getItem().getContainerItem();
								
								chest.getStackInSlot(i).shrink(1);
								if(chest.getStackInSlot(i).isEmpty())
									chest.setStackInSlot(i, ItemStack.EMPTY);
								fuel += cont;
								if(chest.getStackInSlot(i).isEmpty() && container != null)
									chest.setStackInSlot(i, new ItemStack(container));
							}
						}
					}
				} else {
					for(int i = 0; i < chest.getSlots(); i++) {
						
						if(!chest.getStackInSlot(i).isEmpty()) {
							int cont = getFuelContent(chest.getStackInSlot(i).getItem(), getFuelType(chest.getStackInSlot(i).getItem())) * fuelMult;
							if(cont > 0 && fuel + cont <= maxFuel) {
								
								Item container =  chest.getStackInSlot(i).getItem().getContainerItem();
								
								type = getFuelType(chest.getStackInSlot(i).getItem());
								chest.getStackInSlot(i).shrink(1);
								if(chest.getStackInSlot(i).isEmpty())
									chest.setStackInSlot(i, ItemStack.EMPTY);
								fuel += cont;
								
								if(chest.getStackInSlot(i).isEmpty() && container != null)
									chest.setStackInSlot(i, new ItemStack(container));
							}
						}
					}
				}
			}
		}
		
		/*if(te instanceof IInventory) {
			
			IInventory chest = (IInventory)te;
			
			if(fuel > 0) {
				for(int i = 0; i < chest.getSizeInventory(); i++) {
					
					if(chest.getStackInSlot(i) != null) {
						int cont = getFuelContent(chest.getStackInSlot(i).getItem(), type) * fuelMult;
						
						if(cont > 0 && fuel + cont <= maxFuel) {
							
							Item container =  chest.getStackInSlot(i).getItem().getContainerItem();
							
							chest.decrStackSize(i, 1);
							fuel += cont;
							
							if(chest.getStackInSlot(i) == null && container != null)
								chest.setInventorySlotContents(i, new ItemStack(container));
						}
					}
				}
			} else {
				for(int i = 0; i < chest.getSizeInventory(); i++) {
					
					if(chest.getStackInSlot(i) != null) {
						int cont = getFuelContent(chest.getStackInSlot(i).getItem(), getFuelType(chest.getStackInSlot(i).getItem())) * fuelMult;
						
						if(cont > 0 && fuel + cont <= maxFuel) {
							
							Item container =  chest.getStackInSlot(i).getItem().getContainerItem();
							
							type = getFuelType(chest.getStackInSlot(i).getItem());
							chest.decrStackSize(i, 1);
							fuel += cont;
							
							if(chest.getStackInSlot(i) == null && container != null)
								chest.setInventorySlotContents(i, new ItemStack(container));
						}
					}
				}
			}
		}*/
	}
	
	private void generateSteam() {

		//function of SHS produced per tick
		//maxes out at heat% * tank capacity / 20
		
		double statSteMaFiFiLe = 8000;
		
		double steam = (((double)hullHeat / (double)maxHullHeat) * (/*(double)tanks[2].getMaxFill()*/statSteMaFiFiLe / 50D)) * size;
		
		double water = steam;
		
		if(tankTypes[2] == ModForgeFluids.steam){
			water /= 100D;
		} else if(tankTypes[2] == ModForgeFluids.hotsteam){
			water /= 10;
		} else if(tankTypes[2] == ModForgeFluids.superhotsteam){
			
		}
		
		tanks[0].drain((int)Math.ceil(water), true);
		tanks[2].fill(new FluidStack(tankTypes[2], (int)Math.floor(steam)), true);
		
	}

	private void explode() {
		for (int i = 0; i < inventory.getSlots(); i++) {
			inventory.setStackInSlot(i, ItemStack.EMPTY);
		}

		int rad = (int)(((long)fuel) * 25000L / (fuelBase * 15L));
		
		RadiationSavedData.incrementRad(world, pos.getX(), pos.getZ(), rad, 75000);

		world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 7.5F, true);
		ExplosionNukeGeneric.waste(world, pos.getX(), pos.getY(), pos.getZ(), 35);
		
		for(int i = pos.getY() - depth; i <= pos.getY() + height; i++) {

			if(world.rand.nextInt(2) == 0) {
				randomizeRadBlock(pos.getX() + 1, i, pos.getZ() + 1);
			}
			if(world.rand.nextInt(2) == 0) {
				randomizeRadBlock(pos.getX() + 1, i, pos.getZ() - 1);
			}
			if(world.rand.nextInt(2) == 0) {
				randomizeRadBlock(pos.getX() - 1, i, pos.getZ() - 1);
			}
			if(world.rand.nextInt(2) == 0) {
				randomizeRadBlock(pos.getX() - 1, i, pos.getZ() + 1);
			}
			
			if(world.rand.nextInt(5) == 0) {
				world.createExplosion(null, pos.getX(), pos.getY(), pos.getZ(), 5.0F, true);
			}
		}
		
		world.setBlockState(pos, ModBlocks.sellafield_core.getDefaultState());
	}
	
	private void randomizeRadBlock(int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		
		int rand = world.rand.nextInt(20);
		
		if(rand < 7)
			world.setBlockState(pos, ModBlocks.toxic_block.getDefaultState());
		else if(rand < 10)
			world.setBlockState(pos, ModBlocks.sellafield_0.getDefaultState());
		else if(rand < 14)
			world.setBlockState(pos, ModBlocks.sellafield_1.getDefaultState());
		else if(rand < 17)
			world.setBlockState(pos, ModBlocks.sellafield_2.getDefaultState());
		else if(rand < 19)
			world.setBlockState(pos, ModBlocks.sellafield_3.getDefaultState());
		else
			world.setBlockState(pos, ModBlocks.sellafield_4.getDefaultState());
	}
	
	public void fillFluidInit(FluidTank tank) {
		MutableBlockPos mPos = new BlockPos.MutableBlockPos();
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		if(world.getBlockState(mPos.setPos(x - 2, y, z)).getBlock() == ModBlocks.reactor_hatch)
			FFUtils.fillFluid(this, tank, world, new BlockPos(pos.getX() - 3, pos.getY(), pos.getZ()), 2000);
		
		if(world.getBlockState(mPos.setPos(x + 2, y, z)).getBlock() == ModBlocks.reactor_hatch)
			FFUtils.fillFluid(this, tank, world, new BlockPos(pos.getX() + 3, pos.getY(), pos.getZ()), 2000);
		
		if(world.getBlockState(mPos.setPos(x, y, z - 2)).getBlock() == ModBlocks.reactor_hatch)
			FFUtils.fillFluid(this, tank, world, new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 3), 2000);
		
		if(world.getBlockState(mPos.setPos(x, y, z + 2)).getBlock() == ModBlocks.reactor_hatch)
			FFUtils.fillFluid(this, tank, world, new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 3), 2000);

		FFUtils.fillFluid(this, tank, world, new BlockPos(pos.getX(), pos.getY() + height + 1, pos.getZ()), 2000);
		
		FFUtils.fillFluid(this, tank, world, new BlockPos(pos.getX(), pos.getY() - depth - 1, pos.getZ() + 3), 2000);
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return new IFluidTankProperties[]{tanks[0].getTankProperties()[0], tanks[1].getTankProperties()[0], tanks[2].getTankProperties()[0]};
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(resource == null){
			return 0;
		} else if(resource.getFluid() == tankTypes[0]){
			return tanks[0].fill(resource, doFill);
		} else if(resource.getFluid() == tankTypes[1]){
			return tanks[1].fill(resource, doFill);
		} else {
			return 0;
		}
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(resource != null && resource.getFluid() == tankTypes[2]){
			return tanks[2].drain(resource.amount, doDrain);
		} else {
			return null;
		}
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		return tanks[2].drain(maxDrain, doDrain);
	}
	
	@Override
	public void recievePacket(NBTTagCompound[] tags) {
		if(tags.length != 3){
			return;
		} else {
			tanks[0].readFromNBT(tags[0]);
			tanks[1].readFromNBT(tags[1]);
			tanks[2].readFromNBT(tags[2]);
		}
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this) : super.getCapability(capability, facing);
	}
	
	public enum ReactorFuelType {

		URANIUM(250000),
		THORIUM(200000),
		PLUTONIUM(312500),
		MOX(250000),
		SCHRABIDIUM(2085000),
		UNKNOWN(1);
		
		private ReactorFuelType(int i) {
			heat = i;
		}
		
		//Heat per nugget burned
		private int heat;
		
		public int getHeat() {
			return heat;
		}
		
		public int getID() {
			return Arrays.asList(ReactorFuelType.values()).indexOf(this);
		}
		
		public static ReactorFuelType getEnum(int i) {
			if(i < ReactorFuelType.values().length)
				return ReactorFuelType.values()[i];
			else
				return ReactorFuelType.URANIUM;
		}
	}
	
	static class ReactorFuelEntry {
		
		int value;
		ReactorFuelType type;
		Item item;
		
		public ReactorFuelEntry(int value, ReactorFuelType type, Item item) {
			this.value = value;
			this.type = type;
			this.item = item;
		}
	}
	
	static class ReactorWasteEntry {
		
		int value;
		ReactorFuelType type;
		Item in;
		Item out;
		
		public ReactorWasteEntry(int value, ReactorFuelType type, Item in, Item out) {
			this.value = value;
			this.type = type;
			this.in = in;
			this.out = out;
		}
	}

	static List<ReactorFuelEntry> fuels = new ArrayList<ReactorFuelEntry>();
	static List<ReactorWasteEntry> wastes = new ArrayList<ReactorWasteEntry>();
	
	public static void registerFuelEntry(int nuggets, ReactorFuelType type, Item fuel) {
		
		fuels.add(new ReactorFuelEntry(nuggets, type, fuel));
	}
	
	public static void registerWasteEntry(int nuggets, ReactorFuelType type, Item in, Item out) {
		
		wastes.add(new ReactorWasteEntry(nuggets, type, in, out));
	}
	
	public static int getFuelContent(Item item, ReactorFuelType type) {
		
		for(ReactorFuelEntry ent : fuels) {
			if(ent.item == item && type.toString().equals(ent.type.toString()))
				return ent.value;
		}
			
		return 0;
	}
	
	public static ReactorFuelType getFuelType(Item item) {
		
		for(ReactorFuelEntry ent : fuels) {
			if(ent.item == item)
				return ent.type;
		}
			
		return ReactorFuelType.UNKNOWN;
	}
	
	public static Item getWaste(Item item, ReactorFuelType type) {
		
		for(ReactorWasteEntry ent : wastes) {
			if(ent.in == item && type.toString().equals(ent.type.toString()))
				return ent.out;
		}
			
		return null;
	}
	
	public static int getWasteAbsorbed(Item item, ReactorFuelType type) {
		
		for(ReactorWasteEntry ent : wastes) {
			if(ent.in == item && type.toString().equals(ent.type.toString()))
				return ent.value;
		}
			
		return 0;
	}

}