package com.github.canisartorus.prospectorjournal;

import com.github.canisartorus.prospectorjournal.lib.GeoTag;
import com.github.canisartorus.prospectorjournal.lib.RockMatter;
import com.github.canisartorus.prospectorjournal.lib.Utils;
import com.github.canisartorus.prospectorjournal.network.PacketOreSurvey;

import gregapi.block.metatype.BlockStones;
import gregapi.item.multiitem.MultiItem;
import gregapi.oredict.OreDictMaterial;
import gregapi.tileentity.notick.TileEntityBase03MultiTileEntities;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class JournalBehaviour extends gregapi.item.multiitem.behaviors.IBehavior.AbstractBehaviorDefault {
	public static JournalBehaviour INSTANCE = new JournalBehaviour();

	static final short[] multiFlowers = { 9130, 9211, 9133, 9194, 9217, 9193, 9128, 9195, 9196, 9197 };

	@Override
	public boolean onItemUse(MultiItem aItem, ItemStack aStack, EntityPlayer aPlayer, World aWorld, int aX, int aY,
			int aZ, byte aSide, float hitX, float hitY, float hitZ) {
		return false;
	}

	@Override
	public ItemStack onItemRightClick(MultiItem aItem, ItemStack aStack, World aWorld, EntityPlayer aPlayer) {
		return aStack;
	}

	private static boolean lookForSampleServer(World aWorld, int x, int y, int z, EntityPlayer aPlayer) {
		// stuff that needs server-side data
		final net.minecraft.tileentity.TileEntity i = aWorld.getTileEntity(x, y, z);
		if (!(i instanceof TileEntityBase03MultiTileEntities)) {
			return false;
		}

		// FIXME: why the heck this requires AE2?..
		if (!((TileEntityBase03MultiTileEntities) i).getTileEntityName()
				.equalsIgnoreCase("gt.multitileentity.rock")) {
			return false;
		}

		// serverside data only!!!
		final ItemStack sample = ((gregtech.tileentity.placeables.MultiTileEntityRock) i).mRock; // XXX GT
		if (sample == null) {
			// is default rock.
			if (ConfigHandler.trackRock) {
				TakeSampleServer(aWorld, x, y, z,
						(short) ((TileEntityBase03MultiTileEntities) i).getDrops(0, false).get(0)
								.getItemDamage(),
						Utils.ROCK, aPlayer);
			}
		} else if (gregapi.util.OM.is(gregapi.data.OD.itemFlint, sample)) {
			// ignore
		} else if (!ConfigHandler.trackRock
				&& gregapi.util.OM.materialcontains(sample, gregapi.data.TD.Properties.STONE)) {
			// ignore
		} else if (gregapi.data.OP.oreRaw.contains(sample)) {
			TakeSampleServer(aWorld, x, y, z, (short) sample.getItemDamage(), Utils.FLOWER, aPlayer);
		} else {
			TakeSampleServer(aWorld, x, y, z, (short) sample.getItemDamage(), Utils.ROCK, aPlayer);
		}
		return true;
	}

	private static boolean lookForSampleClient(World aWorld, int x, int y, int z, EntityPlayer aPlayer) {
		// works client-side, since it's based only on block meta-id
		net.minecraft.block.Block b = aWorld.getBlock(x, y, z);

		if (b instanceof gregapi.block.prefixblock.PrefixBlock) {
			final ItemStack sample = ((gregapi.block.prefixblock.PrefixBlock) b).getItemStackFromBlock(aWorld, x, y,
					z, gregapi.data.CS.SIDE_INVALID);
			final String tName = b.getUnlocalizedName();
			if (tName.endsWith(".bedrock")) {
				TakeSample(aWorld, x, y, z, (short) sample.getItemDamage(), Utils.BEDROCK, aPlayer);
			} else if (tName.startsWith("gt.meta.ore.normal.")) {
				TakeSample(aWorld, x, y, z, (short) sample.getItemDamage(), Utils.ORE_VEIN, aPlayer);
			}
		} else if (b instanceof gregapi.block.misc.BlockBaseFlower) {
			short type = 0;
			final int metadata = aWorld.getBlockMetadata(x, y, z);
			if (b.getUnlocalizedName().equalsIgnoreCase("gt.block.flower.a")) {
				// TODO: map or ...?
				switch (metadata) {
					case 0: // Gold
						type = 790;
						break;
					case 1: // Galena
						type = 9117;
						break;
					case 2: // Chalcopyrite
						type = 9111;
						break;
					case 3: // Sphalerite & Smithsonite
						type = 0; // either 9130 or 9211
						break;
					case 4: // Pentlandite
						type = 9145;
						break;
					case 5: // Uraninite
						type = 9134;
						break;
					case 6: // Cooperite
						type = 9116;
						break;
					case 8: // any Hexorium
					case 7: // generic Orechid
						break;
					default:
						if (ConfigHandler.debug && aPlayer.isClientWorld()) {
							aPlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
									"[ProspectorJournal] Found unregistered ore with block metadata " + metadata));
						}
						break;
				}
			} else if (b.getUnlocalizedName().equalsIgnoreCase("gt.block.flower.b")) {
				// TODO: map or ...?
				switch (metadata) {
					case 0: // Arsenopyrite
						type = 9216;
						break;
					case 1: // Stibnite
						type = 9131;
						break;
					case 2: // Gold
						type = 790;
						break;
					case 3: // Copper
						type = 290;
						break;
					case 4: // Redstone
						type = 8333;
						break;
					case 5: // Pitchblende
						type = 9155;
						break;
					case 6: // Diamonds
						type = 8300;
						break;
					case 7: // any W
						type = 0; // any of 9133, 9194, 9217, 9193, 9128, 9195, 9196, 9197
						break;
					default:
						if (ConfigHandler.debug && aPlayer.isClientWorld()) {
							aPlayer.addChatMessage(new net.minecraft.util.ChatComponentText(
									"[ProspectorJournal] Found unregistered ore with block metadata " + metadata));
						}
						break;
				}
			}
			TakeSample(aWorld, x, y, z, type, Utils.FLOWER, aPlayer);
		} else if (ConfigHandler.trackRock && b instanceof BlockStones
				&& b.getDamageValue(aWorld, x, y, z) == BlockStones.STONE) {
			TakeSample(aWorld, x, y, z, ((BlockStones) b).mMaterial.mID, Utils.ORE_VEIN, aPlayer);
		}
		return true;
	}

	/**
	 * Determines if an ore sample can be generated from this location, then calls
	 * TakeSample to do so.
	 * 
	 * @param aWorld
	 * @param x
	 * @param y
	 * @param z
	 * @param aPlayer
	 * @return
	 */
	public static boolean lookForSample(World aWorld, int x, int y, int z, EntityPlayer aPlayer) {
		if (aWorld.isRemote) {
			return lookForSampleClient(aWorld, x, y, z, aPlayer);
		}

		return lookForSampleServer(aWorld, x, y, z, aPlayer);
	}

	// @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.SERVER)
	static void TakeSampleServer(final World aWorld, int x, int y, int z, short meta, byte sourceType,
			final EntityPlayer aPlayer) {
		if (sourceType == Utils.ROCK && (meta == 8649 || meta == 8757)) {
			// ignore meteors
		} else {
			Utils.NW_PJ.sendToPlayer(new PacketOreSurvey(x, y, z, meta, sourceType), (EntityPlayerMP) aPlayer);
		}
	}

	/**
	 * Generates an ore sample knowledge for this location.
	 * 
	 * @param aWorld
	 * @param x
	 * @param y
	 * @param z
	 * @param meta
	 * @param sourceType
	 * @param aPlayer
	 */
	// @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
	public static void TakeSample(final World aWorld, int x, int y, int z, short meta, byte sourceType,
			final EntityPlayer aPlayer) {
		final int dim = aWorld.provider.dimensionId;
		if (ConfigHandler.debug) {
			System.out.println(ProspectorJournal.MOD_NAME + "[Info] Sampling " + meta + " at " + x + "," + y + "," + z
					+ " on world " + dim);
		}

		if (sourceType == Utils.FLOWER || sourceType == Utils.BEDROCK) {
			boolean match = false;
			if (ProspectorJournal.bedrockFault.size() != 0) {
				for (GeoTag tag : ProspectorJournal.bedrockFault) {
					if (dim == tag.dim && meta == tag.ore) {
						// include adjacent chunks as same unit.
						// generates a 32 pattern of indicators, and a 6 spread of ores.
						if (tag.x >= x - 32 && tag.x <= x + 32 && tag.z >= z - 32 && tag.z <= z + 32) {
							match = true;
							if (sourceType == Utils.BEDROCK) {
								tag.x = x;
								tag.z = z;
								tag.sample = false;
								tag.dead = false;
								Utils.writeJson(Utils.GT_BED_FILE);
							}
							break;
						}
					} else if (tag.dim == dim && tag.ore == 0) {
						// find a vein under non-specific flowers
						boolean tSpecify = (sourceType == Utils.BEDROCK);
						// allow the confusing Sphalerite / Smithsonite flower to be specified by the
						// raw ore chunk
						// and the various tungsten ores too
						for (int i = 0, j = multiFlowers.length; i < j && !tSpecify; i++) {
							if (tag.ore == multiFlowers[i]) {
								tSpecify = true;
							}
						}
						if (tSpecify && tag.x >= x - 40 && tag.x <= x + 40 && tag.z >= z - 40 && tag.z <= z + 40) {
							ProspectorJournal.bedrockFault.remove(tag);
							match = false;
							continue;
						}
					} else if (tag.dim == dim && meta == 0) {
						if (tag.x >= x - 40 && tag.x <= x + 40 && tag.z >= z - 40 && tag.z <= z + 40) {
							if (!tag.sample) {
								match = true;
								break;
							}
							for (int i = 0, j = multiFlowers.length; i < j; i++) {
								if (tag.ore == multiFlowers[i]) {
									match = true;
									break;
								}
							}
						}
					}
					if (match)
						break;
				}
			}
			if (!match) {
				// make a new entry
				ProspectorJournal.bedrockFault
						.add(new GeoTag(meta, dim, x, z, sourceType == Utils.BEDROCK ? false : true));
				Utils.writeJson(Utils.GT_BED_FILE);
			}
		}

		if (meta == 0) {
			return;
		}

		// ignore non-specific rocks and empty ores
		if (ProspectorJournal.rockSurvey.size() != 0) {
			final int chunkX = x / 16;
			final int chunkZ = z / 16;
			for (RockMatter rock : ProspectorJournal.rockSurvey) {
				if (meta == rock.ore && dim == rock.dim && chunkX >= rock.cx() - 1 && chunkX <= rock.cx() + 1
						&& chunkZ >= rock.cz() - 1 && chunkZ <= rock.cz() + 1) {
					switch (sourceType) {
						case Utils.ORE_VEIN:
							if (rock.sample) {
								if (rock.y > y) {
									rock.sample = false;
									if (rock.dead) {
										rock.dead = false;
									}
									rock.y = (short) y;
								} else {
									continue;
								}
							} else {
								rock.dead = false;
							}
							break;
						case Utils.ROCK: // result of server-side message only
							if (rock.sample) {
								if (rock.y > y) {
									rock.y = (short) y;
								}
							} else {
								if (rock.y > y) {
									continue;
								}
								return;
							}
							break;
						default:
							if (rock.sample) {
								return;
							}
							if (rock.y > (short) 10) {
								continue;
							}
							return;
					}
					// Editing an existing vein
					rock.x = x;
					rock.z = z;
					Utils.writeJson(Utils.GT_FILE);
					return;
				}
			}
		}

		// Found traces of a new vein
		final String oreName = OreDictMaterial.MATERIAL_ARRAY[meta].mNameLocal;
		switch (sourceType) {
			case Utils.ORE_VEIN:
				ProspectorJournal.rockSurvey.add(new RockMatter(meta, dim, x, y, z, false));
				Utils.createMapMarker(x, y, z, dim, oreName, aPlayer);
				break;
			case Utils.ROCK:
				ProspectorJournal.rockSurvey.add(new RockMatter(meta, dim, x, y, z, true));
				Utils.createMapMarker(x, y, z, dim, oreName, aPlayer);
				break;
			default:
				ProspectorJournal.rockSurvey.add(new RockMatter(meta, dim, x, 10, z, true));
				Utils.createMapMarker(x, 10, z, dim, oreName, aPlayer);
				break;
		}
		Utils.writeJson(Utils.GT_FILE);
	}
}
