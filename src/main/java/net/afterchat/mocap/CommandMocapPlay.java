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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public class CommandMocapPlay extends CommandBase {
	Mocap parent;
	ArrayList<PlayThread> playThreads;

	public CommandMocapPlay(Mocap _parent) {
		parent = _parent;
		playThreads = new ArrayList<PlayThread>();
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender icommandsender) {
		if (!icommandsender.getCommandSenderName().equals("@")) {
			return false;
		}

		return true;
	}

	@Override
	public String getCommandName() {
		return "mocap-play";
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "mocap-play <replay> <skinname> <entityname>";
	}

	@Override
	public void processCommand(ICommandSender icommandsender, String[] args) {
		if (!icommandsender.getCommandSenderName().equals("@")) {
			return;
		}

		if (args.length < 3) {
			Mocap.instance.broadcastMsg(getCommandUsage(icommandsender));
			return;
		}

		if (args[0].equals("@p")) {
			EntityPlayer GuessPlayer = Mocap.getPlayerForName(icommandsender,
					args[0]);

			if (GuessPlayer != null) {
				args[0] = GuessPlayer.getCommandSenderName();
			}
		}

		File file = new File(DimensionManager.getCurrentSaveRootDirectory()
				+ "/" + "mocaps/" + args[0] + ".mocap");

		if (!file.exists()) {
			Mocap.instance.broadcastMsg("Can't find " + args[0]
					+ ".mocap replay file!");
			return;
		}

		RandomAccessFile in;
		double x = 0;
		double y = 0;
		double z = 0;

		try {
			in = new RandomAccessFile(file, "r");
			short magic = in.readShort();

			if (magic != -5119) {
				Mocap.instance.broadcastMsg(args[0]
						+ " isn't a .mocap file (or is an old version?)");
				in.close();
				return;
			}

			float yaw = in.readFloat();
			float pitch = in.readFloat();
			x = in.readDouble();
			y = in.readDouble();
			z = in.readDouble();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		World world = icommandsender.getEntityWorld();
		EntityMocap entity;
		entity = new EntityMocap(world);
		entity.setPosition(x, y, z);
		entity.setSkinSource(args[1]);
		entity.setCustomNameTag(args[2]);
		entity.setAlwaysRenderNameTag(true);
		world.spawnEntityInWorld(entity);

		/**
		 * Cleanup.
		 */
		for (Iterator<PlayThread> iterator = playThreads.iterator(); iterator
				.hasNext();) {
			PlayThread item = iterator.next();

			if (!item.t.isAlive()) {
				iterator.remove();
			}
		}

		playThreads.add(new PlayThread(entity, args[0]));

	}
}
