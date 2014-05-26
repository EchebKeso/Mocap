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
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraftforge.common.DimensionManager;

class RecordThread implements Runnable {
	public Thread t;
	EntityPlayer player;
	public Boolean capture = false;
	RandomAccessFile in;
	Boolean lastTickSwipe = false;
	int itemsEquipped[] = new int[5];
	List<MocapAction> eventList;

	RecordThread(EntityPlayer _player, String capname) {
		// Create a new, second thread
		try {
			File file = new File(DimensionManager.getCurrentSaveRootDirectory()
					+ "/" + "mocaps");

			if (!file.exists()) {
				file.mkdirs();
			}

			in = new RandomAccessFile(file.getAbsolutePath() + "/" + capname
					+ ".mocap", "rw");
			in.setLength(0);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		player = _player;
		capture = true;
		eventList = Mocap.instance.getActionListForPlayer(player);
		t = new Thread(this, "Mocap Record Thread");
		t.start();
	}

	// This is the entry point for the second thread.
	public void run() {
		try {
			in.writeShort(0xEC01);

			while (capture) {
				trackAndWriteMovement();
				trackSwing();
				trackHeldItem();
				trackArmor();
				writeActions();
				Thread.sleep(100);

				if (player.isDead) {
					capture = false;
					Mocap.instance.recordThreads.remove(player);
					Mocap.instance.broadcastMsg("Stopped recording "
							+ player.getDisplayName() + ". RIP.");
				}
			}

			in.close();
		} catch (InterruptedException e) {
			System.out.println("Child interrupted.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Exiting child thread.");
	}

	private void trackAndWriteMovement() throws IOException {
		in.writeFloat(player.rotationYaw);
		in.writeFloat(player.rotationPitch);
		in.writeDouble(player.posX);
		in.writeDouble(player.posY);
		in.writeDouble(player.posZ);
		in.writeDouble(player.motionX);
		in.writeDouble(player.motionY);
		in.writeDouble(player.motionZ);
		in.writeFloat(player.fallDistance);
		in.writeBoolean(player.isAirBorne);
		in.writeBoolean(player.isSneaking());
		in.writeBoolean(player.isSprinting());
		in.writeBoolean(player.onGround);
		in.writeBoolean((player.getDataWatcher().getWatchableObjectByte(0) & 1 << 4) != 0);
	}

	private void trackArmor() {
		/*
		 * Track armor equipped.
		 */
		// TODO: Sequential equipping of same item id but different type =
		// problem.
		for (int ci = 1; ci < 5; ci++) {
			if (player.inventory.armorInventory[ci - 1] != null) {
				if (Item.getIdFromItem(player.inventory.armorInventory[ci - 1]
						.getItem()) != itemsEquipped[ci]) {
					itemsEquipped[ci] = Item
							.getIdFromItem(player.inventory.armorInventory[ci - 1]
									.getItem());
					MocapAction ma = new MocapAction(MocapActionTypes.EQUIP);
					ma.armorSlot = ci;
					ma.armorId = itemsEquipped[ci];
					ma.armorDmg = player.inventory.armorInventory[ci - 1]
							.getItemDamage();
					player.inventory.armorInventory[ci - 1]
							.writeToNBT(ma.itemData);
					eventList.add(ma);
				}
			} else {
				// TODO
				if (itemsEquipped[ci] != -1) {
					itemsEquipped[ci] = -1;
					MocapAction ma = new MocapAction(MocapActionTypes.EQUIP);
					ma.armorSlot = ci;
					ma.armorId = itemsEquipped[ci];
					ma.armorDmg = 0;
					eventList.add(ma);
				}
			}
		}
	}

	private void trackHeldItem() {
		if (player.getHeldItem() != null) {
			if (Item.getIdFromItem(player.getHeldItem().getItem()) != itemsEquipped[0]) {
				itemsEquipped[0] = Item.getIdFromItem(player.getHeldItem()
						.getItem());
				MocapAction ma = new MocapAction(MocapActionTypes.EQUIP);
				ma.armorSlot = 0;
				ma.armorId = itemsEquipped[0];
				ma.armorDmg = player.getHeldItem().getItemDamage();
				player.getHeldItem().writeToNBT(ma.itemData);
				eventList.add(ma);
			}
		} else {
			if (itemsEquipped[0] != -1) {
				itemsEquipped[0] = -1;
				MocapAction ma = new MocapAction(MocapActionTypes.EQUIP);
				ma.armorSlot = 0;
				ma.armorId = itemsEquipped[0];
				ma.armorDmg = 0;
				eventList.add(ma);
			}
		}
	}

	private void trackSwing() {
		/*
		 * Track "Swings" weapon / fist.
		 */
		if (player.isSwingInProgress) {
			if (!lastTickSwipe) {
				lastTickSwipe = true;
				eventList.add(new MocapAction(MocapActionTypes.SWIPE));
			}
		} else {
			lastTickSwipe = false;
		}
	}

	private void writeActions() throws IOException {
		// Any actions?
		if (eventList.size() > 0) {
			in.writeBoolean(true);
			MocapAction ma = eventList.get(0);
			in.writeByte(ma.type);

			switch (ma.type) {
			case MocapActionTypes.CHAT: {
				in.writeUTF(ma.message);
				break;
			}

			case MocapActionTypes.SWIPE: {
				break;
			}

			case MocapActionTypes.DROP: {
				CompressedStreamTools.write(ma.itemData, in);
				break;
			}

			case MocapActionTypes.EQUIP: {
				in.writeInt(ma.armorSlot);
				in.writeInt(ma.armorId);
				in.writeInt(ma.armorDmg);

				if (ma.armorId != -1) {
					CompressedStreamTools.write(ma.itemData, in);
				}

				break;
			}

			case MocapActionTypes.SHOOTARROW: {
				in.writeInt(ma.arrowCharge);
				break;
			}

			case MocapActionTypes.PLACEBLOCK: {
				in.writeInt(ma.xCoord);
				in.writeInt(ma.yCoord);
				in.writeInt(ma.zCoord);
				CompressedStreamTools.write(ma.itemData, in);
				break;
			}

			case MocapActionTypes.LOGOUT: {
				Mocap.instance.recordThreads.remove(player);
				Mocap.instance.broadcastMsg("Stopped recording "
						+ player.getDisplayName() + ". Bye!");
				capture = false;
				break;
			}
			}

			eventList.remove(0);
		} else {
			in.writeBoolean(false);
		}
	}
}
