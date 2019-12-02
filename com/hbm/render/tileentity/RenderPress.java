package com.hbm.render.tileentity;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.hbm.main.ResourceManager;
import com.hbm.render.RenderHelper;
import com.hbm.tileentity.machine.TileEntityMachinePress;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.ForgeHooksClient;

public class RenderPress extends TileEntitySpecialRenderer<TileEntityMachinePress> {

	/*private static final float[] IDENTITY_MATRIX =
			new float[] {
				1.0f, 0.0f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f, 0.0f,
				0.0f, 0.0f, 1.0f, 0.0f,
				0.0f, 0.0f, 0.0f, 1.0f };
	
	private static final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);*/

	public RenderPress() {
		super();
	}

	@Override
	public void render(TileEntityMachinePress te, double x, double y, double z, float partialTicks, int destroyStage,
			float alpha) {

		/* GL11.glEnable(GL11.GL_STENCIL_TEST);
	        
	        
	        GL11.glStencilMask(0xFF);
	        GL11.glClearStencil(0);
	        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT );
	        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
	        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
	        GL11.glColorMask(false, false, false, false);
	        GL11.glDepthMask(false);
	        GL11.glDisable(GL11.GL_TEXTURE_2D);
	        GL11.glBegin(GL11.GL_QUADS);
	        GL11.glVertex3d(x, y, z);
	        GL11.glVertex3d(x, y, z+1);
	        GL11.glVertex3d(x, y+1, z+1);
	        GL11.glVertex3d(x, y+1, z);
	        GL11.glEnd();
	        GL11.glEnable(GL11.GL_TEXTURE_2D);
	        GL11.glDepthMask(true);
	        GL11.glColorMask(true, true, true, true);
	        GL11.glStencilMask(0x00);
	        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
	        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
		
		FloatBuffer oldMatrix = GLAllocation.createDirectFloatBuffer(16);
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, oldMatrix);
		float[] duck = RenderHelper.getScreenAreaFromQuad(new Vec3d(x, y, z), new Vec3d(x, y, z + 1),
                new Vec3d(x, y + 1, z + 1), new Vec3d(x, y + 1, z));
        if (duck != null) {
            
        	
        	GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glLoadIdentity();
            float rightFactor = Minecraft.getMinecraft().displayWidth/duck[2];
            float leftFactor = Minecraft.getMinecraft().displayWidth/duck[0];
            float topFactor = Minecraft.getMinecraft().displayHeight/duck[3];
            float bottomFactor = Minecraft.getMinecraft().displayHeight/duck[1];
            
            float testl = (float) (((Math.floor(Minecraft.getMinecraft().displayWidth/leftFactor)*leftFactor)/Minecraft.getMinecraft().displayWidth)*leftFactor);
            
            this.setupProjMatrix(rightFactor, leftFactor, topFactor, bottomFactor, 0.05F, (Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16) * MathHelper.SQRT_2);
            //this.setupProjMatrix(testr, testl, testt, testb, 0.05F, (Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16) * MathHelper.SQRT_2);
            
            IntBuffer vp = GLAllocation.createDirectIntBuffer(16);
			
            GL11.glViewport((int) (Minecraft.getMinecraft().displayWidth/leftFactor), (int) (Minecraft.getMinecraft().displayHeight/bottomFactor), (int) (Minecraft.getMinecraft().displayWidth/rightFactor-Minecraft.getMinecraft().displayWidth/leftFactor), (int) (Minecraft.getMinecraft().displayHeight/topFactor-Minecraft.getMinecraft().displayHeight/bottomFactor));
            GL11.glGetInteger(GL11.GL_VIEWPORT, vp);

            
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }*/
		GL11.glPushMatrix();

		GL11.glTranslated(x + 0.5D, y, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glRotatef(180, 0F, 1F, 0F);
		this.bindTexture(ResourceManager.press_body_tex);
		ResourceManager.press_body.renderAll();

		GL11.glPopMatrix();
		renderTileEntityAt2(te, x, y, z, partialTicks);
	//	GL11.glMatrixMode(GL11.GL_PROJECTION);
	//	GL11.glLoadIdentity();
		//GL11.glLoadMatrix(oldMatrix);
		//GL11.glMatrixMode(GL11.GL_MODELVIEW);
	//	GL11.glViewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
	//	GL11.glDisable(GL11.GL_STENCIL_TEST);
	}

	public void renderTileEntityAt2(TileEntity tileEntity, double x, double y, double z, float f) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y + 1 - 0.125D, z + 0.5D);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glRotatef(180, 0F, 1F, 0F);
		GL11.glScalef(0.95F, 1, 0.95F);

		TileEntityMachinePress press = (TileEntityMachinePress) tileEntity;
		float f1 = press.progress * (1 - 0.125F) / TileEntityMachinePress.maxProgress;
		GL11.glTranslated(0, -f1, 0);

		this.bindTexture(ResourceManager.press_head_tex);

		ResourceManager.press_head.renderAll();

		GL11.glPopMatrix();

		renderTileEntityAt3(tileEntity, x, y, z, f);
	}

	public void renderTileEntityAt3(TileEntity tileEntity, double x, double y, double z, float f) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y + 1, z - 0.5);
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glRotatef(180, 0F, 1F, 0F);
		GL11.glRotatef(-90, 1F, 0F, 0F);

		TileEntityMachinePress press = (TileEntityMachinePress) tileEntity;
		ItemStack stack = new ItemStack(Item.getItemById(press.item), 1, press.meta);

		if (!(stack.getItem() instanceof ItemBlock) && !stack.isEmpty()) {
			IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(stack,
					tileEntity.getWorld(), null);
			model = ForgeHooksClient.handleCameraTransforms(model, TransformType.FIXED, false);
			Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			GL11.glTranslatef(0.0F, 1.0F, 0.0F);
			GL11.glRotatef(180, 0F, 1F, 0F);
			GL11.glScalef(0.5F, 0.5F, 0.5F);
			Minecraft.getMinecraft().getRenderItem().renderItem(stack, model);
		}

		GL11.glPopMatrix();
	}
	
	/**
	 * 
	 * @param r - 
	 * @param l
	 * @param t
	 * @param b
	 * @param zNear
	 * @param zFar
	 */
	/*public void setupProjMatrix(float r, float l, float t, float b, float zNear, float zFar){
		
		float r2, l2, t2, b2, r3, l3, t3, b3;
		float sine, cotangent, deltaZ;
		float radians = Minecraft.getMinecraft().gameSettings.fovSetting*fov / 2 * (float)Math.PI / 180;

		deltaZ = zFar - zNear;
		sine = (float) Math.sin(radians);

		if ((deltaZ == 0) || (sine == 0) || (1 == 0)) {
			return;
		}

		cotangent = (float) Math.cos(radians) / sine;

		float aspect = (float)Minecraft.getMinecraft().displayWidth/(float)Minecraft.getMinecraft().displayHeight;
		float duck = cotangent/aspect;
		//System.out.println(cotangent/aspect + " " + cotangent + " " + cotangent/(cotangent/aspect) + " " + Minecraft.getMinecraft().displayWidth/(float)Minecraft.getMinecraft().displayHeight);
		
		
		
		
		r2 = (float) (((2.0F*zNear)/duck)/2);
		l2 =  -r2;
		t2 = (float) (((2.0F*zNear)/cotangent)/2);
		b2 = -t2;
		
		r3 = (r2+r2)/r-r2;
		l3 = -((l2+l2)/l-l2);
		t3 = (t2+t2)/t-t2;
		b3 = -((b2+b2)/b-b2);
		this.setupProjMatrixDirect(r3, l3, t3, b3, zNear, zFar);
	}
	
	public void setupProjMatrixDirect(float r, float l, float t, float b, float zNear, float zFar){

		
		
		
		__gluMakeIdentityf(matrix);
//2*zNear / (r-l))
		//2*zNear / (t-b)
		matrix.put(0 * 4 + 0, 2*zNear / (r-l));
		matrix.put(1 * 4 + 1, 2*zNear / (t-b));
		matrix.put(2*4 + 0, (r+l)/(r-l));
		matrix.put(2*4 + 1, (t+b)/(t-b));
		matrix.put(2 * 4 + 2, - (zFar + zNear) / (zFar-zNear));
		matrix.put(2 * 4 + 3, -1);
		matrix.put(3 * 4 + 2, -2 * zNear * zFar / (zFar-zNear));
		matrix.put(3 * 4 + 3, 0);

		//glMultMatrix(matrix);
		GL11.glLoadMatrix(matrix);
	}

	private static void __gluMakeIdentityf(FloatBuffer m) {
		int oldPos = m.position();
		m.put(IDENTITY_MATRIX);
		m.position(oldPos);
	}
	
	public float normalize(float value, float min, float max, float mult) {
		float normalized = (value - min) / (max - min);
		return normalized*mult;
	}
	public float normalize(float value, float min, float max){
		return this.normalize(value, min, max, 1.0f);
	}*/
	
}
