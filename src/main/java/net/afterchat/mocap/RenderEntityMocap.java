/**
 * The Mocap mod is a Minecraft mod aimed at film-makers who wish to
 * record the motion of their character and replay it back via a scripted
 * entity. It allows you to create a whole series of composite characters
 * for use in your own videos without having to splash out on extra accounts
 * or enlist help.
 * 
 * Copyright (C) 2013-2014 Echeb Keso
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.*
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.afterchat.mocap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderEntityMocap extends RenderBiped {

	/*
	 * Should we render the held item in a 'blocking' fashion?
	 */
	Boolean renderEat = false;

	public static final ResourceLocation locationStevePng = new ResourceLocation(
			"textures/entity/steve.png");

	public RenderEntityMocap(ModelBiped par1ModelBase, float par2) {
		super(par1ModelBase, par2);
	}

	/**
	 * Called from doRenderLiving in RenderBiped, overridden to prevent default
	 * behaviour.
	 */
	@Override
	protected void func_82420_a(EntityLiving par1EntityLiving,
			ItemStack par2ItemStack) {
		return;
	}

	/**
	 * Called from RenderBiper in renderEquippedItems, used to rotate item when
	 * eating / blocking.
	 */
	@Override
	protected void func_82422_c() {
		if (renderEat) {
			GL11.glTranslatef(0.05F, 0.0F, -0.1F);
			GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
		}
	}

	@Override
	public void doRender(EntityLiving par1EntityLiving, double par2,
			double par4, double par6, float par8, float par9) {
		EntityMocap par1EntityMocap = (EntityMocap) par1EntityLiving;
		ItemStack itemstack = par1EntityLiving.getHeldItem();
		this.field_82423_g.heldItemRight = this.field_82425_h.heldItemRight = this.modelBipedMain.heldItemRight = itemstack != null ? 1
				: 0;
		renderEat = false;

		if (itemstack != null && par1EntityMocap.isEating()) {
			EnumAction enumaction = itemstack.getItemUseAction();

			if (enumaction == EnumAction.eat) {
				this.field_82423_g.heldItemRight = this.field_82425_h.heldItemRight = this.modelBipedMain.heldItemRight = 6;
				renderEat = true;
			}

			if (enumaction == EnumAction.block) {
				renderEat = true;
				this.field_82423_g.heldItemRight = this.field_82425_h.heldItemRight = this.modelBipedMain.heldItemRight = 3;
			}

			else if (enumaction == EnumAction.bow) {
				this.field_82423_g.aimedBow = this.field_82425_h.aimedBow = this.modelBipedMain.aimedBow = true;
			}

		}

		this.field_82423_g.isSneak = this.field_82425_h.isSneak = this.modelBipedMain.isSneak = par1EntityLiving
				.isSneaking();
		super.doRender(par1EntityLiving, par2, par4, par6, par8, par9);
	}

	private static ThreadDownloadImageData getDownloadImage(
			ResourceLocation par0ResourceLocation, String par1Str,
			ResourceLocation par2ResourceLocation, IImageBuffer par3IImageBuffer) {
		TextureManager texturemanager = Minecraft.getMinecraft()
				.getTextureManager();
		Object object = texturemanager.getTexture(par0ResourceLocation);

		if (object == null) {
			object = new ThreadDownloadImageData(par1Str, par2ResourceLocation,
					par3IImageBuffer);
			texturemanager.loadTexture(par0ResourceLocation,
					(ITextureObject) object);
		}

		return (ThreadDownloadImageData) object;
	}

	public static ThreadDownloadImageData getDownloadImageSkin(
			ResourceLocation par0ResourceLocation, String par1Str) {
		String theUrl = "";

		if (par1Str.matches("\\d{10,}")) {
			theUrl = getSkindexUrl(par1Str);
		} else {
			theUrl = getSkinUrl(par1Str);
		}

		return getDownloadImage(par0ResourceLocation, theUrl, locationStevePng,
				new ImageBufferDownload());
	}

	public static String getSkinUrl(String par0Str) {
		return String.format(
				"http://skins.minecraft.net/MinecraftSkins/%s.png",
				new Object[] { StringUtils.stripControlCodes(par0Str) });
	}

	public static String getSkindexUrl(String par0Str) {
		return String.format(
				"http://www.minecraftskins.com/newuploaded_skins/skin_%s.png",
				new Object[] { StringUtils.stripControlCodes(par0Str) });
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		EntityMocap fo = (EntityMocap) entity;
		String skinName = fo.getSkinSource();
		ResourceLocation resourcelocation = AbstractClientPlayer.locationStevePng;

		if (skinName != null && skinName.length() > 0) {
			resourcelocation = AbstractClientPlayer.getLocationSkin(skinName);
			getDownloadImageSkin(resourcelocation, skinName);
		}

		return resourcelocation;
	}
}
