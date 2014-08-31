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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler; // used in 1.6.2
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

/**
 * The main Mocap class.
 * 
 * @author Echeb Keso
 * 
 */
@Mod(modid = "Mocap", name = "Motion Capture", version = "1.1")
public class Mocap {
	private static final Logger logger = LogManager.getLogger();

	public Map<EntityPlayer, MocapRecorder> recordThreads = Collections
			.synchronizedMap(new HashMap<EntityPlayer, MocapRecorder>());

	public List<MocapAction> getActionListForPlayer(EntityPlayer ep) {
		MocapRecorder aRecorder = recordThreads.get(ep);

		if (aRecorder == null) {
			return null;
		}

		return aRecorder.eventsList;
	}

	@Instance(value = "Mocap")
	public static Mocap instance;

	@SidedProxy(clientSide = "net.afterchat.mocap.ClientProxy", serverSide = "net.afterchat.mocap.CommonProxy")
	public static CommonProxy proxy;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
	}

	public void LogMessage(String msg) {
		logger.info(msg);
	}

	public void broadcastMsg(String msg) {
		ArrayList<EntityPlayerMP> temp = (ArrayList<EntityPlayerMP>) FMLCommonHandler
				.instance().getSidedDelegate().getServer()
				.getConfigurationManager().playerEntityList;

		for (EntityPlayerMP player : temp) {
			if (FMLCommonHandler.instance().getSidedDelegate().getServer()
					.getConfigurationManager()
					.func_152596_g(player.getGameProfile())) {
				ChatComponentText cmp = new ChatComponentText("[MOCAP]: " + msg);

				player.addChatMessage(cmp);
			}
		}
	}

	@EventHandler
	public void serverStart(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandMocapRec(this));
		event.registerServerCommand(new CommandMocapPlay(this));
		event.registerServerCommand(new CommandMocapRecPlayer(this));
	}

	@EventHandler
	public void load(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(new MocapEventHandler());
		int id = 0;
		EntityRegistry.registerModEntity(EntityMocap.class, "EntityMocap", id,
				this, 80, 1, true);
		id++;

		LanguageRegistry.instance().addStringLocalization(
				"entity.EntityMocap.name", "en_US", "Mocap Actor");

		proxy.registerRenderers();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	public static EntityPlayerMP getPlayerForName(ICommandSender sender,
			String name) {
		EntityPlayerMP var2 = PlayerSelector.matchOnePlayer(sender, name);

		if (var2 != null) {
			return var2;
		} else {
			return getPlayerForName(name);
		}
	}

	public static EntityPlayerMP getPlayerForName(String name) {
		/* Try an exact match first */
		{
			EntityPlayerMP tempPlayer = FMLCommonHandler.instance()
					.getMinecraftServerInstance().getConfigurationManager()
					.func_152612_a(name); /* 1.7.10 func_152612_a is getPlayerByName */

			if (tempPlayer != null) {
				return tempPlayer;
			}
		}
		
		// now try getting others
		List<EntityPlayerMP> possibles = new LinkedList<EntityPlayerMP>();
		ArrayList<EntityPlayerMP> temp = (ArrayList<EntityPlayerMP>) FMLCommonHandler
				.instance().getSidedDelegate().getServer()
				.getConfigurationManager().playerEntityList;

		for (EntityPlayerMP player : temp) {
			if (player.getCommandSenderName().equalsIgnoreCase(name)) {
				return player;
			}

			if (player.getCommandSenderName().toLowerCase()
					.contains(name.toLowerCase())) {
				possibles.add(player);
			}
		}

		if (possibles.size() == 1) {
			return possibles.get(0);
		}

		return null;
	}
}
