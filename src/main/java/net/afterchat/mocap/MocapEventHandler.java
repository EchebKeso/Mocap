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

import java.io.IOException;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.relauncher.Side;

public class MocapEventHandler {
	@SubscribeEvent
	public void onPlayerLoggedOutEvent(PlayerLoggedOutEvent event) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();

		if (side == Side.SERVER) {
			List<MocapAction> aList = Mocap.instance
					.getActionListForPlayer(event.player);

			if (aList != null) {
				MocapAction ma = new MocapAction(MocapActionTypes.LOGOUT);
				aList.add(ma);
			}
		}
	}

	@SubscribeEvent
	public void onLivingPlaceBlockEvent(LivingPlaceBlockEvent event) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();

		if (side == Side.SERVER) {
			if (event.entityLiving instanceof EntityPlayerMP) {
				EntityPlayerMP thePlayer = (EntityPlayerMP) event.entityLiving;
				List<MocapAction> aList = Mocap.instance
						.getActionListForPlayer(thePlayer);

				if (aList != null) {
					MocapAction ma = new MocapAction(
							MocapActionTypes.PLACEBLOCK);
					event.theItem.writeToNBT(ma.itemData);
					ma.xCoord = event.xCoord;
					ma.yCoord = event.yCoord;
					ma.zCoord = event.zCoord;
					aList.add(ma);
				}
			}
		}
	}

	@SubscribeEvent
	public void onArrowLooseEvent(ArrowLooseEvent ev) throws IOException {
		Side side = FMLCommonHandler.instance().getEffectiveSide();

		if (side == Side.SERVER) {
			List<MocapAction> aList = Mocap.instance
					.getActionListForPlayer(ev.entityPlayer);

			if (aList != null) {
				MocapAction ma = new MocapAction(MocapActionTypes.SHOOTARROW);
				ma.arrowCharge = ev.charge;
				aList.add(ma);
			}
		}
	}

	@SubscribeEvent
	public void onItemTossEvent(ItemTossEvent ev) throws IOException {
		Side side = FMLCommonHandler.instance().getEffectiveSide();

		if (side == Side.SERVER) {
			List<MocapAction> aList = Mocap.instance
					.getActionListForPlayer(ev.player);

			if (aList != null) {
				MocapAction ma = new MocapAction(MocapActionTypes.DROP);
				ev.entityItem.getEntityItem().writeToNBT(ma.itemData);
				aList.add(ma);
			}
		}
	}

	@SubscribeEvent
	public void onServerChatEvent(ServerChatEvent ev) {
		Side side = FMLCommonHandler.instance().getEffectiveSide();

		if (side == Side.SERVER) {
			List<MocapAction> aList = Mocap.instance
					.getActionListForPlayer(ev.player);

			if (aList != null) {
				MocapAction ma = new MocapAction(MocapActionTypes.CHAT);
				ma.message = ev.message;
				aList.add(ma);
			}
		}
	}
}
