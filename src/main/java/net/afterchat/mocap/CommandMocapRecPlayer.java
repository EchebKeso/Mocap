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

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class CommandMocapRecPlayer extends CommandBase {
	Mocap parent;

	public CommandMocapRecPlayer(Mocap _parent) {
		parent = _parent;
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
		return "mocap-rec-player";
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "Usage: /mocap-rec-player <playername> <savefile>, eg: /mocap-rec Ridgedog running";
	}

	@Override
	public void processCommand(ICommandSender icommandsender, String[] args) {
		EntityPlayer player;

		if (!icommandsender.getCommandSenderName().equals("@")) {
			return;
		}

		if (args.length < 2) {
			icommandsender.addChatMessage(new ChatComponentText(
					getCommandUsage(icommandsender)));
			return;
		}

		player = Mocap.getPlayerForName(icommandsender, args[0]);

		if (player == null) {
			Mocap.instance.broadcastMsg("Couldn't find player " + args[0]);
			return;
		}

		if (args[1].equals("@p")) {
			args[1] = player.getCommandSenderName();
		}

		/* Are we being recorded? */
		MocapRecorder aRecorder = Mocap.instance.recordThreads.get(player);

		if (aRecorder != null) {
			aRecorder.recordThread.capture = false;
			Mocap.instance.broadcastMsg("Stopped recording "
					+ player.getDisplayName() + " to file "
					+ aRecorder.fileName + ".mocap");
			Mocap.instance.recordThreads.remove(player);
			return;
		}

		/* Is this filename being recorded to? */

		synchronized (Mocap.instance.recordThreads) {
			for (MocapRecorder ar : Mocap.instance.recordThreads.values()) {
				if (ar.fileName.equals(args[1].toLowerCase())) {
					Mocap.instance.broadcastMsg("'" + ar.fileName
							+ ".mocap' is already being recorded to?");
					return;
				}
			}
		}

		if (aRecorder == null) {
			Mocap.instance.broadcastMsg("Started recording "
					+ player.getDisplayName() + " to file " + args[1]
					+ ".mocap");
			MocapRecorder mcr = new MocapRecorder();
			mcr.fileName = args[1].toLowerCase();
			Mocap.instance.recordThreads.put(player, mcr);
			mcr.recordThread = new RecordThread(player, args[1]);
			return;
		}
	}
}
