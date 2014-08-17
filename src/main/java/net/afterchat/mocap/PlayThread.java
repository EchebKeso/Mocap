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

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraftforge.common.DimensionManager;

class PlayThread implements Runnable {
	Thread t;
	EntityMocap replayEntity;	
	DataInputStream in;

	public PlayThread(EntityMocap _player, String recfile) {
		try {
			File file = new File(DimensionManager.getCurrentSaveRootDirectory()
					+ "/" + "mocaps");

			in = new DataInputStream(new FileInputStream(file.getAbsolutePath() + "/" + recfile + ".mocap"));			

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		replayEntity = _player;
		t = new Thread(this, "Mocap Playback Thread");
		t.start();
	}

	// This is the entry point for the second thread.
	public void run() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			short magic = in.readShort();

			if (magic != -5119) {
				throw new Exception("Not a mocap");
			}

			while (true) {
				float yaw = in.readFloat();
				float pitch = in.readFloat();
				double x = in.readDouble();
				double y = in.readDouble();
				double z = in.readDouble();
				double mx = in.readDouble();
				double my = in.readDouble();
				double mz = in.readDouble();
				float fd = in.readFloat();
				Boolean iab = in.readBoolean();
				Boolean isn = in.readBoolean();
				Boolean isp = in.readBoolean();
				Boolean iog = in.readBoolean();
				Boolean ie = in.readBoolean();
				/* Set Position Fields in Entity */
				replayEntity.isAirBorne = iab;
				replayEntity.motionX = mx;
				replayEntity.motionY = my;
				replayEntity.motionZ = mz;
				replayEntity.fallDistance = fd;
				replayEntity.setSneaking(isn);
				replayEntity.setSprinting(isp);
				replayEntity.onGround = iog;
				replayEntity.setPositionAndRotation(x, y, z, yaw, pitch);
				replayEntity.setEating(ie);

				Boolean hasAction = in.readBoolean();

				/* Post actions to queue for Entity to run on main Tick thread */

				if (hasAction) {
					byte type = in.readByte();

					switch (type) {
					case MocapActionTypes.CHAT: {
						String msg = in.readUTF();
						MocapAction ma = new MocapAction(MocapActionTypes.CHAT);
						ma.message = msg;
						replayEntity.eventsList.add(ma);
						break;
					}

					case MocapActionTypes.EQUIP: {
						MocapAction ma = new MocapAction(MocapActionTypes.EQUIP);
						int aSlot = in.readInt();
						int aId = in.readInt();
						int aDmg = in.readInt();

						/* If not a "Clear Slot" event, load item data. */
						if (aId != -1) {
							ma.itemData = CompressedStreamTools.read(in);
						}

						ma.armorSlot = aSlot;
						ma.armorId = aId;
						ma.armorDmg = aDmg;
						replayEntity.eventsList.add(ma);
						break;
					}

					case MocapActionTypes.SWIPE: {
						MocapAction ma = new MocapAction(MocapActionTypes.SWIPE);
						replayEntity.eventsList.add(ma);
						break;
					}

					case MocapActionTypes.DROP: {
						MocapAction ma = new MocapAction(MocapActionTypes.DROP);
						ma.itemData = CompressedStreamTools.read(in);

						replayEntity.eventsList.add(ma);
						break;
					}

					case MocapActionTypes.SHOOTARROW: {
						int aCharge = in.readInt();
						MocapAction ma = new MocapAction(
								MocapActionTypes.SHOOTARROW);
						ma.arrowCharge = aCharge;
						replayEntity.eventsList.add(ma);
						break;
					}

					case MocapActionTypes.PLACEBLOCK: {
						MocapAction ma = new MocapAction(
								MocapActionTypes.PLACEBLOCK);
						ma.xCoord = in.readInt();
						ma.yCoord = in.readInt();
						ma.zCoord = in.readInt();
						ma.itemData = CompressedStreamTools.read(in);
						replayEntity.eventsList.add(ma);
						break;
					}
					}
				}

				Thread.sleep(100);
			}
		}
		catch (EOFException e) {
			System.out.println("Replay thread completed.");
			// "Normal" exception (I kinda hate these ;)
		} catch (Exception e) {
			System.out.println("Replay thread interrupted.");
			Mocap.instance
			.broadcastMsg("Error loading mocap file, either not a mocap or recorded by an older version.");
			e.printStackTrace();
		}

		replayEntity.setDead();

		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
