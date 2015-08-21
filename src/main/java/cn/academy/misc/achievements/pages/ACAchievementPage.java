package cn.academy.misc.achievements.pages;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cn.academy.core.AcademyCraft;
import cn.academy.misc.achievements.aches.ACAchievement;
import cn.academy.misc.achievements.aches.AchBasic;
import cn.academy.misc.achievements.aches.AchCrAnd;
import cn.academy.misc.achievements.aches.AchCrSingle;
import cn.academy.misc.achievements.aches.AchEvMatterUnitHarvest;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Achievement;
import net.minecraftforge.common.AchievementPage;

/**
 * @author EAirPeter
 */
public abstract class ACAchievementPage extends AchievementPage {
	
	private String name;
	private LinkedList<ACAchievement> list = new LinkedList();
	private List<Achievement> wrapped = new LinkedList<Achievement>();
	
	public ACAchievementPage(String pName) {
		super(pName);
		this.name = pName;
	}

	protected final void add(ACAchievement... aches) {
		for (ACAchievement ach : aches) {
			ach.registerAll();
			list.add(ach);
			wrapped.add(ach);
		}
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public List<Achievement> getAchievements() {
		return wrapped;
	}
	
}